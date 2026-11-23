package com.gpgenius.api;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.gson.Gson;
import org.junit.Test;

public class MeTest
{
	@Test
	public void freePlanSaysWhatTheWaitingTrialNeeds()
	{
		Me me = new Gson().fromJson("{\"premium\":false,\"trial_waiting\":\"needs_discord\"}", Me.class);
		assertTrue(me.freePlanText().contains("Link your Discord account"));

		me = new Gson().fromJson("{\"trial_waiting\":\"discord_too_new\",\"trial_discord_min_age_days\":30}", Me.class);
		assertTrue(me.freePlanText().contains("at least 30 days old"));

		// Older servers send neither field.
		me = new Gson().fromJson("{\"premium\":false}", Me.class);
		assertTrue(me.freePlanText().contains("free-to-play items"));
		assertFalse(me.freePlanText().contains("Discord"));
	}
}
