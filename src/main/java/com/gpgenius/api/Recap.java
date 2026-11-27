package com.gpgenius.api;

import com.google.gson.annotations.SerializedName;

/** Reply from GET /v1/recap: last Monday-to-Monday week (UTC), all characters. */
public class Recap
{
	@SerializedName("week_start")
	public String weekStart;
	@SerializedName("week_end")
	public String weekEnd;
	public long profit;
	public int flips;
	public long tax;
	public double roi;
	@SerializedName("win_rate")
	public double winRate;
	@SerializedName("previous_profit")
	public long previousProfit;
	public long change;
	@SerializedName("best_item")
	public BestItem bestItem;
	public Goal goal;
	@SerializedName("dashboard_url")
	public String dashboardUrl;

	public static class BestItem
	{
		@SerializedName("item_id")
		public int itemId;
		public String name;
		public long profit;
		public int flips;
	}

	public static class Goal
	{
		public String kind;
		public long target;
		public long progress;
		public String status;
		public double fraction;
	}
}
