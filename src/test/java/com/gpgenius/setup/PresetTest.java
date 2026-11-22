package com.gpgenius.setup;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import com.gpgenius.GpGeniusConfig.CheckIn;
import com.gpgenius.GpGeniusConfig.RiskLevel;
import com.gpgenius.api.Profiles;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;

public class PresetTest
{
	@Test
	public void fiveBuiltInPresetsMatchTheServerIds()
	{
		assertEquals(5, Preset.BUILT_IN.size());
		assertEquals("overnight", Preset.byId(Preset.BUILT_IN, "overnight").id);
		assertNull(Preset.byId(Preset.BUILT_IN, "turbo"));
	}

	@Test
	public void serverListReplacesTheBuiltInOne()
	{
		Profiles.PresetInfo info = new Profiles.PresetInfo();
		info.id = "afk";
		info.label = "AFK";
		info.blurb = "Leave it.";
		info.risk = "low";
		info.checkInMinutes = 100;
		Preset p = Preset.fromServer(Collections.singletonList(info)).get(0);
		assertEquals("AFK", p.label);
		assertEquals(RiskLevel.LOW, p.risk);
		assertEquals(CheckIn.HOURS_2, p.checkIn);

		assertSame(Preset.BUILT_IN, Preset.fromServer(null));
		assertSame(Preset.BUILT_IN, Preset.fromServer(Arrays.asList(null, new Profiles.PresetInfo())));
	}

	@Test
	public void nearestCheckIn()
	{
		assertEquals(CheckIn.MINUTES_5, Preset.nearestCheckIn(1));
		assertEquals(CheckIn.HOURS_8, Preset.nearestCheckIn(480));
		assertEquals(CheckIn.DAY_1, Preset.nearestCheckIn(100_000));
	}
}
