package com.gpgenius.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.gson.Gson;
import org.junit.Test;

public class TierTest
{
	@Test
	public void refreshFollowsTheServerWithinBounds()
	{
		assertEquals(180_000L, Tier.refreshMillis(180));
		assertEquals(20_000L, Tier.refreshMillis(20));
		// Older servers send nothing: the old one-minute refresh.
		assertEquals(60_000L, Tier.refreshMillis(0));
		// A bad value can't make the plugin hammer the server or stop asking.
		assertEquals(15_000L, Tier.refreshMillis(1));
		assertEquals(600_000L, Tier.refreshMillis(99_999));
	}

	@Test
	public void freeWaitsLongerAfterAChange()
	{
		assertEquals(10_000L, Tier.minGapMillis(Tier.FREE));
		assertEquals(2_000L, Tier.minGapMillis(Tier.PRO));
		assertEquals(2_000L, Tier.minGapMillis(null));
	}

	@Test
	public void sayingHowOften()
	{
		assertEquals("every 3 minutes", Tier.every(180));
		assertEquals("every minute", Tier.every(60));
		assertEquals("every 20 seconds", Tier.every(20));
	}

	@Test
	public void suggestionsCarryThePlan()
	{
		Suggestion s = new Gson().fromJson(
			"{\"action\":\"buy\",\"tier\":\"pro\",\"tier_name\":\"Pro\",\"refresh_seconds\":20,\"explanations\":true}",
			Suggestion.class);
		assertEquals(Tier.PRO, s.tier);
		assertEquals(20, s.refreshSeconds);
		assertTrue(s.explanations);
		Suggestion old = new Gson().fromJson("{\"action\":\"buy\"}", Suggestion.class);
		assertFalse(old.explanations);
		assertEquals(0, old.refreshSeconds);
	}

	@Test
	public void planLinesSayHowOftenTheyRefresh()
	{
		Me pro = new Gson().fromJson("{\"premium\":true,\"seats\":1,\"tier\":\"pro\",\"tier_summary\":\"Everything in Premium.\","
			+ "\"refresh_seconds\":20,\"current_account\":{\"premium\":true}}", Me.class);
		assertTrue(pro.pro());
		assertTrue(pro.paidPlanText().contains("every 20 seconds"));
		assertTrue(pro.paidPlanText().startsWith("1 game account."));

		Me free = new Gson().fromJson("{\"premium\":false,\"tier\":\"free\",\"refresh_seconds\":180}", Me.class);
		assertTrue(free.freePlanText().contains("every 3 minutes"));

		// Older servers: no tier, the text is as before.
		Me old = new Gson().fromJson("{\"premium\":true,\"seats\":2}", Me.class);
		assertFalse(old.pro());
		assertEquals("2 game accounts.", old.paidPlanText());
	}
}
