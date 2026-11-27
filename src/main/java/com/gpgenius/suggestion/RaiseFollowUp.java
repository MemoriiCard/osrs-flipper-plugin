package com.gpgenius.suggestion;

import com.gpgenius.api.Suggestion;
import com.gpgenius.tracking.OfferSnapshot;
import java.util.Map;

/**
 * The GE can't change an offer's price, so "raise this buy" means abort it and buy again at the new
 * price. This remembers the raise and, once the player has aborted that offer, hands back the rebuy so
 * the highlights walk them through it. The player still does every click. Kept free of RuneLite types
 * so it can be unit tested.
 */
public final class RaiseFollowUp
{
	/** How long the rebuy stays on show before normal suggestions take over again. */
	public static final long PIN_MILLIS = 3 * 60_000;

	private Suggestion rebuy;
	private int slot = -1;
	private int itemId = -1;

	/** Call with every suggestion the server sends. A raise is remembered; anything else forgets it. */
	public void shown(Suggestion s)
	{
		Suggestion next = s == null ? null : s.rebuy();
		if (next != null && s.slot != null && s.itemId != null)
		{
			rebuy = next;
			slot = s.slot;
			itemId = s.itemId;
		}
		else
		{
			clear();
		}
	}

	/**
	 * The rebuy, once the raised offer's slot no longer holds that active buy. Handed back once.
	 *
	 * @param slots the last tracked state of each GE slot
	 */
	public Suggestion ready(Map<Integer, OfferSnapshot> slots)
	{
		if (rebuy == null || slots == null)
		{
			return null;
		}
		OfferSnapshot o = slots.get(slot);
		boolean stillThere = o != null && o.itemId == itemId && o.status != null
			&& o.status.isBuy() && o.status.isActive();
		if (stillThere)
		{
			return null;
		}
		Suggestion out = rebuy;
		clear();
		return out;
	}

	public boolean pending()
	{
		return rebuy != null;
	}

	public void clear()
	{
		rebuy = null;
		slot = -1;
		itemId = -1;
	}
}
