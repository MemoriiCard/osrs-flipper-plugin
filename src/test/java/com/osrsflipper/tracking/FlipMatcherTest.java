package com.osrsflipper.tracking;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.junit.Test;

public class FlipMatcherTest
{
	private static Transaction tx(boolean buy, int quantity, long totalCoins, long time)
	{
		return new Transaction(4151, "Abyssal whip", buy, quantity, totalCoins, time, 0);
	}

	@Test
	public void sellMatchesOldestBuysFirst()
	{
		FlipMatcher matcher = new FlipMatcher();
		assertNull(matcher.add(tx(true, 2, 2 * 1_500_000L, 1)));
		assertNull(matcher.add(tx(true, 2, 2 * 1_600_000L, 2)));

		Flip flip = matcher.add(tx(false, 3, 3 * 1_700_000L, 3));
		assertEquals(3, flip.quantity);
		assertEquals((2 * 1_500_000L + 1_600_000L) / 3, flip.buyPrice);
		assertEquals(1, flip.buyTimeMillis);
		// 2% tax on 1.7m is 34,000 per item.
		assertEquals(3 * 34_000L, flip.tax);
		assertEquals(3 * 1_700_000L - 3 * 34_000L - (2 * 1_500_000L + 1_600_000L), flip.profit);
		assertEquals(Integer.valueOf(1), matcher.getHoldings().get(4151));
	}

	@Test
	public void sellWithNoTrackedBuyIsIgnored()
	{
		FlipMatcher matcher = new FlipMatcher();
		assertNull(matcher.add(tx(false, 1, 1_700_000L, 1)));
	}

	@Test
	public void oversellOnlyCountsMatchedQuantity()
	{
		FlipMatcher matcher = new FlipMatcher();
		matcher.add(tx(true, 1, 1_000_000L, 1));
		Flip flip = matcher.add(tx(false, 3, 3_300_000L, 2));
		assertEquals(1, flip.quantity);
		assertEquals(1_100_000L - 22_000L - 1_000_000L, flip.profit);
		assertNull(matcher.getHoldings().get(4151));
	}
}
