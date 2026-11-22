package com.gpgenius.suggestion;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import com.google.gson.Gson;
import com.gpgenius.api.Suggestion;
import java.util.Arrays;
import org.junit.Test;

public class MarketWarningsTest
{
	private static final String JSON = "{\"action\":\"sell\",\"reason\":\"Sell your Abyssal whip at 1,620,000 gp. "
		+ "Prices for Abyssal whip are unstable right now, so the price may move before your offer fills.\","
		+ "\"item_id\":4151,\"name\":\"Abyssal whip\",\"market_notices\":["
		+ "{\"item_id\":null,\"name\":null,\"message\":\"Prices may move more than usual right now.\"},"
		+ "{\"item_id\":4151,\"name\":\"Abyssal whip\",\"message\":\"Prices for Abyssal whip are unstable right now, "
		+ "so the price may move before your offer fills.\"}]}";

	@Test
	public void readsNoticesAndSkipsWhatTheReasonSays()
	{
		Suggestion s = new Gson().fromJson(JSON, Suggestion.class);
		assertEquals(2, s.marketNotices.size());
		assertEquals(Arrays.asList("Prices may move more than usual right now."), s.warnings(s.reason));
		// Simple mode hides the reason, so every notice shows.
		assertEquals(2, s.warnings("").size());
	}

	@Test
	public void olderServersSendNoNotices()
	{
		Suggestion s = new Gson().fromJson("{\"action\":\"wait\",\"reason\":\"Nothing to do\"}", Suggestion.class);
		assertTrue(s.warnings(s.reason).isEmpty());
	}
}
