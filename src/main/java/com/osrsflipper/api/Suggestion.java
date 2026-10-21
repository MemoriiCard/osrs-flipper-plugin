package com.osrsflipper.api;

import com.google.gson.annotations.SerializedName;

/**
 * One next action from the server: buy, sell, abort or wait.
 */
public class Suggestion
{
	public static final String BUY = "buy";
	public static final String SELL = "sell";
	public static final String ABORT = "abort";
	public static final String WAIT = "wait";

	public String action;
	public String reason;
	@SerializedName("item_id")
	public Integer itemId;
	public String name;
	public Long price;
	public Integer quantity;
	public Integer slot;
	@SerializedName("expected_profit")
	public Long expectedProfit;

	public boolean is(String type)
	{
		return type.equals(action);
	}

	/** Short description for the side panel, such as "Buy 30 x Abyssal whip at 1,560,001". */
	public String headline()
	{
		if (is(WAIT) || itemId == null)
		{
			return "Wait";
		}
		if (is(ABORT))
		{
			return "Abort " + name + " offer";
		}
		String verb = is(BUY) ? "Buy" : "Sell";
		return String.format("%s %,d x %s at %,d", verb, quantity == null ? 0 : quantity, name, price == null ? 0 : price);
	}
}
