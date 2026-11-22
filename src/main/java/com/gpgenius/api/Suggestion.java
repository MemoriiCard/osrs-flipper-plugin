package com.gpgenius.api;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;

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
	/** Sent back with feedback. */
	@SerializedName("suggestion_id")
	public String suggestionId;
	/** False when the suggestion came from the free item pool. */
	public boolean premium;
	/** Plain-language market warnings: the whole market, then the suggested item and the player's own. */
	@SerializedName("market_notices")
	public List<MarketNotice> marketNotices;

	public static class MarketNotice
	{
		/** Null for a notice about the whole market. */
		@SerializedName("item_id")
		public Integer itemId;
		public String name;
		public String message;
	}

	/** Warnings to show under the suggestion, leaving out any the shown reason already says. */
	public List<String> warnings(String shownReason)
	{
		List<String> out = new ArrayList<>();
		if (marketNotices == null)
		{
			return out;
		}
		for (MarketNotice n : marketNotices)
		{
			if (n == null || n.message == null || n.message.isEmpty() || out.contains(n.message))
			{
				continue;
			}
			if (shownReason != null && shownReason.contains(n.message))
			{
				continue;
			}
			out.add(n.message);
		}
		return out;
	}

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
