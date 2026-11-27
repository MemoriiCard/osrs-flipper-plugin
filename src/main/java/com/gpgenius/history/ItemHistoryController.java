package com.gpgenius.history;

import com.gpgenius.api.ItemHistory;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Keeps the "You: 12 flips, +3.4M ..." line for the item on show (the suggestion, or the offer being
 * set up). One request per item every few minutes at most, remembered for the last few items so going
 * back and forth between suggestions costs nothing. Kept free of RuneLite types so it can be unit tested.
 */
public class ItemHistoryController
{
	static final long REFRESH_MILLIS = 5 * 60_000;
	static final int REMEMBER = 20;

	/** Fetches one item's history; calls back on the client thread with the result or null on failure. */
	public interface Fetcher
	{
		void fetch(int itemId, Consumer<ItemHistory> done);
	}

	private static final class Entry
	{
		ItemHistory history;
		long fetchedAt;
	}

	private final Fetcher fetcher;
	private final Map<Integer, Entry> cache = new LinkedHashMap<Integer, ItemHistoryController.Entry>(16, 0.75f, true)
	{
		@Override
		protected boolean removeEldestEntry(Map.Entry<Integer, ItemHistoryController.Entry> eldest)
		{
			return size() > REMEMBER;
		}
	};
	private Runnable onChange = () -> { };
	private Integer itemId;
	private Integer inFlight;

	public ItemHistoryController(Fetcher fetcher)
	{
		this.fetcher = fetcher;
	}

	public void setOnChange(Runnable onChange)
	{
		this.onChange = onChange;
	}

	/** @param wanted the item on show, or null for none (signed out, setting off) */
	public void tick(Integer wanted, long now)
	{
		boolean switched = wanted == null ? itemId != null : !wanted.equals(itemId);
		itemId = wanted;
		if (wanted == null)
		{
			if (switched)
			{
				onChange.run();
			}
			return;
		}
		Entry e = cache.get(wanted);
		boolean fresh = e != null && now - e.fetchedAt < REFRESH_MILLIS;
		if (switched)
		{
			onChange.run();
		}
		if (fresh || wanted.equals(inFlight))
		{
			return;
		}
		final int id = wanted;
		inFlight = id;
		fetcher.fetch(id, h -> finish(id, h, now));
	}

	private void finish(int id, ItemHistory h, long now)
	{
		if (inFlight != null && inFlight == id)
		{
			inFlight = null;
		}
		Entry e = cache.computeIfAbsent(id, k -> new Entry());
		// A failure keeps any older answer and waits the full refresh before trying again.
		if (h != null)
		{
			e.history = h;
		}
		e.fetchedAt = now;
		if (itemId != null && itemId == id)
		{
			onChange.run();
		}
	}

	/** The line for the item on show, or null when there's nothing to show. */
	public String line()
	{
		if (itemId == null)
		{
			return null;
		}
		Entry e = cache.get(itemId);
		return e == null || e.history == null ? null : e.history.line;
	}

	public void clear()
	{
		cache.clear();
		itemId = null;
		inFlight = null;
	}
}
