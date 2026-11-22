package com.gpgenius.setup;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.gpgenius.GpGeniusConfig.Category;
import com.gpgenius.GpGeniusConfig.CheckIn;
import com.gpgenius.GpGeniusConfig.ItemScope;
import com.gpgenius.GpGeniusConfig.RiskLevel;
import java.util.Map;
import org.junit.Test;

public class ProfileChoicesTest
{
	@Test
	public void onlySetChoicesAreSaved()
	{
		ProfileChoices.Values v = new ProfileChoices.Values();
		Map<String, Object> plain = ProfileChoices.toChoices(v);
		assertEquals(2, plain.size());
		assertEquals("medium", plain.get("risk"));
		assertEquals(30, plain.get("timeframe_minutes"));
		// No minimum profit is sent, so the preset's own applies on the server.
		assertFalse(plain.containsKey("min_profit_per_flip"));

		v.itemScope = ItemScope.F2P;
		v.coinCap = 5_000_000;
		v.maxItemPercent = 150;
		v.categories.add(Category.RUNES);
		Map<String, Object> set = ProfileChoices.toChoices(v);
		assertEquals(true, set.get("f2p_only"));
		assertEquals(5_000_000, set.get("coin_cap"));
		assertEquals(100, set.get("max_item_percent"));
		assertTrue(set.get("categories").toString().contains("runes"));
	}

	@Test
	public void switchingBackReadsWhatTheServerStored()
	{
		ProfileChoices.Values v = new ProfileChoices.Values();
		v.risk = RiskLevel.HIGH;
		v.checkIn = CheckIn.HOURS_4;
		v.itemScope = ItemScope.MEMBERS;
		v.categories.add(Category.GEAR);
		v.coinCap = 20_000_000;
		v.minProfit = 1_000;
		// Round-trip through JSON as the server would, so numbers come back as doubles.
		Gson gson = new Gson();
		Map<String, Object> stored = gson.fromJson(gson.toJson(ProfileChoices.toChoices(v)),
			new TypeToken<Map<String, Object>>()
			{
			}.getType());

		ProfileChoices.Values back = ProfileChoices.fromChoices(stored, Preset.byId(Preset.BUILT_IN, "overnight"));
		assertEquals(RiskLevel.HIGH, back.risk);
		assertEquals(CheckIn.HOURS_4, back.checkIn);
		assertEquals(ItemScope.MEMBERS, back.itemScope);
		assertTrue(back.categories.contains(Category.GEAR));
		assertEquals(20_000_000, back.coinCap);
		assertEquals(1_000, back.minProfit);
	}

	@Test
	public void emptyChoicesStartFromThePreset()
	{
		ProfileChoices.Values v = ProfileChoices.fromChoices(null, Preset.byId(Preset.BUILT_IN, "overnight"));
		assertEquals(RiskLevel.LOW, v.risk);
		assertEquals(CheckIn.HOURS_8, v.checkIn);
		assertEquals(0, v.coinCap);
	}
}
