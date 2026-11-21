package com.gpgenius.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.gson.Gson;
import java.util.List;
import org.junit.Test;

public class SuggestionTest
{
	private static final Gson GSON = new Gson();

	@Test
	public void lossSellShowsBreakEvenAndTheOtherChoices()
	{
		Suggestion s = GSON.fromJson("{\"action\":\"sell\",\"item_id\":4151,\"name\":\"Abyssal whip\","
			+ "\"price\":1734694,\"quantity\":3,\"cost_basis\":1700000,\"break_even\":1734694,\"loss\":true,"
			+ "\"choice\":\"relist\",\"choices\":["
			+ "{\"kind\":\"hold\",\"note\":\"Keep it out of the GE.\"},"
			+ "{\"kind\":\"relist\",\"price\":1734694,\"profit\":0,\"note\":\"List at 1,734,694.\"},"
			+ "{\"kind\":\"cut\",\"price\":1619999,\"profit\":-307203,\"note\":\"Sell at 1,619,999 now.\"}]}",
			Suggestion.class);
		List<String> lines = s.details();
		assertEquals("Paid 1,700,000 each. Break even at 1,734,694 after tax.", lines.get(0));
		assertEquals("Or hold: Keep it out of the GE.", lines.get(1));
		assertEquals("Or cut: Sell at 1,619,999 now.", lines.get(2));
		assertEquals(3, lines.size());
		assertTrue(s.headline().startsWith("Sell (loss) 3 x Abyssal whip"));
	}

	@Test
	public void buyAtTheLimitAndHeldItemsAreListed()
	{
		Suggestion s = GSON.fromJson("{\"action\":\"buy\",\"item_id\":554,\"name\":\"Fire rune\",\"price\":5,"
			+ "\"quantity\":200,\"limit_remaining\":200,\"holds\":[{\"item_id\":4151,\"name\":\"Abyssal whip\","
			+ "\"quantity\":3,\"cost_basis\":1650000,\"break_even\":1683674,\"price_now\":1619999}]}",
			Suggestion.class);
		List<String> lines = s.details();
		assertEquals("Holding 3 x Abyssal whip: paid 1,650,000, buyers pay 1,619,999, break even 1,683,674.",
			lines.get(0));
		assertEquals("Uses the last 200 of your 4-hour buy limit.", lines.get(1));
	}

	@Test
	public void oldServersWithoutTheNewFieldsAddNothing()
	{
		Suggestion s = GSON.fromJson("{\"action\":\"sell\",\"item_id\":4151,\"name\":\"Abyssal whip\","
			+ "\"price\":1619999,\"quantity\":3}", Suggestion.class);
		assertTrue(s.details().isEmpty());
		assertEquals("Sell 3 x Abyssal whip at 1,619,999", s.headline());
	}
}
