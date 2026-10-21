package com.osrsflipper.api;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;

/** Reply from /v1/prices/{id}/history: average prices per 5-minute or 1-hour bucket, oldest first. */
public class PriceHistory
{
	@SerializedName("item_id")
	public int itemId;
	public String name;
	public String period;
	public List<Point> points = new ArrayList<>();

	public static class Point
	{
		/** Bucket start, Unix seconds. */
		public long t;
		/** Average instant-buy price; null when nothing traded at that side in the bucket. */
		public Long high;
		public Long low;
		@SerializedName("high_volume")
		public long highVolume;
		@SerializedName("low_volume")
		public long lowVolume;
	}
}
