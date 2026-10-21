package com.osrsflipper.api;

import com.google.gson.annotations.SerializedName;

/** An item that was just sold far below its usual price, worth buying and selling back. */
public class DumpAlert
{
	public String id;
	@SerializedName("item_id")
	public int itemId;
	public String name;
	@SerializedName("buy_price")
	public long buyPrice;
	@SerializedName("sell_price")
	public long sellPrice;
	public int quantity;
	@SerializedName("expected_profit")
	public long expectedProfit;
	@SerializedName("drop_percent")
	public double dropPercent;
	@SerializedName("expires_at")
	public String expiresAt;
}
