package com.gpgenius.history;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.gpgenius.tracking.AccountTrades;
import com.gpgenius.tracking.Flip;
import com.gpgenius.tracking.Transaction;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

public class FlippingUtilitiesImportTest
{
	private static final long T = 1_760_000_000L;

	/** Shaped like a Flipping Utilities account file: one whip bought in two fills then sold, and a reset trade. */
	private static final String FILE = "{\"lastOffers\":{},\"trades\":[{"
		+ "\"id\":4151,\"name\":\"Abyssal whip\",\"tGL\":70,\"fB\":\"Zezima\",\"h\":{\"sO\":["
		// A buy that filled 2, then 5 in total, then completed at 5.
		+ "{\"b\":true,\"id\":4151,\"cQIT\":2,\"p\":1500000,\"t\":{\"seconds\":" + T + ",\"nanos\":0},\"s\":0,\"st\":\"BUYING\",\"tQIT\":5,\"vSQ\":true},"
		+ "{\"b\":true,\"id\":4151,\"cQIT\":5,\"p\":1500000,\"t\":{\"seconds\":" + (T + 60) + ",\"nanos\":0},\"s\":0,\"st\":\"BOUGHT\",\"tQIT\":5,\"vSQ\":true},"
		// Sold all 5 in one go, in the same slot.
		+ "{\"b\":false,\"id\":4151,\"cQIT\":5,\"p\":1600000,\"t\":{\"seconds\":" + (T + 3600) + ",\"nanos\":0},\"s\":0,\"st\":\"SOLD\",\"tQIT\":5,\"vSQ\":true},"
		// Cleared by a stats reset: ignored.
		+ "{\"b\":true,\"id\":4151,\"cQIT\":1,\"p\":1,\"t\":{\"seconds\":" + (T - 99) + ",\"nanos\":0},\"s\":1,\"st\":\"BOUGHT\",\"tQIT\":1,\"vSQ\":false},"
		// Added by hand from the GE history tab (slot -1): taken as is.
		+ "{\"b\":true,\"id\":4151,\"cQIT\":1,\"p\":1400000,\"t\":" + ((T - 7200) * 1000) + ",\"s\":-1,\"st\":\"BOUGHT\"},"
		// Not a trade at all.
		+ "\"junk\""
		+ "]}},"
		// An item with no history.
		+ "{\"id\":554,\"name\":\"Fire rune\"}]}";

	@Test
	public void readsFillsAsDeltasInTimeOrder() throws IOException
	{
		FlippingUtilitiesImport.Result result = FlippingUtilitiesImport.parse(new StringReader(FILE));
		List<Transaction> trades = result.trades;
		assertEquals(4, trades.size());
		assertEquals(1, result.skipped);

		Transaction manual = trades.get(0);
		assertTrue(manual.buy);
		assertEquals(1, manual.quantity);
		assertEquals(-1, manual.slot);

		assertEquals(2, trades.get(1).quantity);
		assertEquals(3_000_000, trades.get(1).totalCoins);
		assertEquals(3, trades.get(2).quantity);
		assertEquals(4_500_000, trades.get(2).totalCoins);
		assertEquals(T * 1000 + 60_000, trades.get(2).timestampMillis);

		Transaction sell = trades.get(3);
		assertFalse(sell.buy);
		assertEquals(5, sell.quantity);
		assertEquals(1_600_000, sell.pricePerItem);
		for (Transaction t : trades)
		{
			assertTrue(t.imported);
			assertEquals("Abyssal whip", t.itemName);
		}
	}

	@Test
	public void sameFileGivesSameIds() throws IOException
	{
		List<Transaction> a = FlippingUtilitiesImport.parse(new StringReader(FILE)).trades;
		List<Transaction> b = FlippingUtilitiesImport.parse(new StringReader(FILE)).trades;
		for (int i = 0; i < a.size(); i++)
		{
			assertEquals(a.get(i).clientId, b.get(i).clientId);
		}
	}

	@Test
	public void importedBuysPairWithLaterSellsAndImportingTwiceAddsNothing() throws IOException
	{
		AccountTrades account = new AccountTrades();
		List<Transaction> trades = FlippingUtilitiesImport.parse(new StringReader(FILE)).trades;
		assertEquals(4, account.importHistory(trades));
		assertEquals(0, account.importHistory(FlippingUtilitiesImport.parse(new StringReader(FILE)).trades));
		assertEquals(4, account.getTransactions().size());

		List<Flip> flips = account.getFlips();
		assertEquals(1, flips.size());
		Flip flip = flips.get(0);
		// First in, first out: the hand-added whip at 1.4m, then four at 1.5m.
		assertEquals(5, flip.quantity);
		assertEquals((1_400_000L + 4 * 1_500_000L) / 5, flip.buyPrice);
		// One whip from the second fill is still held.
		assertEquals(Integer.valueOf(1), account.getMatcher().getHoldings().get(4151));
	}

	@Test
	public void readsEveryTimeShape()
	{
		com.google.gson.JsonParser parser = new com.google.gson.JsonParser();
		assertEquals(1_000_500L, FlippingUtilitiesImport.time(parser.parse("{\"seconds\":1000,\"nanos\":500000000}")));
		assertEquals(1_760_000_000_000L, FlippingUtilitiesImport.time(parser.parse("1760000000")));
		assertEquals(1_760_000_000_123L, FlippingUtilitiesImport.time(parser.parse("1760000000123")));
		assertEquals(1_760_000_000_000L, FlippingUtilitiesImport.time(parser.parse("\"2025-10-09T08:53:20Z\"")));
		assertEquals(0L, FlippingUtilitiesImport.time(parser.parse("\"yesterday\"")));
	}

	@Test
	public void refusesFilesThatArentHistory()
	{
		for (String text : new String[]{"not json {", "[]", "{\"options\":[]}"})
		{
			try
			{
				FlippingUtilitiesImport.parse(new StringReader(text));
				fail(text);
			}
			catch (IOException expected)
			{
				// Shown to the player as is.
			}
		}
	}

	@Test
	public void fileNameIsTheCharacterName()
	{
		File dir = new File("rl");
		assertEquals(new File(new File(dir, "flipping"), "Zezima.json"), FlippingUtilitiesImport.fileFor(dir, "Zezima"));
		assertNull(FlippingUtilitiesImport.fileFor(dir, "../evil"));
		assertNull(FlippingUtilitiesImport.fileFor(dir, ""));
	}

	@Test
	public void existingTrackedTradesKeepTheirState() throws IOException
	{
		AccountTrades account = new AccountTrades();
		List<Transaction> own = new ArrayList<>();
		Transaction t = new Transaction(554, "Fire rune", true, 100, 500, T * 1000 + 99_000_000, 3);
		t.uploaded = true;
		own.add(t);
		account.importHistory(own);
		account.importHistory(FlippingUtilitiesImport.parse(new StringReader(FILE)).trades);
		Transaction last = account.getTransactions().get(account.getTransactions().size() - 1);
		assertTrue(last.uploaded);
		assertFalse(last.imported);
	}
}
