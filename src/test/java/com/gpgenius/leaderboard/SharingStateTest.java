package com.gpgenius.leaderboard;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class SharingStateTest
{
	@Test
	public void loginReadsTheCharactersChoiceFromTheServer()
	{
		SharingState s = new SharingState();
		assertEquals(SharingState.Action.FETCH, s.loggedIn(42));
		assertEquals(SharingState.Action.NOTHING, s.loggedIn(42));
		assertEquals(SharingState.Action.NOTHING, s.loggedIn(-1));
		assertTrue(s.fromServer(42, LeaderboardName.NAMED, LeaderboardName.ANONYMOUS));
		// The change event from writing the server's value isn't sent back.
		assertEquals(SharingState.Action.NOTHING, s.settingChanged(LeaderboardName.NAMED));
		// The player's own change is.
		assertEquals(SharingState.Action.PUSH, s.settingChanged(LeaderboardName.HIDDEN));
	}

	@Test
	public void aReplyForAnotherCharacterIsIgnored()
	{
		SharingState s = new SharingState();
		s.loggedIn(1);
		s.loggedOut();
		s.loggedIn(2);
		assertFalse(s.fromServer(1, LeaderboardName.NAMED, LeaderboardName.ANONYMOUS));
		assertFalse(s.fromServer(2, LeaderboardName.ANONYMOUS, LeaderboardName.ANONYMOUS));
	}

	@Test
	public void aChangeWhileLoggedOutIsSentForTheNextCharacter()
	{
		SharingState s = new SharingState();
		assertEquals(SharingState.Action.NOTHING, s.settingChanged(LeaderboardName.NAMED));
		assertEquals(SharingState.Action.PUSH, s.loggedIn(7));
		s.loggedOut();
		assertEquals(SharingState.Action.FETCH, s.loggedIn(8));
	}

	@Test
	public void wireValuesRoundTrip()
	{
		for (LeaderboardName n : LeaderboardName.values())
		{
			assertEquals(n, LeaderboardName.fromWire(n.wire));
		}
		assertEquals(LeaderboardName.ANONYMOUS, LeaderboardName.fromWire("nonsense"));
	}
}
