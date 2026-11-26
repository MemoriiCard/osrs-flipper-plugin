package com.gpgenius.notify;

import com.gpgenius.tracking.OfferStatus;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Collects what happened to the player's offers and turns it into at most one reminder per
 * check-in window, such as "2 sells done, +184k. 1 offer to reprice. Next: Buy 30 x Abyssal whip."
 *
 * <p>A reminder only tells the player that something is worth coming back for. It never acts in
 * game. Kept free of RuneLite types so it can be unit tested.</p>
 */
public class CheckInDigest
{
	/** Wait this long after the first event so things that happen together arrive in one reminder. */
	static final long SETTLE_MILLIS = 30_000;
	static final int MAX_NAMES = 3;

	private int buysDone;
	private int sellsDone;
	private long profit;
	private boolean hasProfit;
	private final Set<String> reprice = new LinkedHashSet<>();
	private String nextStep;
	/** Sell-now cues and dump alerts waiting for this reminder, in the order they came. */
	private final Set<String> notes = new LinkedHashSet<>();
	/** An urgent nudge was let through (NudgeGate): send without waiting for the window. */
	private boolean early;
	private long firstEventMillis;
	private long lastSentMillis;

	private final Map<Integer, OfferStatus> slots = new HashMap<>();
	private String lastAction;
	private String lastHeadline;

	/**
	 * Call for every GE slot update.
	 *
	 * @param trusted false for the updates RuneLite replays at login, which only seed the slot state
	 * @return whether this update completed an offer
	 */
	public boolean offerChanged(int slot, OfferStatus status, boolean trusted, long nowMillis)
	{
		OfferStatus before = slots.put(slot, status);
		boolean completed = trusted && before != null && before != status
			&& (status == OfferStatus.BOUGHT || status == OfferStatus.SOLD);
		if (completed)
		{
			if (status == OfferStatus.BOUGHT)
			{
				buysDone++;
			}
			else
			{
				sellsDone++;
			}
			touch(nowMillis);
		}
		return completed;
	}

	/** Profit from a flip that just completed. */
	public void flipProfit(long amount, long nowMillis)
	{
		profit += amount;
		hasProfit = true;
		touch(nowMillis);
	}

	/**
	 * Call when the current suggestion changes.
	 *
	 * @param action   buy, sell, abort or wait
	 * @param name     the item's name, or null
	 * @param headline the panel's one-line description
	 * @param repriceOn whether to include offers to reprice
	 * @param nextOn    whether to include a newly ready buy or sell
	 */
	public void suggestion(String action, String name, String headline, boolean repriceOn, boolean nextOn, long nowMillis)
	{
		boolean changed = headline != null && !headline.equals(lastHeadline);
		if (changed && "abort".equals(action) && name != null && repriceOn)
		{
			reprice.add(name);
			touch(nowMillis);
		}
		else if (changed && nextOn && ("buy".equals(action) || "sell".equals(action))
			&& (lastAction == null || "wait".equals(lastAction)))
		{
			nextStep = headline;
			touch(nowMillis);
		}
		lastAction = action;
		lastHeadline = headline;
	}

	/**
	 * A line for the next reminder, such as a sell-now cue or a dump alert, already deduplicated by
	 * NudgeGate.
	 *
	 * @param early send the reminder without waiting for the rest of the check-in window
	 */
	public void note(String line, boolean early, long nowMillis)
	{
		if (line == null || line.isEmpty())
		{
			return;
		}
		notes.add(line);
		this.early |= early;
		touch(nowMillis);
	}

	/** Something already waiting is urgent (NudgeGate let it through): send without waiting for the window. */
	public void hurry()
	{
		if (!isEmpty())
		{
			early = true;
		}
	}

	/** The player is at the GE and can see everything: nothing to remind them of. */
	public void seen(long nowMillis)
	{
		clear();
		lastSentMillis = nowMillis;
	}

	/** Forgets slot states and the last suggestion, for a new login. Pending news is kept. */
	public void resetSlots()
	{
		slots.clear();
		lastAction = null;
		lastHeadline = null;
	}

