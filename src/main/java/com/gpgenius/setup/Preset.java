package com.gpgenius.setup;

import com.gpgenius.GpGeniusConfig.CheckIn;
import com.gpgenius.GpGeniusConfig.ItemScope;
import com.gpgenius.GpGeniusConfig.RiskLevel;

/**
 * Named starting points for the settings, picked in the first-run wizard or switched to later in
 * one click. A preset only changes settings; the player still does every trade by hand.
 */
public enum Preset
{
	AFK_OVERNIGHT("AFK overnight",
		"Patient offers with wide margins and buy limits that last, for checking once or twice a day.",
		RiskLevel.MEDIUM, CheckIn.HOURS_8, ItemScope.ANY, 25),
	ACTIVE_SESSION("Active session",
		"Quick flips that fill in minutes, for when you're at the GE and checking often.",
		RiskLevel.MEDIUM, CheckIn.MINUTES_15, ItemScope.ANY, 0),
	BANK_BUILDER("Low-risk bank builder",
		"Steady, high-volume items, with no more than a fifth of your coins in any one item.",
		RiskLevel.LOW, CheckIn.HOUR_1, ItemScope.ANY, 20),
	F2P_STARTER("F2P starter",
		"Free-to-play items at low risk, for a small stack or a new account.",
		RiskLevel.LOW, CheckIn.MINUTES_30, ItemScope.F2P, 34);

	public final String label;
	public final String description;
	public final RiskLevel risk;
	public final CheckIn checkIn;
	public final ItemScope itemScope;
	/** Max per item as a percentage of flipping coins; 0 means no extra cap. */
	public final int maxItemPercent;

	Preset(String label, String description, RiskLevel risk, CheckIn checkIn, ItemScope itemScope, int maxItemPercent)
	{
		this.label = label;
		this.description = description;
		this.risk = risk;
		this.checkIn = checkIn;
		this.itemScope = itemScope;
		this.maxItemPercent = maxItemPercent;
	}

	@Override
	public String toString()
	{
		return label;
	}

	/** First question: roughly how many coins. */
	public enum Coins
	{
		UNDER_1M("Under 1m"),
		TO_10M("1m to 10m"),
		TO_100M("10m to 100m"),
		OVER_100M("100m+");

		public final String label;

		Coins(String label)
		{
			this.label = label;
		}
	}

	/** Second question: how often the player will check. */
	public enum Often
	{
		MINUTES("Every few minutes", CheckIn.MINUTES_15),
		HOURLY("About hourly", CheckIn.HOUR_1),
		FEW_A_DAY("A few times a day", CheckIn.HOURS_4),
		DAILY("Once a day", CheckIn.HOURS_12);

		public final String label;
		public final CheckIn checkIn;

		Often(String label, CheckIn checkIn)
		{
			this.label = label;
			this.checkIn = checkIn;
		}
	}

	/** Third question: how much risk is OK. */
	public enum Appetite
	{
		SAFE("Play it safe", RiskLevel.LOW),
		BALANCED("Balanced", RiskLevel.MEDIUM),
		BOLD("Bigger margins", RiskLevel.HIGH);

		public final String label;
		public final RiskLevel risk;

		Appetite(String label, RiskLevel risk)
		{
			this.label = label;
			this.risk = risk;
		}
	}

	/** The preset that best fits the wizard's three answers. */
	public static Preset recommend(Coins coins, Often often, Appetite appetite)
	{
		if (coins == Coins.UNDER_1M)
		{
			return F2P_STARTER;
		}
		if (often == Often.FEW_A_DAY || often == Often.DAILY)
		{
			return AFK_OVERNIGHT;
		}
		if (appetite == Appetite.SAFE)
		{
			return BANK_BUILDER;
		}
		return ACTIVE_SESSION;
	}

	/** The settings to save: the preset, with the player's own check-in time and risk answers on top. */
	public static Settings settings(Preset preset, Often often, Appetite appetite)
	{
		Settings s = new Settings();
		s.preset = preset;
		s.risk = appetite == null ? preset.risk : appetite.risk;
		s.checkIn = often == null ? preset.checkIn : often.checkIn;
		s.itemScope = preset.itemScope;
		s.maxItemPercent = preset.maxItemPercent;
		return s;
	}

	/** What applying a preset writes to the config. */
	public static class Settings
	{
		public Preset preset;
		public RiskLevel risk;
		public CheckIn checkIn;
		public ItemScope itemScope;
		public int maxItemPercent;
	}
}
