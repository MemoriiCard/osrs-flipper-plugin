package com.gpgenius.setup;

import static org.junit.Assert.assertEquals;

import com.gpgenius.GpGeniusConfig;
import org.junit.Test;

public class PresetTest
{
	@Test
	public void recommendsFromTheThreeAnswers()
	{
		assertEquals(Preset.F2P_STARTER, Preset.recommend(Preset.Coins.UNDER_1M, Preset.Often.MINUTES, Preset.Appetite.BOLD));
		assertEquals(Preset.AFK_OVERNIGHT, Preset.recommend(Preset.Coins.TO_100M, Preset.Often.DAILY, Preset.Appetite.SAFE));
		assertEquals(Preset.AFK_OVERNIGHT, Preset.recommend(Preset.Coins.OVER_100M, Preset.Often.FEW_A_DAY, Preset.Appetite.BALANCED));
		assertEquals(Preset.BANK_BUILDER, Preset.recommend(Preset.Coins.TO_10M, Preset.Often.HOURLY, Preset.Appetite.SAFE));
		assertEquals(Preset.ACTIVE_SESSION, Preset.recommend(Preset.Coins.TO_10M, Preset.Often.MINUTES, Preset.Appetite.BALANCED));
	}

	@Test
	public void answersOverrideThePresetsCheckInAndRisk()
	{
		Preset.Settings s = Preset.settings(Preset.F2P_STARTER, Preset.Often.DAILY, Preset.Appetite.BOLD);
		assertEquals(GpGeniusConfig.ItemScope.F2P, s.itemScope);
		assertEquals(GpGeniusConfig.CheckIn.HOURS_12, s.checkIn);
		assertEquals(GpGeniusConfig.RiskLevel.HIGH, s.risk);

		Preset.Settings plain = Preset.settings(Preset.BANK_BUILDER, null, null);
		assertEquals(GpGeniusConfig.CheckIn.HOUR_1, plain.checkIn);
		assertEquals(GpGeniusConfig.RiskLevel.LOW, plain.risk);
		assertEquals(20, plain.maxItemPercent);
	}
}