	public boolean isEmpty()
	{
		return buysDone == 0 && sellsDone == 0 && !hasProfit && reprice.isEmpty() && nextStep == null
			&& notes.isEmpty();
	}

	/**
	 * @param windowMillis the player's check-in time; at most one reminder is sent per window
	 * @param quiet        whether quiet hours are on right now
	 */
	public boolean due(long nowMillis, long windowMillis, boolean quiet)
	{
		return !isEmpty() && !quiet
			&& nowMillis - firstEventMillis >= SETTLE_MILLIS
			&& (early || nowMillis - lastSentMillis >= windowMillis);
	}

	/** The reminder text, and starts a new window. */
	public String drain(long nowMillis)
	{
		String message = message();
		clear();
		lastSentMillis = nowMillis;
		return message;
	}

	String message()
	{
		StringBuilder out = new StringBuilder("GP Genius: ");
		if (sellsDone > 0 || hasProfit)
		{
			out.append(sellsDone > 0 ? count(sellsDone, "sell") + " done" : "Flips done");
			if (hasProfit)
			{
				out.append(", ").append(signed(profit));
			}
			out.append(". ");
		}
		if (buysDone > 0)
		{
			out.append(count(buysDone, "buy")).append(" done. ");
		}
		if (!reprice.isEmpty())
		{
			out.append(count(reprice.size(), "offer")).append(" to reprice (").append(names()).append("). ");
		}
		int shown = 0;
		for (String note : notes)
		{
			if (shown == MAX_NAMES)
			{
				out.append("+").append(notes.size() - MAX_NAMES).append(" more in the panel. ");
				break;
			}
			out.append(note.endsWith(".") ? note : note + ".").append(' ');
			shown++;
		}
		if (nextStep != null)
		{
			out.append("Next: ").append(nextStep).append('.');
		}
		return out.toString().trim();
	}

	/** Whether the hour falls in quiet hours from start (inclusive) to end (exclusive), across midnight too. */
	public static boolean inQuietHours(int hour, int start, int end)
	{
		if (start == end)
		{
			return false;
		}
		return start < end ? hour >= start && hour < end : hour >= start || hour < end;
	}

	private String names()
	{
		StringBuilder out = new StringBuilder();
		int i = 0;
		for (String name : reprice)
		{
			if (i == MAX_NAMES)
			{
				out.append(", +").append(reprice.size() - MAX_NAMES).append(" more");
				break;
			}
			out.append(i == 0 ? "" : ", ").append(name);
			i++;
		}
		return out.toString();
	}

	private void touch(long nowMillis)
	{
		if (firstEventMillis == 0)
		{
			firstEventMillis = nowMillis;
		}
	}

	private void clear()
	{
		buysDone = 0;
		sellsDone = 0;
		profit = 0;
		hasProfit = false;
		reprice.clear();
		nextStep = null;
		notes.clear();
		early = false;
		firstEventMillis = 0;
	}

	private static String count(int n, String noun)
	{
		return n + " " + noun + (n == 1 ? "" : "s");
	}

	/** Like 184k, 1.2m or 950 gp, with a sign. */
	static String signed(long amount)
	{
		String sign = amount < 0 ? "-" : "+";
		long abs = Math.abs(amount);
		String value;
		if (abs >= 1_000_000_000L)
		{
			value = trim(abs / 1_000_000_000.0) + "b";
		}
		else if (abs >= 1_000_000)
		{
			value = trim(abs / 1_000_000.0) + "m";
		}
		else if (abs >= 1_000)
		{
			value = (abs / 1_000) + "k";
		}
		else
		{
			value = abs + " gp";
		}
		return sign + value;
	}

	private static String trim(double value)
	{
		String text = String.format(java.util.Locale.ROOT, "%.1f", Math.floor(value * 10) / 10);
		return text.endsWith(".0") ? text.substring(0, text.length() - 2) : text;
	}
}
