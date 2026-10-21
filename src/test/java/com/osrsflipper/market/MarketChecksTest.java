package com.osrsflipper.market;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.osrsflipper.api.Suggestion;
import org.junit.Test;

public class MarketChecksTest
{
	private static final MarketPrice WHIP = new MarketPrice(4151, 1_560_000L, 1_620_000L);

	@Test
	public void slotColours()
	{
		assertEquals(SlotColor.COMPETITIVE, SlotColor.of(true, 1_560_000, WHIP));
		assertEquals(SlotColor.OUTBID, SlotColor.of(true, 1_500_000, WHIP));
		assertEquals(SlotColor.COMPETITIVE, SlotColor.of(false, 1_620_000, WHIP));
		assertEquals(SlotColor.OUTBID, SlotColor.of(false, 1_700_000, WHIP));
		assertEquals(SlotColor.NONE, SlotColor.of(true, 1, null));
		assertEquals(SlotColor.NONE, SlotColor.of(true, 1, new MarketPrice(1, null, 5L)));
	}

	@Test
	public void guardCatchesTypos()
	{
		// An extra zero on a buy.
		String extraZero = ConfirmGuard.check(false, 4151, 15_600_000, WHIP, null, 10);
		assertNotNull(extraZero);
		assertTrue(extraZero.contains("above"));
		// A missing zero on a sell.
		assertNotNull(ConfirmGuard.check(true, 4151, 156_000, WHIP, null, 10));
		// Normal prices pass.
		assertNull(ConfirmGuard.check(false, 4151, 1_561_000, WHIP, null, 10));
		assertNull(ConfirmGuard.check(true, 4151, 1_619_000, WHIP, null, 10));
	}

	@Test
	public void guardComparesWithTheSuggestion()
	{
		Suggestion buy = new Suggestion();
		buy.action = Suggestion.BUY;
		buy.itemId = 4151;
		buy.price = 1_560_001L;
		assertNotNull(ConfirmGuard.check(false, 4151, 1_300_000, null, buy, 10));
		assertNull(ConfirmGuard.check(false, 4151, 1_560_001, null, buy, 10));
		// A different item, or a sell while a buy is suggested, isn't compared.
		assertNull(ConfirmGuard.check(false, 554, 1, null, buy, 10));
		assertNull(ConfirmGuard.check(true, 4151, 1_300_000, null, buy, 10));
		// Off means off.
		assertNull(ConfirmGuard.check(false, 4151, 15_600_000, WHIP, buy, 0));
	}
}
