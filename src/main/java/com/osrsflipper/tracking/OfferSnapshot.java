package com.osrsflipper.tracking;

/**
 * The state of one GE slot at a point in time.
 */
public class OfferSnapshot
{
	public int slot;
	public int itemId;
	public OfferStatus status;
	public int price;
	public int totalQuantity;
	public int quantityFilled;
	/** Total coins spent (buy) or received before tax (sell) so far. */
	public long spent;

	public OfferSnapshot()
	{
	}

	public OfferSnapshot(int slot, int itemId, OfferStatus status, int price, int totalQuantity, int quantityFilled, long spent)
	{
		this.slot = slot;
		this.itemId = itemId;
		this.status = status;
		this.price = price;
		this.totalQuantity = totalQuantity;
		this.quantityFilled = quantityFilled;
		this.spent = spent;
	}

	/**
	 * Whether {@code next} is a later state of the same offer rather than a new offer placed in this slot.
	 */
	boolean isSameOfferAs(OfferSnapshot next)
	{
		return itemId == next.itemId
			&& status.isBuy() == next.status.isBuy()
			&& price == next.price
			&& totalQuantity == next.totalQuantity
			&& next.quantityFilled >= quantityFilled
			&& next.spent >= spent;
	}
}
