package com.osrsflipper.sync;

import java.util.HashMap;
import java.util.Map;
import javax.inject.Singleton;

/**
 * Counts how often features are used, sent to the server a few times an hour as plain totals,
 * so we know which features are worth improving. No item, price or timing details.
 */
@Singleton
public class UsageCounter
{
	private static final int MAX_NAMES = 30;
	private static final int MAX_COUNT = 1000;

	private final Map<String, Integer> counts = new HashMap<>();

	public synchronized void add(String name)
	{
		if (!name.matches("[a-z][a-z0-9_.]{1,47}") || (!counts.containsKey(name) && counts.size() >= MAX_NAMES))
		{
			return;
		}
		counts.merge(name, 1, (a, b) -> Math.min(MAX_COUNT, a + b));
	}

	/** Returns the counts so far and starts again from zero. */
	public synchronized Map<String, Integer> drain()
	{
		Map<String, Integer> out = new HashMap<>(counts);
		counts.clear();
		return out;
	}
}
