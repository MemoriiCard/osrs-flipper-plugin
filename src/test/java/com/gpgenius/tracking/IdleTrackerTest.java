package com.gpgenius.tracking;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class IdleTrackerTest
{
	private static final long MIN = 60_000;

	@Test
	public void countsIdleTimeUntilAnOfferIsWorking()
	{
		IdleTracker t = new IdleTracker();
		t.start(0, 3);
		t.slot(0, true, 10 * MIN);
		t.slot(1, true, 0);
		// Slot 0 sat empty 10 minutes, slot 1 none, slot 2 all 30.
		assertEquals(10 * MIN, t.idleMillis(0, 30 * MIN));
		assertEquals(0, t.idleMillis(1, 30 * MIN));
		assertEquals(30 * MIN, t.idleMillis(2, 30 * MIN));
		assertEquals(40.0 / 90.0, t.idleShare(30 * MIN), 1e-9);
		// The offer finishes and waits to be collected: idle again.
		t.slot(0, false, 30 * MIN);
		assertEquals(15 * MIN, t.idleMillis(0, 35 * MIN));
		// Slots past what the world allows don't count.
		assertEquals(0, t.idleMillis(5, 35 * MIN));
	}

	@Test
	public void repeatedStatesChangeNothing()
	{
		IdleTracker t = new IdleTracker();
		t.start(0, 8);
		t.slot(3, true, 5 * MIN);
		t.slot(3, true, 9 * MIN);
		assertEquals(5 * MIN, t.idleMillis(3, 20 * MIN));
	}

	@Test
	public void unusedCashIsCoinsNoBuyOfferUses()
	{
		IdleTracker t = new IdleTracker();
		t.start(0, 8);
		t.cash(30_000_000, 70_000_000);
		assertEquals(30_000_000, t.unusedCash());
		assertEquals(0.3, t.unusedShare(), 1e-9);
		t.cash(0, 0);
		assertEquals(0.0, t.unusedShare(), 1e-9);
	}

	@Test
	public void stoppedTrackerReportsNothing()
	{
		IdleTracker t = new IdleTracker();
		assertFalse(t.running());
		t.slot(0, true, 5);
		assertEquals(0, t.idleMillis(0, 10));
		assertEquals(0.0, t.idleShare(10), 0);
	}

	@Test
	public void durations()
	{
		assertEquals("45s", IdleTracker.duration(45_000));
		assertEquals("12m", IdleTracker.duration(12 * MIN));
		assertEquals("1h 05m", IdleTracker.duration(65 * MIN));
	}
}
