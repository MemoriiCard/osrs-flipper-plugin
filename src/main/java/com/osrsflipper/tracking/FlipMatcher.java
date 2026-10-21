package com.osrsflipper.tracking;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

/**
 * Matches sells to earlier buys of the same item, first in first out.
 *
 * <p>Sold quantity with no tracked buy (for example items bought before the plugin was installed)
 * is not turned into a flip, since its cost is unknown.</p>
 */
public class FlipMatcher
{
	/** A quantity of an item bought at one price that has not been sold yet. */
	public static class BuyLot
	{
		public int quantity;
		public long pricePerItem;
		public long timeMillis;

		public BuyLot()
		{
		}

		BuyLot(int quantity, long pricePerItem, long timeMillis)
		{
			this.quantity = quantity;
			this.pricePerItem = pricePerItem;
			this.timeMillis = timeMillis;
		}
	}

	private Map<Integer, Deque<BuyLot>> openLots = new HashMap<>();

	/**
	 * @return the flip completed by this transaction, or null if it was a buy or matched no buys
	 */
	public Flip add(Transaction t)
	{
		if (t.buy)
		{
			openLots.computeIfAbsent(t.itemId, id -> new ArrayDeque<>())
				.addLast(new BuyLot(t.quantity, t.pricePerItem, t.timestampMillis));
			return null;
		}

		Deque<BuyLot> lots = openLots.get(t.itemId);
		if (lots == null || lots.isEmpty())
		{
			return null;
		}

		int remaining = t.quantity;
		int matched = 0;
		long buyCost = 0;
		long firstBuyTime = lots.peekFirst().timeMillis;
		while (remaining > 0 && !lots.isEmpty())
		{
			BuyLot lot = lots.peekFirst();
			int take = Math.min(remaining, lot.quantity);
			buyCost += take * lot.pricePerItem;
			matched += take;
			remaining -= take;
			lot.quantity -= take;
			if (lot.quantity == 0)
			{
				lots.removeFirst();
			}
		}
		if (lots.isEmpty())
		{
			openLots.remove(t.itemId);
		}
		return new Flip(t.itemId, t.itemName, matched, buyCost, t.pricePerItem, firstBuyTime, t.timestampMillis);
	}

	/** Quantity bought and not yet sold, per item. */
	public Map<Integer, Integer> getHoldings()
	{
		Map<Integer, Integer> holdings = new HashMap<>();
		for (Map.Entry<Integer, Deque<BuyLot>> e : openLots.entrySet())
		{
			int total = 0;
			for (BuyLot lot : e.getValue())
			{
				total += lot.quantity;
			}
			holdings.put(e.getKey(), total);
		}
		return holdings;
	}
}
