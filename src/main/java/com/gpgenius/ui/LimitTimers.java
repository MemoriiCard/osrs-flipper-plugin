package com.gpgenius.ui;

import com.gpgenius.api.Suggestion;
import java.time.Instant;
import java.time.format.DateTimeParseException;

/** Text for the buy limit timers in the panel. Free of Swing so it can be unit tested. */
public final class LimitTimers
{
	private LimitTimers()
	{
	}

	/** Milliseconds since the epoch, or -1 when the reset time is unknown or unreadable. */
	public static long resetsAtMillis(String iso)
	{
		if (iso == null || iso.isEmpty())
		{
			return -1;
		}
		try
		{
			return Instant.parse(iso).toEpochMilli();
		}
		catch (DateTimeParseException e)
		{
			try
			{
				return java.time.OffsetDateTime.parse(iso).toInstant().toEpochMilli();
			}
			catch (DateTimeParseException e2)
			{
				return -1;
			}
		}
	}

	/** "1:05:09", "4:32" or "Resetting", counting down to the reset. */
	public static String countdown(long resetsAtMillis, long nowMillis)
	{
		if (resetsAtMillis < 0)
		{
			return "";
		}
		long seconds = (resetsAtMillis - nowMillis + 999) / 1000;
		if (seconds <= 0)
		{
			return "Resetting";
		}
		long h = seconds / 3600;
		long m = seconds / 60 % 60;
		long s = seconds % 60;
		return h > 0 ? String.format("%d:%02d:%02d", h, m, s) : String.format("%d:%02d", m, s);
	}

	/** "70 / 70 bought" with what's left. */
	public static String usage(Suggestion.BuyLimit l)
	{
		return String.format("%,d / %,d bought", l.bought, l.limit);
	}

	/** The line under a row: why nothing is suggested for it, or how many are left. */
	public static String note(Suggestion.BuyLimit l, long resetsAtMillis, long nowMillis)
	{
		String when = countdown(resetsAtMillis, nowMillis);
		if (l.locked)
		{
			return when.isEmpty() || "Resetting".equals(when)
				? "At its limit. Other items are suggested meanwhile."
				: "At its limit for " + when + ". Other items are suggested meanwhile.";
		}
		return String.format("%,d left", l.remaining) + (when.isEmpty() ? "" : ", resets in " + when);
	}
}
