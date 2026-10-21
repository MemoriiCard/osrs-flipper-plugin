package com.osrsflipper.tracking;

/**
 * Grand Exchange sales tax: 2% of the sale price per item, rounded down, capped at 5m per item.
 * Items selling for under 50 gp round down to no tax.
 *
 * <p>TODO: a few items (such as Old school bonds) are exempt; add the exempt list before the
 * paid beta.</p>
 */
public final class GeTax
{
	static final double RATE = 0.02;
	static final long MAX_TAX_PER_ITEM = 5_000_000L;

	private GeTax()
	{
	}

	public static long perItem(long pricePerItem)
	{
		return Math.min((long) Math.floor(pricePerItem * RATE), MAX_TAX_PER_ITEM);
	}

	public static long total(long pricePerItem, int quantity)
	{
		return perItem(pricePerItem) * quantity;
	}
}
