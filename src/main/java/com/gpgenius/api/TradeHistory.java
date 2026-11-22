package com.gpgenius.api;

import com.google.gson.annotations.SerializedName;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Reply from GET /v1/history: the player's own completed trades with times, prices, GE tax and
 * gross and net profit, plus totals and chart series over the same trades. Nothing else.
 */
public class TradeHistory
{
	public String since;
	public String until;
	public Summary summary = new Summary();
	public List<Point> running = new ArrayList<>();
	public List<Bucket> distribution = new ArrayList<>();
	@SerializedName("hold_times")
	public List<Bucket> holdTimes = new ArrayList<>();
	@SerializedName("top_items")
	public List<ItemTotal> topItems = new ArrayList<>();
	@SerializedName("worst_items")
	public List<ItemTotal> worstItems = new ArrayList<>();
	public List<Trade> trades = new ArrayList<>();
	/** Pass back as before_id for the next page; null when there are no more. */
	@SerializedName("next_before_id")
	public Long nextBeforeId;

	public static class Summary
	{
		public int trades;
		public int wins;
		public int losses;
		public long quantity;
		public long cost;
		public long revenue;
		public long tax;
		@SerializedName("gross_profit")
		public long grossProfit;
		@SerializedName("net_profit")
		public long netProfit;
		public long best;
		public long worst;
		@SerializedName("avg_hold_minutes")
		public double avgHoldMinutes;
		@SerializedName("win_rate")
		public double winRate;
		@SerializedName("return_on_coins")
		public double returnOnCoins;
	}

	public static class Point
	{
		public String at;
		@SerializedName("net_profit")
		public long netProfit;
	}

	public static class Bucket
	{
		public String label;
		public int trades;
		@SerializedName("net_profit")
		public long netProfit;
	}

	public static class ItemTotal
	{
		@SerializedName("item_id")
		public int itemId;
		public String item;
		public int trades;
		public long quantity;
		@SerializedName("gross_profit")
		public long grossProfit;
		public long tax;
		@SerializedName("net_profit")
		public long netProfit;
	}

	public static class Trade
	{
		public long id;
		public String account;
		@SerializedName("item_id")
		public int itemId;
		public String item;
		public long quantity;
		@SerializedName("buy_price")
		public long buyPrice;
		@SerializedName("sell_price")
		public long sellPrice;
		@SerializedName("opened_at")
		public String openedAt;
		@SerializedName("closed_at")
		public String closedAt;
		@SerializedName("hold_minutes")
		public long holdMinutes;
		public long cost;
		public long revenue;
		public long tax;
		@SerializedName("gross_profit")
		public long grossProfit;
		@SerializedName("net_profit")
		public long netProfit;
		public boolean imported;

		public Instant opened()
		{
			return parse(openedAt);
		}

		public Instant closed()
		{
			return parse(closedAt);
		}
	}

	/** An ISO time from the server, or null if it can't be read. */
	public static Instant parse(String iso)
	{
		if (iso == null)
		{
			return null;
		}
		try
		{
			return Instant.parse(iso);
		}
		catch (RuntimeException e)
		{
			try
			{
				// Times without a zone are UTC.
				return java.time.LocalDateTime.parse(iso).toInstant(java.time.ZoneOffset.UTC);
			}
			catch (RuntimeException ignored)
			{
				return null;
			}
		}
	}

	/** "45 min", "3h 05m" or "2d 4h". */
	public static String hold(long minutes)
	{
		Duration d = Duration.ofMinutes(Math.max(0, minutes));
		if (d.toDays() >= 1)
		{
			return d.toDays() + "d " + d.minusDays(d.toDays()).toHours() + "h";
		}
		if (d.toHours() >= 1)
		{
			return d.toHours() + "h " + String.format("%02dm", d.minusHours(d.toHours()).toMinutes());
		}
		return d.toMinutes() + " min";
	}
}
