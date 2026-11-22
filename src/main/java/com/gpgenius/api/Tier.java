package com.gpgenius.api;

/**
 * The plan a game account is on (Free, Premium or Pro), as the server reports it. Plans differ in how
 * fresh prices are, how often suggestions refresh and features; the server decides the plan.
 */
public final class Tier
{
	public static final String FREE = "free";
	public static final String PREMIUM = "premium";
	public static final String PRO = "pro";

	/** Idle refresh bounds, whatever the server says. */
	static final int MIN_REFRESH_SECONDS = 15;
	static final int MAX_REFRESH_SECONDS = 10 * 60;
	/** Default when the server is older and sends no refresh time. */
	public static final int DEFAULT_REFRESH_SECONDS = 60;

	private Tier()
	{
	}

	/** Milliseconds to wait between requests while nothing in the game changed. */
	public static long refreshMillis(int refreshSeconds)
	{
		int seconds = refreshSeconds <= 0 ? DEFAULT_REFRESH_SECONDS : refreshSeconds;
		return Math.max(MIN_REFRESH_SECONDS, Math.min(MAX_REFRESH_SECONDS, seconds)) * 1000L;
	}

	/** Shortest gap between requests after a change. Free runs the engine at most a few times a minute. */
	public static long minGapMillis(String tier)
	{
		return FREE.equals(tier) ? 10_000L : 2_000L;
	}

	/** "every 3 minutes", "every 20 seconds". */
	public static String every(int seconds)
	{
		if (seconds <= 0)
		{
			seconds = DEFAULT_REFRESH_SECONDS;
		}
		if (seconds % 60 == 0)
		{
			int minutes = seconds / 60;
			return minutes == 1 ? "every minute" : "every " + minutes + " minutes";
		}
		return "every " + seconds + " seconds";
	}

	/** The plan line under the account title: what the plan covers and how often it refreshes. */
	public static String line(String tier, String summary, int refreshSeconds)
	{
		StringBuilder text = new StringBuilder();
		if (summary != null && !summary.isEmpty())
		{
			text.append(summary).append(' ');
		}
		text.append("Suggestions refresh ").append(every(refreshSeconds)).append(", and straight away when your offers change.");
		return text.toString();
	}
}
