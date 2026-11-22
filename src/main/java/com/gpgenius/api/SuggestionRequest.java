package com.gpgenius.api;

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
	/** RuneLite's account hash, as a string so no precision is lost. */
	@SerializedName("account_hash")
	public String accountHash;
	@SerializedName("display_name")
	public String displayName;
	/** Also send the next steps for every free slot. */
	public boolean plan;
	/** Buys from the plan the player is working through, so the list stays put. */
	@SerializedName("planned_item_ids")
	public List<Integer> plannedItemIds = new ArrayList<>();
	/** A fast-start preset id, or null. The server decides what it means. */
	public String preset;
	/** The saved profile in use, or null. Its preset and saved choices are applied on the server. */
	@SerializedName("profile_id")
	public String profileId;
	/** "overnight" while the player is getting ready to log off; null (left out) otherwise. */
	public String mode;
	/** Hours the player will be away, for overnight mode. Null lets the server choose. */
	@SerializedName("away_hours")
	public Integer awayHours;
	/** Let the player's goal pick the preset and check-in rhythm while it runs (when the goal allows it). */
	@SerializedName("follow_goal")
	public boolean followGoal = true;

	public static class Profile
	{
		public String risk;
		@SerializedName("timeframe_minutes")
		public int timeframeMinutes;
		@SerializedName("f2p_only")
		public boolean f2pOnly;
		@SerializedName("blocked_item_ids")
		public List<Integer> blockedItemIds = new ArrayList<>();
		/** Null (left out) lets a preset's own minimum apply. */
		@SerializedName("min_profit_per_flip")
		public Long minProfitPerFlip;
		@SerializedName("allow_shared_items")
		public boolean allowSharedItems;
		/** auto, hold, relist or cut. */
		@SerializedName("loss_handling")
		public String lossHandling = "auto";
		/** Null means no cap. */
		@SerializedName("coin_cap")
		public Long coinCap;
		@SerializedName("max_item_percent")
		public Integer maxItemPercent;
		@SerializedName("min_item_price")
		public Long minItemPrice;
		@SerializedName("max_item_price")
		public Long maxItemPrice;
		@SerializedName("favourite_item_ids")
		public List<Integer> favouriteItemIds = new ArrayList<>();
		public List<String> categories = new ArrayList<>();
		@SerializedName("members_only")
		public boolean membersOnly;
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
		/** Minutes since the offer was placed or last filled; null when the plugin didn't see it placed. */
		@SerializedName("idle_minutes")
		public Integer idleMinutes;
	}
}
