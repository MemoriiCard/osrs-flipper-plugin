package com.gpgenius.api;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;

/** Reply from GET /v1/conversions: set and decant flips (premium, opt-in). */
public class Conversions
{
	@SerializedName("premium_required")
	public boolean premiumRequired;
	public List<Opportunity> opportunities = new ArrayList<>();

	public static class Leg
	{
		@SerializedName("item_id")
		public int itemId;
		public String name;
		public int quantity;
		public long price;
	}

	public static class Opportunity
	{
		/** set_pack, set_unpack or decant */
		public String kind;
		public String title;
		/** What to do at the NPC, in words. */
		public String exchange;
		public List<Leg> buys = new ArrayList<>();
		public List<Leg> sells = new ArrayList<>();
		public long cost;
		public long profit;
		@SerializedName("thinnest_hourly_volume")
		public long thinnestHourlyVolume;
	}
}
