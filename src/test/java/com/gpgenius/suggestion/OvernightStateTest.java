package com.gpgenius.suggestion;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class OvernightStateTest
{
	@Test
	public void startsStopsAndTurnsItselfOff()
	{
		OvernightState s = new OvernightState();
		assertFalse(s.active(0));
		s.start(10, 1_000);
		assertTrue(s.active(1_000));
		assertEquals(10, s.getHours());
		assertTrue(s.active(1_000 + OvernightState.SETUP_MILLIS - 1));
		assertFalse(s.active(1_000 + OvernightState.SETUP_MILLIS));
		s.start(8, 0);
		s.stop();
		assertFalse(s.active(1));
	}

	@Test
	public void hoursAreClampedToWhatTheServerAccepts()
	{
		OvernightState s = new OvernightState();
		s.start(1, 0);
		assertEquals(OvernightState.MIN_HOURS, s.getHours());
		s.start(99, 0);
		assertEquals(OvernightState.MAX_HOURS, s.getHours());
		for (int h : OvernightState.CHOICES)
		{
			assertEquals(h, OvernightState.clamp(h));
		}
	}
}
