package com.gpgenius.notify;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.gpgenius.tracking.OfferStatus;
import org.junit.Test;

public class CheckInDigestTest
{
	private static final long MINUTE = 60_000;
	private static final long WINDOW = 30 * MINUTE;
	private static final long T0 = 1_800_000_000_000L;

	@Test
	public void groupsEverythingIntoOneReminderPerWindow()
	{
		CheckInDigest digest = new CheckInDigest();
		digest.seen(T0);
		digest.offerChanged(0, OfferStatus.SELLING, true, T0);
		digest.offerChanged(1, OfferStatus.SELLING, true, T0);
		digest.offerChanged(2, OfferStatus.BUYING, true, T0);

		digest.offerChanged(0, OfferStatus.SOLD, true, T0 + 5 * MINUTE);
		digest.flipProfit(100_000, T0 + 5 * MINUTE);
		digest.offerChanged(1, OfferStatus.SOLD, true, T0 + 6 * MINUTE);
		digest.flipProfit(84_500, T0 + 6 * MINUTE);
		digest.suggestion("abort", "Abyssal whip", "Abort Abyssal whip offer", true, true, T0 + 7 * MINUTE);

		// Not until a whole window has passed since the last one.
		assertFalse(digest.due(T0 + 10 * MINUTE, WINDOW, false));
		assertTrue(digest.due(T0 + WINDOW, WINDOW, false));
		assertEquals("GP Genius: 2 sells done, +184k. 1 offer to reprice (Abyssal whip).", digest.drain(T0 + WINDOW));

		digest.offerChanged(2, OfferStatus.BOUGHT, true, T0 + WINDOW + MINUTE);
		assertFalse(digest.due(T0 + WINDOW + 10 * MINUTE, WINDOW, false));
		assertTrue(digest.due(T0 + 2 * WINDOW, WINDOW, false));
		assertEquals("GP Genius: 1 buy done.", digest.drain(T0 + 2 * WINDOW));
		assertTrue(digest.isEmpty());
	}

	@Test
	public void waitsBrieflySoEventsTogetherArriveTogether()
	{
		CheckInDigest digest = new CheckInDigest();
		digest.offerChanged(0, OfferStatus.BUYING, true, T0);
		digest.offerChanged(0, OfferStatus.BOUGHT, true, T0 + WINDOW);
		assertFalse(digest.due(T0 + WINDOW + 10_000, WINDOW, false));
		assertTrue(digest.due(T0 + WINDOW + CheckInDigest.SETTLE_MILLIS, WINDOW, false));
	}

	@Test
	public void loginReplaysAndRepeatsAreNotCompletions()
	{
		CheckInDigest digest = new CheckInDigest();
		assertFalse(digest.offerChanged(0, OfferStatus.BOUGHT, false, T0));
		assertFalse(digest.offerChanged(0, OfferStatus.BOUGHT, true, T0));
		// The first update seen for a slot only seeds it.
		assertFalse(digest.offerChanged(1, OfferStatus.SOLD, true, T0));
		assertTrue(digest.isEmpty());
	}

	@Test
	public void quietHoursHoldRemindersAndWorkAcrossMidnight()
	{
		assertTrue(CheckInDigest.inQuietHours(23, 23, 8));
		assertTrue(CheckInDigest.inQuietHours(3, 23, 8));
		assertFalse(CheckInDigest.inQuietHours(8, 23, 8));
		assertTrue(CheckInDigest.inQuietHours(13, 12, 14));
		assertFalse(CheckInDigest.inQuietHours(14, 12, 14));
		assertFalse(CheckInDigest.inQuietHours(5, 5, 5));

		CheckInDigest digest = new CheckInDigest();
		digest.offerChanged(0, OfferStatus.BUYING, true, T0);
		digest.offerChanged(0, OfferStatus.BOUGHT, true, T0 + WINDOW);
		assertFalse(digest.due(T0 + 2 * WINDOW, WINDOW, true));
		assertTrue(digest.due(T0 + 2 * WINDOW, WINDOW, false));
	}

	@Test
	public void nextStepOnlyWhenASlotFrees()
	{
		CheckInDigest digest = new CheckInDigest();
		digest.seen(T0);
		digest.suggestion("buy", "Fire rune", "Buy 1,000 x Fire rune at 5", true, true, T0);
		digest.drain(T0);
		digest.suggestion("buy", "Shark", "Buy 50 x Shark at 900", true, true, T0 + MINUTE);
		assertTrue("buy straight after buy is a refresh, not news", digest.isEmpty());

		digest.suggestion("wait", null, "Wait", true, true, T0 + 2 * MINUTE);
		digest.suggestion("sell", "Shark", "Sell 50 x Shark at 950", true, true, T0 + 3 * MINUTE);
		assertEquals("GP Genius: Next: Sell 50 x Shark at 950.", digest.drain(T0 + WINDOW));

		digest.suggestion("wait", null, "Wait", true, false, T0 + WINDOW);
		digest.suggestion("sell", "Shark", "Sell 50 x Shark at 960", true, false, T0 + WINDOW);
		assertTrue("turned off in settings", digest.isEmpty());
	}

	@Test
	public void seenAtTheGeClearsNews()
	{
		CheckInDigest digest = new CheckInDigest();
		digest.offerChanged(0, OfferStatus.BUYING, true, T0);
		digest.offerChanged(0, OfferStatus.BOUGHT, true, T0 + MINUTE);
		digest.seen(T0 + 2 * MINUTE);
		assertTrue(digest.isEmpty());
	}

	@Test
	public void formatsCoins()
	{
		assertEquals("+950 gp", CheckInDigest.signed(950));
		assertEquals("+184k", CheckInDigest.signed(184_500));
		assertEquals("-1.2m", CheckInDigest.signed(-1_250_000));
		assertEquals("+2b", CheckInDigest.signed(2_000_000_000L));
	}

	@Test
	public void notesWaitForTheWindowUnlessAnUrgentOneWasLetThrough()
	{
		CheckInDigest digest = new CheckInDigest();
		digest.seen(T0);
		digest.note("Sell now: Abyssal whip has reached your 1,600,000 target", false, T0 + MINUTE);
		assertFalse(digest.due(T0 + 5 * MINUTE, WINDOW, false));
		assertTrue(digest.due(T0 + WINDOW, WINDOW, false));
		assertEquals("GP Genius: Sell now: Abyssal whip has reached your 1,600,000 target.", digest.drain(T0 + WINDOW));

		digest.note("Sell now: buyers pay 1,610,000 for Abyssal whip", true, T0 + WINDOW + MINUTE);
		// Urgent: goes out once things that happen together have settled, not a whole window later.
		assertFalse(digest.due(T0 + WINDOW + MINUTE, WINDOW, false));
		assertTrue(digest.due(T0 + WINDOW + 2 * MINUTE, WINDOW, false));
		// Quiet hours still win.
		assertFalse(digest.due(T0 + WINDOW + 2 * MINUTE, WINDOW, true));
		digest.drain(T0 + WINDOW + 2 * MINUTE);

		for (int i = 0; i < 5; i++)
		{
			digest.note("Dump " + i, false, T0 + 3 * WINDOW);
		}
		assertEquals("GP Genius: Dump 0. Dump 1. Dump 2. +2 more in the panel.", digest.drain(T0 + 4 * WINDOW));
	}
}
