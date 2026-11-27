package com.gpgenius.api;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reply from /v1/efficiency: GP per hour overall, this session, over the last hours and per GE slot,
 * as the player chose to show them. Only the player's own results; null figures are switched off.
 */
public class Efficiency
{
	/** overall, session, day, slots: each {enabled, days or hours}. */
	public Map<String, Map<String, Object>> prefs = new LinkedHashMap<>();
	public Rate overall;
	public Rate session;
	public Rate day;
	public List<SlotRate> slots;
	@SerializedName("slots_label")
	public String slotsLabel;

	public static class Rate
	{
		public String label;
		public long profit;
		public int flips;
		public double hours;
		/** Null until the window is 10 minutes old. */
		@SerializedName("per_hour")
		public Long perHour;
	}

	public static class SlotRate extends Rate
	{
		public int slot;
	}

	/** The figures to show, in order, skipping those switched off. */
	public List<Rate> rates()
	{
		List<Rate> out = new ArrayList<>();
		for (Rate r : new Rate[]{overall, session, day})
		{
			if (r != null)
			{
				out.add(r);
			}
		}
		return out;
	}

	public boolean enabled(String key)
	{
		Map<String, Object> p = prefs == null ? null : prefs.get(key);
		return p == null || !Boolean.FALSE.equals(p.get("enabled"));
	}

	public int window(String key, String field, int fallback)
	{
		Map<String, Object> p = prefs == null ? null : prefs.get(key);
		Object v = p == null ? null : p.get(field);
		return v instanceof Number ? ((Number) v).intValue() : fallback;
	}
}
