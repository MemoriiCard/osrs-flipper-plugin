package com.gpgenius.notify;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import com.gpgenius.api.Suggestion;
import com.gpgenius.market.SlotColor;
import java.util.EnumSet;
import java.util.Set;
import org.junit.Test;

public class SoundCuesTest
{
	private static final Set<SoundCue> ALL = EnumSet.allOf(SoundCue.class);

	@Test
	public void offUnlessTurnedOnAndSoundsAllowed()
	{
		SoundCues cues = new SoundCues();
		assertFalse(cues.play(SoundCue.OFFER_DONE, true, EnumSet.noneOf(SoundCue.class), 0));
		assertFalse(cues.play(SoundCue.OFFER_DONE, false, ALL, 0));
		assertTrue(cues.play(SoundCue.OFFER_DONE, true, ALL, 0));
	}

	@Test
	public void burstsGiveOneSound()
	{
		SoundCues cues = new SoundCues();
		assertTrue(cues.play(SoundCue.OFFER_DONE, true, ALL, 1_000));
		// Another cue right after, and the same cue again soon after, stay quiet.
		assertFalse(cues.play(SoundCue.NEXT_STEP, true, ALL, 1_500));
		assertFalse(cues.play(SoundCue.OFFER_DONE, true, ALL, 1_000 + SoundCues.GAP_MILLIS + 1));
		assertTrue(cues.play(SoundCue.NEXT_STEP, true, ALL, 1_000 + SoundCues.GAP_MILLIS + 1));
		assertTrue(cues.play(SoundCue.OFFER_DONE, true, ALL, 1_000 + SoundCues.SAME_CUE_GAP_MILLIS));
	}

	@Test
	public void onlyANewStepCounts()
	{
		SoundCues cues = new SoundCues();
		Suggestion s = new Suggestion();
		s.action = Suggestion.BUY;
		s.itemId = 4151;
		assertTrue(cues.stepChanged(SoundCues.stepKey(s)));
		s.price = 1_000L;
		assertFalse(cues.stepChanged(SoundCues.stepKey(s)));
		s.action = Suggestion.WAIT;
		assertNull(SoundCues.stepKey(s));
		assertFalse(cues.stepChanged(null));
		s.action = Suggestion.BUY;
		assertTrue(cues.stepChanged(SoundCues.stepKey(s)));
	}

	@Test
	public void outbidOnlyWhenAFillingOfferIsPassed()
	{
		SoundCues cues = new SoundCues();
		// Seen outbid on first look (just logged in): quiet.
		assertFalse(cues.slotColor(0, SlotColor.OUTBID));
		assertFalse(cues.slotColor(1, SlotColor.COMPETITIVE));
		assertTrue(cues.slotColor(1, SlotColor.OUTBID));
		assertFalse(cues.slotColor(1, SlotColor.OUTBID));
		cues.clearSlot(1);
		assertFalse(cues.slotColor(1, SlotColor.OUTBID));
		cues.reset();
		assertEquals(null, SoundCues.stepKey(null));
	}
}
