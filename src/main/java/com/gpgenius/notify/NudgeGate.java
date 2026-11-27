package com.gpgenius.notify;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * The one rule every nudge (sell-now cues, dump alerts, offers that need updating) goes through, so
 * players are never barraged:
 *
 * <ul>
 * <li>The same nudge is said once: it isn't repeated until it has been quiet for a check-in window
 * (at least {@link #MIN_REPEAT_MILLIS}).</li>
 * <li>Everything waits for the player's check-in reminder, batched into one message.</li>
 * <li>Only urgent nudges (an instant sell or buy, an offer that needs updating) may bring that
 * reminder forward, at most {@link #MAX_EARLY_PER_WINDOW} times per check-in window and never closer
 * together than {@link #MIN_EARLY_GAP_MILLIS}. Quiet hours hold them back too.</li>
 * </ul>
 *
 * <p>Client thread only. Free of RuneLite types so it can be unit tested.</p>
 */
public class NudgeGate
{
	/** What to do with a nudge. */
	public enum Decision
	{
		/** Already said recently: drop it. */
		DROP,
		/** Add it to the next check-in reminder. */
		BATCH,
		/** Add it to the reminder and send that reminder now rather than waiting for the window. */
		EARLY
	}

	static final long MIN_REPEAT_MILLIS = 30 * 60_000L;
	static final long MIN_EARLY_GAP_MILLIS = 5 * 60_000L;
	static final int MAX_EARLY_PER_WINDOW = 2;

	private final Map<String, Long> said = new HashMap<>();
	private final Deque<Long> early = new ArrayDeque<>();

	/**
	 * @param key          what makes two nudges the same, such as "cue:instant:4151"
	 * @param urgent       whether this may bring the reminder forward
	 * @param quiet        whether quiet hours are on now
	 * @param windowMillis the player's check-in time
	 */
	public Decision offer(String key, boolean urgent, boolean quiet, long nowMillis, long windowMillis)
	{
		long repeat = Math.max(MIN_REPEAT_MILLIS, windowMillis);
		prune(nowMillis, repeat, windowMillis);
		Long last = said.get(key);
		if (last != null && nowMillis - last < repeat)
		{
			return Decision.DROP;
		}
		said.put(key, nowMillis);
		if (!urgent || quiet)
		{
			return Decision.BATCH;
		}
		Long latest = early.peekLast();
		if (early.size() >= MAX_EARLY_PER_WINDOW || (latest != null && nowMillis - latest < MIN_EARLY_GAP_MILLIS))
		{
			return Decision.BATCH;
		}
		early.addLast(nowMillis);
		return Decision.EARLY;
	}

	/** The condition behind a nudge has gone (the item sold, the offer was updated): it may be said again. */
	public void forget(String key)
	{
		said.remove(key);
	}

	public void reset()
	{
		said.clear();
		early.clear();
	}

	int remembered()
	{
		return said.size();
	}

	private void prune(long nowMillis, long repeat, long windowMillis)
	{
		for (Iterator<Long> it = said.values().iterator(); it.hasNext(); )
		{
			if (nowMillis - it.next() >= repeat)
			{
				it.remove();
			}
		}
		long span = Math.max(windowMillis, MIN_EARLY_GAP_MILLIS);
		while (!early.isEmpty() && nowMillis - early.peekFirst() >= span)
		{
			early.removeFirst();
		}
	}
}
