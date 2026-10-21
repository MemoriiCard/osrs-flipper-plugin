package com.osrsflipper.market;

import com.google.gson.annotations.SerializedName;

/** Latest instant-buy (high) and instant-sell (low) prices for one item, from the server. */
public class MarketPrice
{
	@SerializedName("item_id")
	public int itemId;
	public Long high;
	public Long low;

	public MarketPrice()
	{
	}

	public MarketPrice(int itemId, Long low, Long high)
	{
		this.itemId = itemId;
		this.low = low;
		this.high = high;
	}
}
