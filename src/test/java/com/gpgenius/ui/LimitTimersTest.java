package com.gpgenius.ui;

import static org.junit.Assert.assertEquals;
import com.gpgenius.api.Suggestion;
import org.junit.Test;

public class LimitTimersTest
{
	@Test
	public void countsDown()
	{
		long reset = LimitTimers.resetsAtMillis("2026-11-26T14:00:00Z");
		assertEquals(LimitTimers.resetsAtMillis("2026-11-26T14:00:00+00:00"), reset);
		assertEquals("1:05:09", LimitTimers.countdown(reset, reset - (3600 + 5 * 60 + 9) * 1000L));
		assertEquals("4:32", LimitTimers.countdown(reset, reset - 272_000L));
		assertEquals("0:01", LimitTimers.countdown(reset, reset - 500));
		assertEquals("Resetting", LimitTimers.countdown(reset, reset + 1));
		assertEquals("", LimitTimers.countdown(-1, 0));
		assertEquals(-1, LimitTimers.resetsAtMillis("soon"));
		assertEquals(-1, LimitTimers.resetsAtMillis(null));
	}

	@Test
	public void notesSayWhyAnItemIsSkipped()
	{
		Suggestion.BuyLimit whip = new Suggestion.BuyLimit();
		whip.bought = 70;
		whip.limit = 70;
		whip.locked = true;
		assertEquals("70 / 70 bought", LimitTimers.usage(whip));
		assertEquals("At its limit for 4:32. Other items are suggested meanwhile.",
			LimitTimers.note(whip, 272_000L, 0));
		Suggestion.BuyLimit runes = new Suggestion.BuyLimit();
		runes.bought = 5000;
		runes.limit = 25000;
		runes.remaining = 20000;
		assertEquals("20,000 left, resets in 4:32", LimitTimers.note(runes, 272_000L, 0));
		assertEquals("20,000 left", LimitTimers.note(runes, -1, 0));
	}

	@Test
	public void suggestionReadsCuesAndLimits()
	{
		Suggestion s = new com.google.gson.Gson().fromJson("{\"action\":\"buy\",\"sell_cues\":[{\"item_id\":4151,"
			+ "\"name\":\"Abyssal whip\",\"kind\":\"instant\",\"price\":1600000,\"urgent\":true,\"message\":\"m\"}],"
			+ "\"buy_limits\":[{\"item_id\":554,\"name\":\"Fire rune\",\"bought\":10,\"limit\":25000,\"remaining\":24990,"
			+ "\"resets_at\":null,\"locked\":false}]}", Suggestion.class);
		assertEquals("cue:instant:4151", s.sellCues.get(0).key());
		assertEquals(24990, s.buyLimits.get(0).remaining);
		Suggestion old = new com.google.gson.Gson().fromJson("{\"action\":\"wait\"}", Suggestion.class);
		assertEquals(0, old.sellCues.size());
	}
}
