package com.osrsflipper.api;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Body of POST /v1/suggestion. Field names match the server's JSON.
 */
public class SuggestionRequest
{
	public Profile profile = new Profile();
	public Account account = new Account();

	public static class Profile
	{
		public String risk;
		@SerializedName("timeframe_minutes")
		public int timeframeMinutes;
		@SerializedName("f2p_only")
		public boolean f2pOnly;
		@SerializedName("blocked_item_ids")
		public List<Integer> blockedItemIds = new ArrayList<>();
		@SerializedName("min_profit_per_flip")
		public long minProfitPerFlip;
	}

	public static class Account
	{
		public long coins;
		public List<Offer> offers = new ArrayList<>();
		/** Bought and not yet listed, by item ID. */
		public Map<Integer, Integer> held;
		public boolean members;
		@SerializedName("reserved_slots")
		public int reservedSlots;
		@SerializedName("sell_only")
		public boolean sellOnly;
		@SerializedName("skip_item_ids")
		public List<Integer> skipItemIds = new ArrayList<>();
	}

	public static class Offer
	{
		public int slot;
		@SerializedName("item_id")
		public int itemId;
		@SerializedName("is_buy")
		public boolean isBuy;
		public long price;
		@SerializedName("total_quantity")
		public int totalQuantity;
		@SerializedName("filled_quantity")
		public int filledQuantity;
		public boolean active;
	}
}
