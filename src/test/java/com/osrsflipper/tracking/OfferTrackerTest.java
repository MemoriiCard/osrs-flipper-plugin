package com.osrsflipper.tracking;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class OfferTrackerTest
{
	private static final int ITEM = 554; // Fire rune

	private static OfferSnapshot buy(int filled, long spent, OfferStatus status)
	{
		return new OfferSnapshot(0, ITEM, status, 5, 1000, filled, spent);
	}

	@Test
	public void partialFillsBecomeSeparateTransactions()
	{
		OfferTracker tracker = new OfferTracker();
		assertNull(tracker.update(buy(0, 0, OfferStatus.BUYING), "Fire rune", 1, true));

		Transaction first = tracker.update(buy(400, 2000, OfferStatus.BUYING), "Fire rune", 2, true);
		assertEquals(400, first.quantity);
		assertEquals(5, first.pricePerItem);
		assertTrue(first.buy);

		Transaction second = tracker.update(buy(1000, 4900, OfferStatus.BOUGHT), "Fire rune", 3, true);
		assertEquals(600, second.quantity);
		assertEquals(2900, second.totalCoins);
	}

	@Test
	public void loginBurstIsNotCountedTwice()
	{
		OfferTracker tracker = new OfferTracker();
		tracker.update(buy(400, 2000, OfferStatus.BUYING), "Fire rune", 1, true);

		// RuneLite reports the slot as EMPTY, then repeats its current state, at login.
		assertNull(tracker.update(new OfferSnapshot(0, 0, OfferStatus.EMPTY, 0, 0, 0, 0), "", 2, false));
		assertNull(tracker.update(buy(400, 2000, OfferStatus.BUYING), "Fire rune", 3, true));
	}

	@Test
	public void fillsWhileLoggedOutAreCountedOnce()
	{
		OfferTracker tracker = new OfferTracker();
		tracker.update(buy(400, 2000, OfferStatus.BUYING), "Fire rune", 1, true);
		Transaction t = tracker.update(buy(1000, 5000, OfferStatus.BOUGHT), "Fire rune", 2, false);
		assertEquals(600, t.quantity);
	}

	@Test
	public void newOfferAfterCollectingStartsFromZero()
	{
		OfferTracker tracker = new OfferTracker();
		tracker.update(buy(1000, 5000, OfferStatus.BOUGHT), "Fire rune", 1, true);
		tracker.update(new OfferSnapshot(0, 0, OfferStatus.EMPTY, 0, 0, 0, 0), "", 2, true);

		Transaction t = tracker.update(buy(300, 1500, OfferStatus.BUYING), "Fire rune", 3, true);
		assertEquals(300, t.quantity);
	}

	@Test
	public void identicalOfferPlacedWithoutSeeingEmptyIsTreatedAsNew()
	{
		OfferTracker tracker = new OfferTracker();
		tracker.update(buy(1000, 5000, OfferStatus.BOUGHT), "Fire rune", 1, true);

		// Filled went down, so this must be a new offer even though no EMPTY was seen.
		Transaction t = tracker.update(buy(200, 1000, OfferStatus.BUYING), "Fire rune", 2, false);
		assertEquals(200, t.quantity);
	}

	@Test
	public void cancelWithoutNewFillsRecordsNothing()
	{
		OfferTracker tracker = new OfferTracker();
		tracker.update(buy(400, 2000, OfferStatus.BUYING), "Fire rune", 1, true);
		assertNull(tracker.update(buy(400, 2000, OfferStatus.CANCELLED_BUY), "Fire rune", 2, true));
	}

	@Test
	public void activityTimeMovesOnlyWhenTheOfferFills()
	{
		OfferTracker tracker = new OfferTracker();
		tracker.update(buy(0, 0, OfferStatus.BUYING), "Fire rune", 1_000, true);
		tracker.update(buy(0, 0, OfferStatus.BUYING), "Fire rune", 5_000, true);
		assertEquals(1_000, tracker.getSlots().get(0).lastActivityMillis);

		tracker.update(buy(100, 500, OfferStatus.BUYING), "Fire rune", 9_000, true);
		assertEquals(9_000, tracker.getSlots().get(0).lastActivityMillis);

		// A new offer in the same slot starts its own clock.
		tracker.update(new OfferSnapshot(0, ITEM, OfferStatus.SELLING, 9, 100, 0, 0), "Fire rune", 12_000, true);
		assertEquals(12_000, tracker.getSlots().get(0).lastActivityMillis);
	}
}
