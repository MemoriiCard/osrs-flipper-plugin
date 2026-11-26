package com.gpgenius.sync;

import com.gpgenius.api.TradeUpload;
import com.gpgenius.tracking.Transaction;
import java.util.ArrayList;
import java.util.List;

/**
 * Picks tracked trades the server doesn't have yet. Uploads are safe to repeat: each trade carries a
 * random client ID and the server ignores ones it has seen, so a lost reply just means a resend.
 */
public final class TradeSync
{
	/** The server accepts at most this many trades per request. */
	public static final int BATCH = 500;
	/** The server refuses trades older than this (mid-2017), so one bad imported date can't block a batch. */
	static final long OLDEST_MILLIS = 1_500_000_000_000L;

	private TradeSync()
	{
	}

	public static List<Transaction> pending(List<Transaction> all, int max)
	{
		List<Transaction> out = new ArrayList<>();
		for (Transaction t : all)
		{
			if (out.size() >= max)
			{
				break;
			}
			if (!t.uploaded && t.clientId != null && t.quantity > 0 && t.totalCoins >= 0
				&& t.timestampMillis >= OLDEST_MILLIS)
			{
				out.add(t);
			}
		}
		return out;
	}

	public static TradeUpload build(long accountHash, String displayName, List<Transaction> batch)
	{
		TradeUpload upload = new TradeUpload();
		upload.accountHash = Long.toString(accountHash);
		upload.displayName = displayName == null ? "" : displayName;
		for (Transaction t : batch)
		{
			TradeUpload.Trade trade = new TradeUpload.Trade();
			trade.clientId = t.clientId;
			trade.itemId = t.itemId;
			trade.isBuy = t.buy;
			trade.quantity = t.quantity;
			trade.total = t.totalCoins;
			trade.timestampMs = t.timestampMillis;
			trade.imported = t.imported;
			trade.slot = t.imported || t.slot < 0 || t.slot > 7 ? null : t.slot;
			upload.trades.add(trade);
		}
		return upload;
	}

	public static void markUploaded(List<Transaction> batch)
	{
		for (Transaction t : batch)
		{
			t.uploaded = true;
		}
	}
}
