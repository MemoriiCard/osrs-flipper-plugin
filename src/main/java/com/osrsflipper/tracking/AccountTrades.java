package com.osrsflipper.tracking;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Everything tracked for one game account. Serialized to disk as JSON.
 */
public class AccountTrades
{
	static final int MAX_STORED = 10_000;

	private OfferTracker tracker = new OfferTracker();
	private FlipMatcher matcher = new FlipMatcher();
	private List<Transaction> transactions = new ArrayList<>();
	private List<Flip> flips = new ArrayList<>();

	/**
	 * Records a slot update.
	 *
	 * @return the flip it completed, or null
	 */
	public Flip onOffer(OfferSnapshot snapshot, String itemName, long nowMillis, boolean trustEmpty)
	{
		Transaction t = tracker.update(snapshot, itemName, nowMillis, trustEmpty);
		if (t == null)
		{
			return null;
		}
		add(transactions, t);
		Flip flip = matcher.add(t);
		if (flip != null)
		{
			add(flips, flip);
		}
		return flip;
	}

	/** Trades saved by versions before upload support have no client ID yet. */
	public void ensureClientIds()
	{
		for (Transaction t : transactions)
		{
			if (t.clientId == null)
			{
				t.clientId = UUID.randomUUID().toString();
			}
		}
	}

	public List<Flip> getFlips()
	{
		return flips;
	}

	public List<Transaction> getTransactions()
	{
		return transactions;
	}

	/** The last known state of each GE slot, by slot. */
	public Map<Integer, OfferSnapshot> getSlots()
	{
		return tracker.getSlots();
	}

	public FlipMatcher getMatcher()
	{
		return matcher;
	}

	public long profitSince(long sinceMillis)
	{
		long total = 0;
		for (Flip f : flips)
		{
			if (f.sellTimeMillis >= sinceMillis)
			{
				total += f.profit;
			}
		}
		return total;
	}

	private static <T> void add(List<T> list, T item)
	{
		list.add(item);
		if (list.size() > MAX_STORED)
		{
			list.remove(0);
		}
	}
}
