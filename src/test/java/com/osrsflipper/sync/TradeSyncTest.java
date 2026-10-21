package com.osrsflipper.sync;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.osrsflipper.api.TradeUpload;
import com.osrsflipper.tracking.AccountTrades;
import com.osrsflipper.tracking.Transaction;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.Test;

public class TradeSyncTest
{
	@Test
	public void picksUnsentTradesInBatches()
	{
		List<Transaction> all = new ArrayList<>();
		for (int i = 0; i < 5; i++)
		{
			all.add(new Transaction(4151, "Abyssal whip", i % 2 == 0, 1, 1_500_000, 1_700_000_000_000L + i, 0));
		}
		assertNotEquals(all.get(0).clientId, all.get(1).clientId);
		all.get(0).uploaded = true;

		List<Transaction> batch = TradeSync.pending(all, 3);
		assertEquals(3, batch.size());
		assertEquals(all.get(1), batch.get(0));

		TradeUpload upload = TradeSync.build(-123456789012345L, "Zezima", batch);
		assertEquals("-123456789012345", upload.accountHash);
		assertEquals(3, upload.trades.size());
		assertEquals(batch.get(0).clientId, upload.trades.get(0).clientId);
		assertEquals(1_500_000, upload.trades.get(0).total);

		TradeSync.markUploaded(batch);
		assertEquals(1, TradeSync.pending(all, 10).size());
	}

	@Test
	public void oldSavedTradesGetIds()
	{
		AccountTrades trades = new AccountTrades();
		Transaction old = new Transaction();
		old.quantity = 1;
		trades.getTransactions().add(old);
		trades.ensureClientIds();
		assertNotNull(old.clientId);
	}

	@Test
	public void usageCountsAreCappedAndCleared()
	{
		UsageCounter usage = new UsageCounter();
		usage.add("hotkey.quick_set");
		usage.add("hotkey.quick_set");
		usage.add("Bad Name!");
		for (int i = 0; i < 40; i++)
		{
			usage.add("feature_" + i);
		}
		Map<String, Integer> counts = usage.drain();
		assertEquals(Integer.valueOf(2), counts.get("hotkey.quick_set"));
		assertTrue(counts.size() <= 30);
		assertTrue(usage.drain().isEmpty());
	}
}
