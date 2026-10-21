package com.osrsflipper.market;

/**
 * Whether an active GE offer is priced to fill at today's market. A buy offer fills while it pays
 * at least what sellers are taking (the low price); a sell offer fills while it asks no more than
 * buyers are paying (the high price).
 */
public enum SlotColor
{
	NONE,
	COMPETITIVE,
	OUTBID;

	public static SlotColor of(boolean isBuy, long offerPrice, MarketPrice market)
	{
		if (market == null || offerPrice <= 0)
		{
			return NONE;
		}
		if (isBuy)
		{
			return market.low == null ? NONE : offerPrice >= market.low ? COMPETITIVE : OUTBID;
		}
		return market.high == null ? NONE : offerPrice <= market.high ? COMPETITIVE : OUTBID;
	}
}
