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
	/** Buys: what is left of this account's 4-hour buy limit, when the server has seen earlier buys. */
	@SerializedName("limit_remaining")
	public Integer limitRemaining;
	/** ISO-8601 time the buy limit resets. */
	@SerializedName("limit_resets_at")
	public String limitResetsAt;
	/** Sells: average paid per item, and the sell price that gets it back after tax. */
	@SerializedName("cost_basis")
	public Long costBasis;
	@SerializedName("break_even")
	public Long breakEven;
	/** True when this sell loses coins. */
	public boolean loss;
	/** The server's pick for a loss: hold, relist or cut. */
	public String choice;
	public List<Choice> choices = new ArrayList<>();
	/** Items held back from sale until their price recovers. */
	public List<Hold> holds = new ArrayList<>();

	/** One way to handle a loss. The player still makes every GE click themselves. */
	public static class Choice
	{
		public String kind;
		public Long price;
		public Long profit;
		public String note;
	}

	public static class Hold
	{
		@SerializedName("item_id")
		public int itemId;
		public String name;
		public int quantity;
		@SerializedName("cost_basis")
		public long costBasis;
		@SerializedName("break_even")
		public long breakEven;
		@SerializedName("price_now")
		public long priceNow;
		public String until;
	}

	/** The next steps for every free slot, when asked for. The first step is this suggestion. */
	public Plan plan;

	/** A "welcome back" plan. Advice only: the player does each step by hand. */
	public static class Plan
	{
		public List<Suggestion> steps;
		@SerializedName("coins_used")
		public long coinsUsed;
		@SerializedName("expected_profit")
		public long expectedProfit;
	}

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

	/**
	 * Extra lines for the panel, shown in simple view too: what was paid and break-even on a sell,
	 * the other ways to handle a loss, what is being held, and how much buy limit is left.
	 */
	public List<String> details()
	{
		List<String> lines = new ArrayList<>();
		if (costBasis != null && breakEven != null)
		{
			lines.add(String.format("Paid %,d each. Break even at %,d after tax.", costBasis, breakEven));
		}
		if (loss)
		{
			for (Choice c : choices)
			{
				if (c.kind != null && !c.kind.equals(choice) && c.note != null)
				{
					lines.add("Or " + c.kind + ": " + c.note);
				}
			}
		}
		for (Hold h : holds)
		{
			lines.add(String.format("Holding %,d x %s: paid %,d, buyers pay %,d, break even %,d.",
				h.quantity, h.name, h.costBasis, h.priceNow, h.breakEven));
		}
		if (is(BUY) && limitRemaining != null && quantity != null && quantity >= limitRemaining)
		{
			lines.add(String.format("Uses the last %,d of your 4-hour buy limit.", limitRemaining));
		}
		return lines;
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
		String verb = is(BUY) ? "Buy" : loss ? "Sell (loss)" : "Sell";
		return String.format("%s %,d x %s at %,d", verb, quantity == null ? 0 : quantity, name, price == null ? 0 : price);
	}
}
