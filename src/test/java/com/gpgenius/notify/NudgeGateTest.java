package com.gpgenius.notify;

import static org.junit.Assert.assertEquals;
import com.gpgenius.notify.NudgeGate.Decision;
import org.junit.Test;

public class NudgeGateTest
{
	private static final long MIN = 60_000L;
	private static final long WINDOW = 30 * MIN;

	@Test
	public void theSameNudgeIsSaidOncePerWindow()
	{
		NudgeGate gate = new NudgeGate();
		assertEquals(Decision.BATCH, gate.offer("cue:target:1", false, false, 0, WINDOW));
		assertEquals(Decision.DROP, gate.offer("cue:target:1", false, false, 10 * MIN, WINDOW));
		assertEquals(Decision.BATCH, gate.offer("cue:target:1", false, false, 31 * MIN, WINDOW));
		gate.forget("cue:target:1");
		assertEquals(Decision.BATCH, gate.offer("cue:target:1", false, false, 32 * MIN, WINDOW));
	}

	@Test
	public void onlyUrgentNudgesGoEarlyAndThoseAreCapped()
	{
		NudgeGate gate = new NudgeGate();
		assertEquals(Decision.EARLY, gate.offer("cue:instant:1", true, false, 0, WINDOW));
		// Too soon after the last early one: waits for the check-in.
		assertEquals(Decision.BATCH, gate.offer("cue:instant:2", true, false, 2 * MIN, WINDOW));
		assertEquals(Decision.EARLY, gate.offer("dump:3", true, false, 6 * MIN, WINDOW));
		// Two early reminders this window already.
		assertEquals(Decision.BATCH, gate.offer("dump:4", true, false, 20 * MIN, WINDOW));
		// A new window.
		assertEquals(Decision.EARLY, gate.offer("dump:5", true, false, 31 * MIN, WINDOW));
	}

	@Test
	public void quietHoursHoldUrgentNudgesBack()
	{
		NudgeGate gate = new NudgeGate();
		assertEquals(Decision.BATCH, gate.offer("cue:instant:1", true, true, 0, WINDOW));
		assertEquals(Decision.EARLY, gate.offer("cue:instant:2", true, false, MIN, WINDOW));
	}

	@Test
	public void longCheckInsRepeatLessOftenAndMemoryStaysSmall()
	{
		NudgeGate gate = new NudgeGate();
		long day = 24 * 60 * MIN;
		gate.offer("a", false, false, 0, day);
		assertEquals(Decision.DROP, gate.offer("a", false, false, 12 * 60 * MIN, day));
		for (int i = 0; i < 1000; i++)
		{
			gate.offer("k" + i, false, false, 2 * day + i, WINDOW);
		}
		gate.offer("late", false, false, 4 * day, WINDOW);
		assertEquals(1, gate.remembered());
	}
}
