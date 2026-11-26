package com.gpgenius.api;

import com.google.gson.annotations.SerializedName;

/** The player's own history with one item, from /v1/items/{id}/history. */
public class ItemHistory
{
	@SerializedName("item_id")
	public int itemId;
	public String name;
	public int flips;
	public int wins;
	@SerializedName("win_rate")
	public double winRate;
	public long profit;
	@SerializedName("avg_margin")
	public long avgMargin;
	@SerializedName("avg_hold_minutes")
	public int avgHoldMinutes;
	/** Ready to show, e.g. "You: 12 flips, +3.4M, 83% wins, 85,000 gp each, avg 41 min". */
	public String line;
}
