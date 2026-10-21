package com.osrsflipper.tracking;

/**
 * Mirrors RuneLite's GrandExchangeOfferState so the tracking code has no client dependency
 * and can be unit tested on its own.
 */
public enum OfferStatus
{
	EMPTY,
	BUYING,
	BOUGHT,
	CANCELLED_BUY,
	SELLING,
	SOLD,
	CANCELLED_SELL;

	public boolean isBuy()
	{
		return this == BUYING || this == BOUGHT || this == CANCELLED_BUY;
	}

	public boolean isActive()
	{
		return this == BUYING || this == SELLING;
	}
}
