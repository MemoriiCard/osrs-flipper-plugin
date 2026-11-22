package com.gpgenius.setup;

import com.gpgenius.GpGeniusConfig.Category;
import com.gpgenius.GpGeniusConfig.CheckIn;
import com.gpgenius.GpGeniusConfig.ItemScope;
import com.gpgenius.GpGeniusConfig.RiskLevel;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * The choices a saved profile holds, in both directions: from the plugin's settings to what is
 * saved on the server, and from a saved profile back to the settings when the player switches.
 * These are only the choices the player can see; anything a preset does beyond them is on the server.
 */
public final class ProfileChoices
{
	/** The settings a profile carries. 0 means "no cap" for the numbers, as in the plugin config. */
	public static class Values
	{
		public RiskLevel risk = RiskLevel.MEDIUM;
		public CheckIn checkIn = CheckIn.MINUTES_30;
		public ItemScope itemScope = ItemScope.ANY;
		public Set<Category> categories = EnumSet.noneOf(Category.class);
		public int coinCap;
		public int maxItemPercent;
		public int minItemPrice;
		public int maxItemPrice;
		public int minProfit;
	}

	private ProfileChoices()
	{
	}

	/** What to save: risk and check-in always, everything else only when the player set it. */
	public static Map<String, Object> toChoices(Values v)
	{
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("risk", v.risk.name().toLowerCase(Locale.ROOT));
		out.put("timeframe_minutes", v.checkIn.minutes);
		if (v.itemScope == ItemScope.F2P)
		{
			out.put("f2p_only", true);
		}
		else if (v.itemScope == ItemScope.MEMBERS)
		{
			out.put("members_only", true);
		}
		if (v.categories != null && !v.categories.isEmpty())
		{
			List<String> keys = new ArrayList<>();
			for (Category c : v.categories)
			{
				keys.add(c.key());
			}
			out.put("categories", keys);
		}
		putPositive(out, "coin_cap", v.coinCap);
		putPositive(out, "max_item_percent", Math.min(100, v.maxItemPercent));
		putPositive(out, "min_item_price", v.minItemPrice);
		putPositive(out, "max_item_price", v.maxItemPrice);
		putPositive(out, "min_profit_per_flip", v.minProfit);
		return out;
	}

	/** The settings to show after switching to a profile: its saved choices over the preset's start. */
	public static Values fromChoices(Map<String, Object> choices, Preset preset)
	{
		Values v = new Values();
		if (preset != null)
		{
			v.risk = preset.risk;
			v.checkIn = preset.checkIn;
		}
		if (choices == null)
		{
			return v;
		}
		Object risk = choices.get("risk");
		if (risk instanceof String)
		{
			v.risk = Preset.risk((String) risk);
		}
		Integer minutes = number(choices.get("timeframe_minutes"));
		if (minutes != null)
		{
			v.checkIn = Preset.nearestCheckIn(minutes);
		}
		if (Boolean.TRUE.equals(choices.get("f2p_only")))
		{
			v.itemScope = ItemScope.F2P;
		}
		else if (Boolean.TRUE.equals(choices.get("members_only")))
		{
			v.itemScope = ItemScope.MEMBERS;
		}
		Object categories = choices.get("categories");
		if (categories instanceof List)
		{
			for (Object key : (List<?>) categories)
			{
				for (Category c : Category.values())
				{
					if (c.key().equals(key))
					{
						v.categories.add(c);
					}
				}
			}
		}
		v.coinCap = orZero(choices.get("coin_cap"));
		v.maxItemPercent = orZero(choices.get("max_item_percent"));
		v.minItemPrice = orZero(choices.get("min_item_price"));
		v.maxItemPrice = orZero(choices.get("max_item_price"));
		v.minProfit = orZero(choices.get("min_profit_per_flip"));
		return v;
	}

	private static void putPositive(Map<String, Object> out, String key, int value)
	{
		if (value > 0)
		{
			out.put(key, value);
		}
	}

	private static int orZero(Object value)
	{
		Integer n = number(value);
		return n == null ? 0 : Math.max(0, n);
	}

	/** Gson reads JSON numbers in a map as doubles. */
	private static Integer number(Object value)
	{
		if (value instanceof Number)
		{
			double d = ((Number) value).doubleValue();
			return d >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) d;
		}
		return null;
	}
}
