package com.gpgenius.suggestion;

import java.util.Objects;

/**
 * Suggestions are only shown for the OSRS character they were asked for. Each request remembers the
 * account hash and character name (RSN) that were logged in when it was sent; an answer that comes
 * back after the player logged out, switched character or changed name is dropped, and nothing is asked
 * while no character is fully logged in.
 */
public final class CharacterGuard
{
	private long expectedHash = -1;
	private String expectedName = "";

	/** True when a character is logged in well enough to ask for it. */
	public static boolean ready(long accountHash, String name)
	{
		return accountHash != -1 && name != null && !name.isEmpty();
	}

	/** Remembers who the next answer is for. */
	public void expect(long accountHash, String name)
	{
		expectedHash = accountHash;
		expectedName = name == null ? "" : name;
	}

	/** True when the character logged in now is the one the last request was for. */
	public boolean matches(long accountHash, String name)
	{
		return ready(accountHash, name) && accountHash == expectedHash && Objects.equals(expectedName, name);
	}

	public void clear()
	{
		expectedHash = -1;
		expectedName = "";
	}
}
