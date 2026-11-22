package com.gpgenius.goal;

import com.gpgenius.api.Progress;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import net.runelite.client.util.QuantityFormatter;

/** The words on the goal card. Kept apart from Swing so it can be tested. */
public final class GoalText
{
	private GoalText()
	{
	}

	public static String title(Progress.Goal g)
	{
		String amount = QuantityFormatter.quantityToStackSize(g.target);
		String what = g.isStack() ? "Reach a " + amount + " stack" : "Make " + amount + " profit";
		String who = g.accountName != null ? " on " + g.accountName : "";
		return what + who;
	}

	/** "62.4M of 100M · 2d 4h left" */
	public static String progress(Progress.Goal g, Instant now)
	{
		String line = QuantityFormatter.quantityToStackSize(Math.max(0, g.progress)) + " of "
			+ QuantityFormatter.quantityToStackSize(g.target);
		String left = timeLeft(g.deadline, now);
		return left == null ? line : line + " · " + left;
	}

	/** What to do next: the pace needed and the preset that suits it. */
	public static String advice(Progress.Goal g)
	{
		if ("done".equals(g.status))
		{
			return "Goal reached. Set a new one when you're ready.";
		}
		if ("missed".equals(g.status))
		{
			return "The date has passed. Set a new goal to keep going.";
		}
		StringBuilder out = new StringBuilder();
		if (g.neededPerHour != null && g.neededPerHour > 0)
		{
			out.append("About ").append(QuantityFormatter.quantityToStackSize(g.neededPerHour)).append(" an hour to go. ");
		}
		if (g.presetLabel != null && g.checkInMinutes != null)
		{
			out.append(g.adapt ? "Following the goal: " : "Suggested: ")
				.append(g.presetLabel).append(", check in every ").append(minutes(g.checkInMinutes)).append('.');
		}
		return out.toString().trim();
	}

	static String timeLeft(String deadline, Instant now)
	{
		if (deadline == null)
		{
			return null;
		}
		Instant at;
		try
		{
			at = Instant.parse(deadline);
		}
		catch (DateTimeParseException e)
		{
			try
			{
				at = java.time.OffsetDateTime.parse(deadline).toInstant();
			}
			catch (DateTimeParseException e2)
			{
				return null;
			}
		}
		Duration d = Duration.between(now, at);
		if (d.isNegative() || d.isZero())
		{
			return "time's up";
		}
		long hours = d.toHours();
		if (hours >= 24)
		{
			return (hours / 24) + "d " + (hours % 24) + "h left";
		}
		if (hours >= 1)
		{
			return hours + "h " + (d.toMinutes() % 60) + "m left";
		}
		return Math.max(1, d.toMinutes()) + "m left";
	}

	static String minutes(int m)
	{
		if (m % 60 == 0 && m >= 60)
		{
			int h = m / 60;
			return h == 1 ? "hour" : h + " hours";
		}
		return m + " minutes";
	}
}
