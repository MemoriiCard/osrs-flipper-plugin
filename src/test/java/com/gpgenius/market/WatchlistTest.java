package com.gpgenius.market;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Test;

public class WatchlistTest
{
	@Test
	public void parsesNamesAndTargetsInTheUsualWays()
	{
		List<Watchlist.Entry> entries = Watchlist.parse(
			"Abyssal whip, 1.4m, 1.6m\n"
				+ "# a comment\n"
				+ "\n"
				+ "Dragon bones, 2,100, 2,400\n"
				+ "Zulrah's scales,150,0\n"
				+ "4151; 1400000; 1600000\n"
				+ "Rune platebody\n"
				+ "Bandos chestplate,,20m\n",
			100);
		assertEquals(6, entries.size());
		assertEquals("Abyssal whip", entries.get(0).item);
		assertEquals(1_400_000, entries.get(0).buyTarget);
		assertEquals(1_600_000, entries.get(0).sellTarget);
		assertEquals(2_100, entries.get(1).buyTarget);
		assertEquals(2_400, entries.get(1).sellTarget);
		assertEquals(150, entries.get(2).buyTarget);
		assertEquals(0, entries.get(2).sellTarget);
		assertEquals(4151, entries.get(3).itemId());
		assertEquals(-1, entries.get(0).itemId());
		assertEquals(1_600_000, entries.get(3).sellTarget);
		assertEquals(0, entries.get(4).buyTarget);
		assertEquals(0, entries.get(5).buyTarget);
		assertEquals(20_000_000, entries.get(5).sellTarget);
	}

	@Test
	public void keepsToTheItemLimit()
	{
		String text = "a, 1\nb, 1\nc, 1\nd, 1\n";
		assertEquals(Watchlist.FREE_ITEMS, Watchlist.parse(text, Watchlist.FREE_ITEMS).size());
		assertEquals(4, Watchlist.parse(text, Watchlist.PREMIUM_ITEMS).size());
	}

	@Test
	public void readsAmounts()
	{
		assertEquals(250_000, Watchlist.amount("250k"));
		assertEquals(2_000_000_000L, Watchlist.amount("2B"));
		assertEquals(1_200_000, Watchlist.amount("1,200,000 gp"));
		assertEquals(0, Watchlist.amount("lots"));
		assertEquals(0, Watchlist.amount("-5"));
	}

	@Test
	public void eachTargetNotifiesOnceUntilThePriceMovesBack()
	{
		Watchlist watch = new Watchlist();
		Watchlist.Entry whip = new Watchlist.Entry("Abyssal whip", 1_400_000, 1_600_000);
		assertTrue(watch.check(4151, whip, new MarketPrice(4151, 1_450_000L, 1_500_000L)).isEmpty());

		List<Watchlist.Hit> hits = watch.check(4151, whip, new MarketPrice(4151, 1_390_000L, 1_500_000L));
		assertEquals(1, hits.size());
		assertTrue(hits.get(0).buy);
		assertEquals(1_390_000, hits.get(0).price);
		// Still under the target: no second notification.
		assertTrue(watch.check(4151, whip, new MarketPrice(4151, 1_380_000L, 1_500_000L)).isEmpty());
		// Back over, then under again: notifies again.
		assertTrue(watch.check(4151, whip, new MarketPrice(4151, 1_450_000L, 1_500_000L)).isEmpty());
		assertEquals(1, watch.check(4151, whip, new MarketPrice(4151, 1_400_000L, 1_500_000L)).size());

		hits = watch.check(4151, whip, new MarketPrice(4151, 1_450_000L, 1_610_000L));
		assertEquals(1, hits.size());
		assertFalse(hits.get(0).buy);
	}

	@Test
	public void targetsOfZeroAreOff()
	{
		Watchlist.Entry off = new Watchlist.Entry("x", 0, 0);
		assertFalse(Watchlist.buyReached(off, new MarketPrice(1, 1L, 1L)));
		assertFalse(Watchlist.sellReached(off, new MarketPrice(1, 1L, 1L)));
		assertFalse(Watchlist.buyReached(new Watchlist.Entry("x", 5, 0), new MarketPrice(1, null, 1L)));
	}
}
