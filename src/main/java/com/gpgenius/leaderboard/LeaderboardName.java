package com.gpgenius.leaderboard;

/** How the logged-in character shows on the website's leaderboards. */
public enum LeaderboardName
{
	ANONYMOUS("anonymous", "Anonymous name"),
	NAMED("named", "Show this character's name"),
	HIDDEN("hidden", "Don't show this character");

	/** The server's value. */
	public final String wire;
	private final String label;

	LeaderboardName(String wire, String label)
	{
		this.wire = wire;
		this.label = label;
	}

	public static LeaderboardName fromWire(String value)
	{
		for (LeaderboardName n : values())
		{
			if (n.wire.equals(value))
			{
				return n;
			}
		}
		return ANONYMOUS;
	}

	@Override
	public String toString()
	{
		return label;
	}
}
