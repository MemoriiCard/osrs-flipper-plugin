package com.osrsflipper.suggestion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Items the player skipped. A skip lasts a while so the same item isn't suggested straight back,
 * then expires so the item can come back once the market moves.
 */
public class SkipList
{
	static final long SKIP_MILLIS = 30 * 60 * 1000L;
	static final int MAX_SKIPS = 200;

	private final Map<Integer, Long> skippedAt = new HashMap<>();

	public void skip(int itemId, long nowMillis)
	{
		skippedAt.put(itemId, nowMillis);
		if (skippedAt.size() > MAX_SKIPS)
		{
			Integer oldest = null;
			for (Map.Entry<Integer, Long> e : skippedAt.entrySet())
			{
				if (oldest == null || e.getValue() < skippedAt.get(oldest))
				{
					oldest = e.getKey();
				}
			}
			skippedAt.remove(oldest);
		}
	}

	public List<Integer> active(long nowMillis)
	{
		List<Integer> ids = new ArrayList<>();
		Iterator<Map.Entry<Integer, Long>> it = skippedAt.entrySet().iterator();
		while (it.hasNext())
		{
			Map.Entry<Integer, Long> e = it.next();
			if (nowMillis - e.getValue() >= SKIP_MILLIS)
			{
				it.remove();
			}
			else
			{
				ids.add(e.getKey());
			}
		}
		return ids;
	}

	public void clear()
	{
		skippedAt.clear();
	}
}
