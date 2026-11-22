package com.gpgenius.goal;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Turns what the player types into a goal: amounts like "100m" and an "end of which day" deadline. */
public final class GoalInput
{
	public static final int DAYS_AHEAD = 30;
	private static final long MAX = 10_000_000_000_000L;

	private GoalInput()
	{
	}

	/** "100m", "1.5b", "250,000k" or a plain number of coins; null if it isn't one. */
	public static Long parseAmount(String text)
	{
		if (text == null)
		{
			return null;
		}
		String raw = text.trim().toLowerCase(Locale.ROOT).replace(",", "").replace(" ", "");
		if (raw.endsWith("gp"))
		{
			raw = raw.substring(0, raw.length() - 2);
		}
		if (raw.isEmpty() || raw.length() > 20)
		{
			return null;
		}
		double scale = 1;
		char last = raw.charAt(raw.length() - 1);
		if (last == 'k' || last == 'm' || last == 'b')
		{
			scale = last == 'k' ? 1e3 : last == 'm' ? 1e6 : 1e9;
			raw = raw.substring(0, raw.length() - 1);
		}
		double value;
		try
		{
			value = Double.parseDouble(raw);
		}
		catch (NumberFormatException e)
		{
			return null;
		}
		double coins = value * scale;
		if (!(coins > 0) || coins > MAX)
		{
			return null;
		}
		return Math.round(coins);
	}

	/** One choice in the "By" list: the end of a day, in the player's time zone. */
	public static final class Deadline
	{
		public final ZonedDateTime at;
		private final String label;

		Deadline(ZonedDateTime at, String label)
		{
			this.at = at;
			this.label = label;
		}

		@Override
		public String toString()
		{
			return label;
		}
	}

	/** The end of today and each of the next {@link #DAYS_AHEAD} days. Today is left out once it's nearly over. */
	public static List<Deadline> deadlines(ZonedDateTime now)
	{
		List<Deadline> out = new ArrayList<>();
		LocalDate today = now.toLocalDate();
		ZoneId zone = now.getZone();
		for (int i = 0; i <= DAYS_AHEAD; i++)
		{
			LocalDate day = today.plusDays(i);
			ZonedDateTime end = day.atTime(LocalTime.of(23, 59)).atZone(zone);
			if (end.isBefore(now.plusHours(1)))
			{
				continue;
			}
			out.add(new Deadline(end, label(day, i)));
		}
		return out;
	}

	/** The first choice that falls on this weekday, such as "this Friday". */
	public static int indexOf(List<Deadline> choices, DayOfWeek day)
	{
		for (int i = 0; i < choices.size(); i++)
		{
			if (choices.get(i).at.getDayOfWeek() == day)
			{
				return i;
			}
		}
		return 0;
	}

	private static String label(LocalDate day, int offset)
	{
		String name = day.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
			+ " " + day.getDayOfMonth() + " " + day.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
		if (offset == 0)
		{
			return "Today (" + name + ")";
		}
		if (offset == 1)
		{
			return "Tomorrow (" + name + ")";
		}
		return name;
	}
}
