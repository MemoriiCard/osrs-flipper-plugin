package com.osrsflipper.tracking;

import java.util.HashMap;
import java.util.Map;

/**
 * Turns a stream of GE slot snapshots into transactions.
 *
 * <p>Snapshots are compared with the last known state of the same slot, which is persisted between
 * sessions so fills that happened while logged out are counted once on the next login, and the
 * burst of offer events RuneLite sends at login is not counted twice.</p>
 */
public class OfferTracker
{
	private Map<Integer, OfferSnapshot> slots = new HashMap<>();

	/**
	 * @param snapshot the slot's new state
	 * @param itemName name to record on the transaction
	 * @param nowMillis time of the update
	 * @param trustEmpty whether an EMPTY state means the slot was really cleared. RuneLite reports
	 *                   every slot as EMPTY briefly during login, so callers pass false until the
	 *                   login burst is over.
	 * @return the transaction this update represents, or null if nothing was filled
	 */
	public Transaction update(OfferSnapshot snapshot, String itemName, long nowMillis, boolean trustEmpty)
	{
		if (snapshot.status == OfferStatus.EMPTY)
		{
			if (trustEmpty)
			{
				slots.remove(snapshot.slot);
			}
			return null;
		}

		OfferSnapshot previous = slots.get(snapshot.slot);
		slots.put(snapshot.slot, snapshot);

		int filledBefore = 0;
		long spentBefore = 0;
		if (previous != null && previous.isSameOfferAs(snapshot))
		{
			filledBefore = previous.quantityFilled;
			spentBefore = previous.spent;
		}

		int quantity = snapshot.quantityFilled - filledBefore;
		long coins = snapshot.spent - spentBefore;
		if (quantity <= 0)
		{
			return null;
		}
		return new Transaction(snapshot.itemId, itemName, snapshot.status.isBuy(), quantity, coins, nowMillis, snapshot.slot);
	}

	public Map<Integer, OfferSnapshot> getSlots()
	{
		return slots;
	}
}
