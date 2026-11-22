package com.gpgenius.goal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.time.DayOfWeek;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import org.junit.Test;

public class GoalInputTest
{
	@Test
	public void parsesAmountsLikeTheServer()
	{
		assertEquals(Long.valueOf(100_000_000L), GoalInput.parseAmount("100m"));
		assertEquals(Long.valueOf(1_500_000_000L), GoalInput.parseAmount(" 1.5B "));
		assertEquals(Long.valueOf(250_000_000L), GoalInput.parseAmount("250,000k"));
		assertEquals(Long.valueOf(12_345L), GoalInput.parseAmount("12345gp"));
		assertNull(GoalInput.parseAmount(""));
		assertNull(GoalInput.parseAmount("m"));
		assertNull(GoalInput.parseAmount("-5m"));
		assertNull(GoalInput.parseAmount("NaN"));
		assertNull(GoalInput.parseAmount("1e400"));
		assertNull(GoalInput.parseAmount(null));
	}

	@Test
	public void deadlinesEndEachDayAndSkipTodayWhenItsNearlyOver()
	{
		ZoneId zone = ZoneId.of("Europe/London");
		// Sunday 22 November 2026, 10:00.
		ZonedDateTime morning = ZonedDateTime.of(2026, 11, 22, 10, 0, 0, 0, zone);
		List<GoalInput.Deadline> days = GoalInput.deadlines(morning);
		assertEquals(GoalInput.DAYS_AHEAD + 1, days.size());
		assertTrue(days.get(0).toString().startsWith("Today"));
		assertEquals(23, days.get(0).at.getHour());
		int friday = GoalInput.indexOf(days, DayOfWeek.FRIDAY);
		assertEquals("Fri 27 Nov", days.get(friday).toString());

		ZonedDateTime late = ZonedDateTime.of(2026, 11, 22, 23, 30, 0, 0, zone);
		assertTrue(GoalInput.deadlines(late).get(0).toString().startsWith("Tomorrow"));
	}
}
