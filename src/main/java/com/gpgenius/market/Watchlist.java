package com.gpgenius.market;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The player's watchlist: items with buy and sell target prices, from the "Watched items" setting.
 * A buy target is reached when someone instant-sells at or under it (so a buy offer there would
 * fill); a sell target when someone instant-buys at or over it. Each target notifies once, then
 * again only after the price has moved back past it. Kept free of RuneLite types so it can be tested.
 */
public final class Watchlist
{
	public static final int FREE_ITEMS = 3;
	public static final int PREMIUM_ITEMS = 100;

	/** One line of the setting. Targets of 0 are off. */
	public static final class Entry
	{
		/** What the player typed for the item: a name or an item ID. */
		public final String item;
		public final long buyTarget;
		public final long sellTarget;

		public Entry(String item, long buyTarget, long sellTarget)
		{
			this.item = item;
			this.buyTarget = buyTarget;
			this.sellTarget = sellTarget;
		}

		/** The item ID when the player typed one, else -1. */
		public int itemId()
		{
			try
			{
				int id = Integer.parseInt(item);
				return id > 0 ? id : -1;
			}
			catch (NumberFormatException e)
			{
				return -1;
			}
		}
	}

	/** A target that was just reached. */
	public static final class Hit
	{
		public final int itemId;
		public final boolean buy;
		public final long target;
		public final long price;

		Hit(int itemId, boolean buy, long target, long price)
		{
			this.itemId = itemId;
			this.buy = buy;
			this.target = target;
			this.price = price;
		}
	}

	/** Item ID -> targets already reported, so each crossing notifies once. */
	private final Map<Integer, boolean[]> reported = new HashMap<>();

	/** Lines like "Abyssal whip, 1.4m, 1.6m". Blank lines and lines starting with # are skipped. */
	public static List<Entry> parse(String text, int maxItems)
	{
		List<Entry> out = new ArrayList<>();
		if (text == null)
		{
			return out;
		}
		for (String line : text.split("\\r?\\n"))
		{
			String trimmed = line.trim();
			if (trimmed.isEmpty() || trimmed.startsWith("#") || out.size() >= maxItems)
			{
				continue;
			}
			// The name runs to the first ",", ";" or "|" (no tradeable item has one in its name). Targets
			// after it are split on spaces, ";", "|" or ", ", so "1,400,000" stays one number.
			int comma = -1;
			for (int i = 0; i < trimmed.length() && comma < 0; i++)
			{
				char c = trimmed.charAt(i);
				comma = c == ',' || c == ';' || c == '|' ? i : -1;
			}
			String name = (comma < 0 ? trimmed : trimmed.substring(0, comma)).trim();
			if (name.isEmpty())
			{
				continue;
			}
			List<String> targets = new ArrayList<>();
			if (comma >= 0)
			{
				for (String part : trimmed.substring(comma + 1).split("[;|\\s]+|,\\s+"))
				{
					if (!part.isEmpty())
					{
						targets.add(part);
					}
				}
			}
			if (targets.size() == 1 && targets.get(0).contains(",") && !targets.get(0).matches("\\d{1,3}(,\\d{3})+"))
			{
				// "1.4m,1.6m": no spaces at all.
				String both = targets.remove(0);
				for (String part : both.split(","))
				{
					targets.add(part);
				}
			}
			long buy = targets.size() > 0 ? amount(targets.get(0)) : 0;
			long sell = targets.size() > 1 ? amount(targets.get(1)) : 0;
			out.add(new Entry(name, Math.max(0, buy), Math.max(0, sell)));
		}
		return out;
	}

	/** "1.4m", "250k", "2b", "1,200,000" or "1200000" as coins; 0 for anything else. */
	public static long amount(String text)
	{
		if (text == null)
		{
			return 0;
		}
		String t = text.trim().toLowerCase(Locale.ROOT).replace(",", "").replace("gp", "").trim();
		if (t.isEmpty())
		{
			return 0;
		}
		long scale = 1;
		char last = t.charAt(t.length() - 1);
		if (last == 'k' || last == 'm' || last == 'b')
		{
			scale = last == 'k' ? 1_000L : last == 'm' ? 1_000_000L : 1_000_000_000L;
			t = t.substring(0, t.length() - 1).trim();
		}
		try
		{
			double value = Double.parseDouble(t) * scale;
			return value > 0 && value < 1e13 ? Math.round(value) : 0;
		}
		catch (NumberFormatException e)
		{
			return 0;
		}
	}

	public static boolean buyReached(Entry entry, MarketPrice price)
	{
		return entry.buyTarget > 0 && price != null && price.low != null && price.low <= entry.buyTarget;
	}

	public static boolean sellReached(Entry entry, MarketPrice price)
	{
		return entry.sellTarget > 0 && price != null && price.high != null && price.high >= entry.sellTarget;
	}

	/** Targets newly reached since the last check. Call with fresh prices. */
	public List<Hit> check(int itemId, Entry entry, MarketPrice price)
	{
		List<Hit> hits = new ArrayList<>();
		if (price == null)
		{
			return hits;
		}
		boolean[] done = reported.computeIfAbsent(itemId, k -> new boolean[2]);
		boolean buy = buyReached(entry, price);
		boolean sell = sellReached(entry, price);
		if (buy && !done[0])
		{
			hits.add(new Hit(itemId, true, entry.buyTarget, price.low));
		}
		if (sell && !done[1])
		{
			hits.add(new Hit(itemId, false, entry.sellTarget, price.high));
		}
		done[0] = buy;
		done[1] = sell;
		return hits;
	}

	public void reset()
	{
		reported.clear();
	}
}
