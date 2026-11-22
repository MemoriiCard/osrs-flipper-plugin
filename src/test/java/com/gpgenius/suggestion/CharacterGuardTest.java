package com.gpgenius.suggestion;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class CharacterGuardTest
{
	@Test
	public void onlyTheCharacterAskedForGetsTheAnswer()
	{
		CharacterGuard guard = new CharacterGuard();
		assertFalse(CharacterGuard.ready(-1, "Zezima"));
		assertFalse(CharacterGuard.ready(42, null));
		assertFalse(CharacterGuard.ready(42, ""));
		guard.expect(42, "Zezima");
		assertTrue(guard.matches(42, "Zezima"));
		assertFalse("another character", guard.matches(43, "Lynx Titan"));
		assertFalse("same account, name changed", guard.matches(42, "Zezima2"));
		assertFalse("logged out", guard.matches(-1, null));
		guard.clear();
		assertFalse(guard.matches(42, "Zezima"));
	}
}
