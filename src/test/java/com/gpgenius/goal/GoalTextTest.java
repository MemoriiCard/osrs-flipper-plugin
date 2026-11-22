package com.gpgenius.goal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.gson.Gson;
import com.gpgenius.api.Progress;
import java.time.Instant;
import org.junit.Test;

public class GoalTextTest
{
	private static final String JSON = "{"
		+ "\"windows\":[\"hour\",\"12h\",\"day\",\"week\",\"month\",\"all\",\"session\"],"
		+ "\"labels\":{\"hour\":\"1 hour\",\"session\":\"Session\"},"
		+ "\"current_id\":\"a\","
		+ "\"characters\":["
		+ "{\"id\":\"a\",\"name\":\"Main\",\"session_per_hour\":1200,\"windows\":{\"hour\":{\"profit\":500,\"flips\":2}},"
		+ "\"suggested_stack\":200000000,\"suggested_stack_label\":\"200M\"},"
		+ "{\"id\":\"b\",\"name\":\"Alt\",\"windows\":{\"hour\":{\"profit\":-50,\"flips\":1}}}],"
		+ "\"combined\":{\"id\":null,\"name\":\"All characters\",\"windows\":{\"hour\":{\"profit\":450,\"flips\":3}}},"
		+ "\"goal\":{\"kind\":\"stack\",\"target\":100000000,\"progress\":62400000,\"fraction\":0.25,\"remaining\":37600000,"
		+ "\"deadline\":\"2026-11-27T23:59:00Z\",\"account_id\":\"a\",\"account_name\":\"Main\",\"adapt\":true,"
		+ "\"status\":\"behind\",\"status_text\":\"Behind\",\"needed_per_hour\":350000,\"preset\":\"active\","
		+ "\"preset_label\":\"Active\",\"check_in_minutes\":15}}";

	private final Progress progress = new Gson().fromJson(JSON, Progress.class);

	@Test
	public void scopesPickTheRightCharacter()
	{
		assertEquals("Main", progress.scope(Progress.CURRENT).name);
		assertEquals("Alt", progress.scope("b").name);
		assertEquals(450, progress.scope(Progress.ALL).window("hour").profit);
		// Unknown ids fall back to this character; missing windows read as zero.
		assertEquals("Main", progress.scope("gone").name);
		assertEquals(0, progress.scope("b").window("week").profit);
		assertEquals("1 hour", progress.label("hour"));
		assertEquals("week", progress.label("week"));
		assertEquals("200M", progress.current().suggestedStackLabel);
	}

	@Test
	public void goalCardWords()
	{
		Progress.Goal g = progress.goal;
		assertEquals("Reach a 100M stack on Main", GoalText.title(g));
		assertEquals("62.4M of 100M · 5d 23h left",
			GoalText.progress(g, Instant.parse("2026-11-22T00:00:00Z")));
		String advice = GoalText.advice(g);
		assertTrue(advice, advice.startsWith("About 350K an hour to go."));
		assertTrue(advice, advice.endsWith("Following the goal: Active, check in every 15 minutes."));
		g.status = "done";
		assertEquals("Goal reached. Set a new one when you're ready.", GoalText.advice(g));
		assertEquals("time's up", GoalText.timeLeft("2026-11-20T00:00:00+00:00", Instant.parse("2026-11-22T00:00:00Z")));
		assertEquals("2 hours", GoalText.minutes(120));
		assertEquals("hour", GoalText.minutes(60));
	}
}
