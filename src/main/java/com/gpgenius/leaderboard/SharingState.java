package com.gpgenius.leaderboard;

/**
 * Decides what to do with the per-character leaderboard setting. RuneLite settings are shared by
 * every character, but the website keeps one choice per character, so: when a character logs in its
 * choice is read from the server into the setting, and when the player changes the setting it is sent
 * for the character that's logged in. A change made while logged out is sent for the next character
 * instead of being overwritten by it. Not thread safe on its own; LeaderboardSync locks around it.
 */
final class SharingState
{
	enum Action
	{
		NOTHING,
		FETCH,
		PUSH
	}

	private long character = -1;
	// A change made with no character logged in, to send once one logs in.
	private boolean dirty;
	// The value we just wrote from the server; its change event is ours, not the player's.
	private LeaderboardName echo;

	/** A character finished logging in. */
	Action loggedIn(long hash)
	{
		if (hash == -1 || hash == character)
		{
			return Action.NOTHING;
		}
		character = hash;
		if (dirty)
		{
			dirty = false;
			return Action.PUSH;
		}
		return Action.FETCH;
	}

	void loggedOut()
	{
		character = -1;
	}

	/** The setting changed, by the player or by {@link #fromServer}. */
	Action settingChanged(LeaderboardName value)
	{
		if (echo != null && echo == value)
		{
			echo = null;
			return Action.NOTHING;
		}
		echo = null;
		if (character == -1)
		{
			dirty = true;
			return Action.NOTHING;
		}
		return Action.PUSH;
	}

	/**
	 * The server said what this character's choice is. Returns true when the setting should be
	 * updated to it (the character is still the one logged in and the setting differs).
	 */
	boolean fromServer(long hash, LeaderboardName value, LeaderboardName current)
	{
		if (hash != character || value == current)
		{
			return false;
		}
		echo = value;
		return true;
	}

	long character()
	{
		return character;
	}
}
