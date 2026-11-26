package com.gpgenius.suggestion;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.gpgenius.api.Suggestion;
import com.gpgenius.tracking.OfferSnapshot;
import com.gpgenius.tracking.OfferStatus;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

public class RaiseFollowUpTest
{
	static Suggestion fix(String kind)
	{
		Suggestion s = new Suggestion();
		s.action = Suggestion.ABORT;
		s.offerFix = kind;
		s.itemId = 4151;
		s.name = "Abyssal whip";
		s.slot = 2;
		s.price = 1_560_001L;
		s.quantity = 8;
		s.expectedProfit = 220_000L;
		return s;
	}

	private static Map<Integer, OfferSnapshot> slot(OfferStatus status)
	{
		Map<Integer, OfferSnapshot> slots = new HashMap<>();
		slots.put(2, new OfferSnapshot(2, 4151, status, 1_550_000L, 10, 2, 3_100_000L));
		return slots;
	}

	@Test
	public void handsBackTheRebuyOnceTheRaisedOfferIsAborted()
	{
		RaiseFollowUp f = new RaiseFollowUp();
		f.shown(fix(Suggestion.FIX_RAISE));
		assertTrue(f.pending());
		assertNull(f.ready(slot(OfferStatus.BUYING)));

		Suggestion rebuy = f.ready(slot(OfferStatus.CANCELLED_BUY));
		assertEquals(Suggestion.BUY, rebuy.action);
		assertEquals(Integer.valueOf(4151), rebuy.itemId);
		assertEquals(Long.valueOf(1_560_001L), rebuy.price);
		assertEquals(Integer.valueOf(8), rebuy.quantity);
		// Only once.
		assertNull(f.ready(new HashMap<>()));
		assertFalse(f.pending());
	}

	@Test
	public void anEmptiedSlotCountsAsAborted()
	{
		RaiseFollowUp f = new RaiseFollowUp();
		f.shown(fix(Suggestion.FIX_RAISE));
		assertEquals(Suggestion.BUY, f.ready(new HashMap<>()).action);
	}

	@Test
	public void otherSuggestionsForgetTheRaise()
	{
		RaiseFollowUp f = new RaiseFollowUp();
		f.shown(fix(Suggestion.FIX_RAISE));
		f.shown(fix(Suggestion.FIX_SELL_FILLED));
		assertNull(f.ready(new HashMap<>()));
		f.shown(fix(Suggestion.FIX_LOWER));
		assertNull(f.ready(new HashMap<>()));
		f.shown(null);
		assertFalse(f.pending());
	}

	@Test
	public void fixLabelsAndColours()
	{
		assertEquals("RAISE", fix(Suggestion.FIX_RAISE).pillLabel());
		assertEquals(Suggestion.BUY, fix(Suggestion.FIX_RAISE).colorAction());
		assertEquals("LOWER", fix(Suggestion.FIX_LOWER).pillLabel());
		assertEquals("SELL PART", fix(Suggestion.FIX_SELL_FILLED).pillLabel());
		assertEquals(Suggestion.SELL, fix(Suggestion.FIX_SELL_FILLED).colorAction());

		Suggestion plain = fix(null);
		assertNull(plain.fix());
		assertEquals("ABORT", plain.pillLabel());
		assertEquals(Suggestion.ABORT, plain.colorAction());

		// An unknown fix from a newer server, or a raise without a price, reads as a plain abort.
		assertNull(fix("something_new").fix());
		Suggestion noPrice = fix(Suggestion.FIX_RAISE);
		noPrice.price = null;
		assertNull(noPrice.fix());
		assertNull(noPrice.rebuy());
	}
}
