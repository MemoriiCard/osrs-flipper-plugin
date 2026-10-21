package com.osrsflipper.tracking;

import static org.junit.Assert.assertEquals;
import com.google.gson.Gson;
import java.io.File;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class TradeStoreTest
{
	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	@Test
	public void roundTripKeepsOpenBuysAndSlotState() throws Exception
	{
		TradeStore store = new TradeStore(new Gson(), new File(folder.getRoot(), "trades"));
		AccountTrades trades = new AccountTrades();
		trades.onOffer(new OfferSnapshot(2, 4151, OfferStatus.BUYING, 1_500_000, 2, 2, 3_000_000L), "Abyssal whip", 1, true);
		store.save(42L, trades);

		AccountTrades loaded = store.load(42L);
		assertEquals(Integer.valueOf(2), loaded.getMatcher().getHoldings().get(4151));

		// The same slot state seen again after a reload is not a new fill.
		loaded.onOffer(new OfferSnapshot(2, 4151, OfferStatus.BOUGHT, 1_500_000, 2, 2, 3_000_000L), "Abyssal whip", 2, false);
		assertEquals(1, loaded.getTransactions().size());

		Flip flip = loaded.onOffer(new OfferSnapshot(3, 4151, OfferStatus.SOLD, 1_600_000, 2, 2, 3_200_000L), "Abyssal whip", 3, true);
		assertEquals(2, flip.quantity);
		assertEquals(flip.profit, loaded.profitSince(0));
	}

	@Test
	public void missingFileGivesEmptyTrades() throws Exception
	{
		TradeStore store = new TradeStore(new Gson(), folder.getRoot());
		assertEquals(0, store.load(7L).getFlips().size());
	}
}
