package com.gpgenius.notify;

import com.gpgenius.api.Suggestion;
import com.gpgenius.market.SlotColor;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Decides when a sound cue plays. Cues are rate limited so a burst of GE updates (login, a hop,
 * several offers finishing at once) gives one sound, not a string of them.
 *
 * <p>Client thread only. Free of RuneLite types so it can be unit tested.
 */
public class SoundCues
{
	/** No two cues closer than this. */
	static final long GAP_MILLIS = 2_000;
	/** The same cue at most this often. */
	static final long SAME_CUE_GAP_MILLIS = 10_000;

	private final Map<SoundCue, Long> lastPlayed = new EnumMap<>(SoundCue.class);
	private final Map<Integer, SlotColor> slotColors = new HashMap<>();
	private long lastAnyMillis = Long.MIN_VALUE / 2;
	private String lastStepKey;

	/**
	 * Whether to play this cue now. Records it as played when the answer is yes.
	 *
	 * @param soundsOn the profile's master "Notification sounds" setting
	 * @param enabled  the cues the player turned on
	 */
	public boolean play(SoundCue cue, boolean soundsOn, Set<SoundCue> enabled, long nowMillis)
	{
		if (!soundsOn || enabled == null || !enabled.contains(cue))
		{
			return false;
		}
		Long last = lastPlayed.get(cue);
		if (nowMillis - lastAnyMillis < GAP_MILLIS || (last != null && nowMillis - last < SAME_CUE_GAP_MILLIS))
		{
			return false;
		}
		lastAnyMillis = nowMillis;
		lastPlayed.put(cue, nowMillis);
		return true;
	}

	/**
	 * The suggestion shown changed. True when it's a different next step than before, so the
	 * NEXT_STEP cue should be tried. The same suggestion arriving again is not new.
	 */
	public boolean stepChanged(String stepKey)
	{
		boolean changed = stepKey != null && !stepKey.equals(lastStepKey);
		lastStepKey = stepKey;
		return changed;
	}

	/**
	 * One slot's colour this tick. True when an offer that was going to fill has just been passed by
	 * the market. The first look at a slot never counts, so logging in doesn't sound off.
	 */
	public boolean slotColor(int slot, SlotColor color)
	{
		SlotColor before = slotColors.put(slot, color == null ? SlotColor.NONE : color);
		return before == SlotColor.COMPETITIVE && color == SlotColor.OUTBID;
	}

	/** A slot was emptied or changed item; forget what it was. */
	public void clearSlot(int slot)
	{
		slotColors.remove(slot);
	}

	/** What makes two suggestions the same step: the action, item and slot, not the price tweak. */
	public static String stepKey(Suggestion s)
	{
		if (s == null || s.action == null || s.itemId == null || s.is(Suggestion.WAIT))
		{
			return null;
		}
		return s.action + ":" + s.itemId + ":" + (s.slot == null ? "" : s.slot);
	}

	/** Logged out or hopped. */
	public void reset()
	{
		slotColors.clear();
		lastStepKey = null;
	}
}
