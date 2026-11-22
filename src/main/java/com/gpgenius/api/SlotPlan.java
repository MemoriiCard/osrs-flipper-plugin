package com.gpgenius.api;

import com.google.gson.annotations.SerializedName;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Reply from POST /v1/slots: what to do with every GE slot at this check-in.
 *
 * <p>The server decides everything; this class only turns its answer into text. Advice only: the
 * player still sets up, prices and confirms every offer by hand.</p>
 */
public class SlotPlan
{
	public static final String KEEP = "keep";
	public static final String COLLECT = "collect";
	public static final String RELIST = "relist";
	public static final String CUT = "cut";
	public static final String SELL = "sell";
	public static final String BUY = "buy";
	public static final String EMPTY = "empty";
	public static final String RESERVED = "reserved";

	/** The to-do list, in order. A slot can appear twice: collect it, then sell or buy in it. */
	public List<Step> steps;
	/** What each slot holds once the steps are done. */
	public List<Step> slots;
	public List<Hold> holds;
	@SerializedName("total_slots")
	public int totalSlots;
	@SerializedName("coins_used")
	public long coinsUsed;
	@SerializedName("coins_left")
	public long coinsLeft;
	@SerializedName("expected_profit")
	public long expectedProfit;
	@SerializedName("next_check_in_at")
	public String nextCheckInAt;
	public boolean premium;
	/** "overnight" for a plan made for logging off, "normal" otherwise. Older servers leave it out. */
	public String mode;

	public boolean isOvernight()
	{
		return "overnight".equals(mode);
	}

	public static class Step
	{
		public int slot;
		public String action;
		@SerializedName("item_id")
		public Integer itemId;
		public String name;
		@SerializedName("is_buy")
		public Boolean isBuy;
		public Long price;
		public Integer quantity;
		@SerializedName("expected_profit")
		public Long expectedProfit;
		public boolean loss;
		@SerializedName("limit_remaining")
		public Integer limitRemaining;
		@SerializedName("limit_resets_at")
		public String limitResetsAt;
		public String until;
		public Integer order;

		/** One line for the to-do list, such as "Relist Abyssal whip buy at 1,560,001 x 6". */
		public String todo()
		{
			String item = name == null ? "item" : name;
			String side = Boolean.TRUE.equals(isBuy) ? "buy" : "sell";
			switch (action == null ? "" : action)
			{
				case COLLECT:
					return "Collect " + item + " (slot " + (slot + 1) + ")";
				case RELIST:
					return "Abort and relist " + item + " " + side + " at " + amount();
				case CUT:
					if (Boolean.TRUE.equals(isBuy))
					{
						return "Abort " + item + " buy (slot " + (slot + 1) + ")";
					}
					return "Abort and sell " + item + " at " + amount();
				case SELL:
					return "Sell " + item + " at " + amount();
				case BUY:
					return "Buy " + item + " at " + amount();
				default:
					return item;
			}
		}

		/** What the slot holds, such as "Buying Fire rune" or "Free". */
		public String status()
		{
			String item = name == null ? "item" : name;
			switch (action == null ? "" : action)
			{
				case KEEP:
					return (Boolean.TRUE.equals(isBuy) ? "Buying " : "Selling ") + item + ": leave it";
				case RELIST:
				case CUT:
				case SELL:
				case BUY:
					return (Boolean.TRUE.equals(isBuy) ? "Buying " : "Selling ") + item;
				case RESERVED:
					return "Kept free for you";
				default:
					return "Free";
			}
		}

		private String amount()
		{
			return String.format("%,d x %,d", price == null ? 0 : price, quantity == null ? 0 : quantity);
		}
	}

	public static class Hold
	{
		@SerializedName("item_id")
		public int itemId;
		public String name;
		public int quantity;
		public String until;
	}

	/** Local time of the next check-in, such as "14:35", or null if the server didn't say. */
	public String nextCheckIn(ZoneId zone)
	{
		if (nextCheckInAt == null)
		{
			return null;
		}
		try
		{
			return DateTimeFormatter.ofPattern("HH:mm").withZone(zone).format(Instant.parse(nextCheckInAt));
		}
		catch (RuntimeException e)
		{
			return null;
		}
	}
}
