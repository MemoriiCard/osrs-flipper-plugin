package com.gpgenius.setup;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The first-run guide: cash stack, risk, how often the player checks in, and how many GE slots to
 * use, ending with their first suggestion. Starting values come from what the plugin can see: coins
 * (and platinum tokens) in the inventory, coins in the bank while it is open, whether the world is a
 * members world, and how many GE slots already hold offers. The player confirms or changes each one.
 *
 * <p>Plain Java with no RuneLite types, so it can be tested on its own. The plugin turns {@link Choices}
 * into settings with {@link #settings(Choices)}.
 */
public final class Onboarding
{
	public static final int COINS = 995;
	public static final int PLATINUM_TOKEN = 13204;
	public static final int PLATINUM_VALUE = 1000;
	public static final int MEMBERS_SLOTS = 8;
	public static final int FREE_SLOTS = 3;

	public enum Step
	{
		CASH("Your cash stack", "How many coins should suggestions work with?"),
		RISK("Risk", "Steady, high-volume items, or jumpier ones with bigger margins?"),
		TIMEFRAME("How often you check", "Fast flips if you check often, patient offers if you don't."),
		SLOTS("GE slots", "How many Grand Exchange slots should suggestions use?"),
		DONE("All set", "Here's what you picked. Your first suggestion is one click away.");

		public final String title;
		public final String question;

		Step(String title, String question)
		{
			this.title = title;
			this.question = question;
		}

		public Step next()
		{
			return this == DONE ? DONE : values()[ordinal() + 1];
		}

		public Step back()
		{
			return this == CASH ? CASH : values()[ordinal() - 1];
		}

		/** 1-based position, for "Step 2 of 4". DONE counts as the last. */
		public int number()
		{
			return Math.min(ordinal() + 1, values().length - 1);
		}
	}

	/** Risk keys, matching the plugin's RiskLevel names. */
	public static final List<String> RISKS = Collections.unmodifiableList(Arrays.asList("LOW", "MEDIUM", "HIGH"));
	/** Check-in times in minutes, matching the plugin's CheckIn choices. */
	public static final List<Integer> CHECK_INS = Collections.unmodifiableList(Arrays.asList(5, 15, 30, 60, 120, 240, 480, 720, 1440));

	/** What the plugin could see when the guide opened (or last refreshed). */
	public static final class Detected
	{
		public long inventoryCoins;
		/** -1 when the bank hasn't been opened this session. */
		public long bankCoins = -1;
		public boolean members;
		public int slotsInUse;

		public boolean bankSeen()
		{
			return bankCoins >= 0;
		}

		public long total()
		{
			return inventoryCoins + Math.max(0, bankCoins);
		}

		public int slotsAvailable()
		{
			return members ? MEMBERS_SLOTS : FREE_SLOTS;
		}
	}

	/** What the player settled on. */
	public static final class Choices
	{
		/** 0 means "all the coins in my inventory", as in the plugin's Coins to flip with. */
		public long cashCap;
		public String risk = "MEDIUM";
		public int checkInMinutes = 30;
		public int slotsToUse = MEMBERS_SLOTS;
		public int slotsAvailable = MEMBERS_SLOTS;
	}

	private Onboarding()
	{
	}

	/** Counts coins and platinum tokens in an item container, as item id to quantity. */
	public static long coinsIn(Map<Integer, Long> items)
	{
		if (items == null)
		{
			return 0;
		}
		long coins = items.getOrDefault(COINS, 0L);
		long platinum = items.getOrDefault(PLATINUM_TOKEN, 0L);
		return coins + platinum * PLATINUM_VALUE;
	}

	public static Detected detect(Map<Integer, Long> inventory, Map<Integer, Long> bank, boolean members, int slotsInUse)
	{
		Detected d = new Detected();
		d.inventoryCoins = coinsIn(inventory);
		d.bankCoins = bank == null ? -1 : coinsIn(bank);
		d.members = members;
		d.slotsInUse = Math.max(0, slotsInUse);
		return d;
	}

	/**
	 * Starting answers. Cash: everything in the inventory when the bank holds nothing more (cap 0),
	 * otherwise the inventory alone, since suggestions can only spend coins the player carries.
	 * Slots: every slot the world allows.
	 */
	public static Choices defaults(Detected d, String currentRisk, int currentCheckIn)
	{
		Choices c = new Choices();
		c.cashCap = 0;
		c.risk = RISKS.contains(currentRisk) ? currentRisk : "MEDIUM";
		c.checkInMinutes = CHECK_INS.contains(currentCheckIn) ? currentCheckIn : 30;
		c.slotsAvailable = d.slotsAvailable();
		c.slotsToUse = d.slotsAvailable();
		return c;
	}

	/** A short line about the detected coins for the cash step. */
	public static String cashHint(Detected d)
	{
		StringBuilder s = new StringBuilder();
		s.append("You have ").append(gp(d.inventoryCoins)).append(" in your inventory");
		if (d.bankSeen())
		{
			s.append(" and ").append(gp(d.bankCoins)).append(" in your bank");
			if (d.bankCoins > 0)
			{
				s.append(". Withdraw what you want to flip with: suggestions only spend coins you carry");
			}
		}
		else
		{
			s.append(". Open your bank to count those coins too");
		}
		return s.append('.').toString();
	}

	/** A short line about the slots for the slots step. */
	public static String slotsHint(Detected d)
	{
		String world = d.members ? "a members world" : "a free world";
		String inUse = d.slotsInUse > 0 ? String.format(" %d already hold offers.", d.slotsInUse) : "";
		return String.format("You're on %s, so you have %d GE slots.%s", world, d.slotsAvailable(), inUse);
	}

	/** Reserved slots for the plugin's settings: slots the player keeps for their own trades. */
	public static int reservedSlots(Choices c)
	{
		int use = Math.max(1, Math.min(c.slotsToUse, c.slotsAvailable));
		return Math.max(0, Math.min(7, c.slotsAvailable - use));
	}

	/** The plugin settings the choices become, by config key. Enum settings are given by name. */
	public static Map<String, Object> settings(Choices c)
	{
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("coinCap", (int) Math.max(0, Math.min(Integer.MAX_VALUE, c.cashCap)));
		out.put("riskLevel", RISKS.contains(c.risk) ? c.risk : "MEDIUM");
		out.put("checkIn", checkInName(c.checkInMinutes));
		out.put("reservedSlots", reservedSlots(c));
		out.put("setupDone", true);
		return out;
	}

	/** The CheckIn enum name for a number of minutes. */
	public static String checkInName(int minutes)
	{
		switch (minutes)
		{
			case 5:
				return "MINUTES_5";
			case 15:
				return "MINUTES_15";
			case 60:
				return "HOUR_1";
			case 120:
				return "HOURS_2";
			case 240:
				return "HOURS_4";
			case 480:
				return "HOURS_8";
			case 720:
				return "HOURS_12";
			case 1440:
				return "DAY_1";
			default:
				return "MINUTES_30";
		}
	}

	/** The summary on the last step. */
	public static List<String> summary(Choices c)
	{
		String cash = c.cashCap <= 0 ? "All the coins in your inventory" : "Up to " + gp(c.cashCap);
		String risk = c.risk.charAt(0) + c.risk.substring(1).toLowerCase(java.util.Locale.ROOT) + " risk";
		String checks = "Checking " + checkInLabel(c.checkInMinutes).toLowerCase(java.util.Locale.ROOT);
		int reserved = reservedSlots(c);
		String slots = String.format("%d of %d GE slots%s", c.slotsAvailable - reserved, c.slotsAvailable,
			reserved > 0 ? String.format(" (%d kept for you)", reserved) : "");
		return Arrays.asList(cash, risk, checks, slots);
	}

	public static String checkInLabel(int minutes)
	{
		if (minutes < 60)
		{
			return "Every " + minutes + " minutes";
		}
		if (minutes == 60)
		{
			return "Every hour";
		}
		if (minutes == 1440)
		{
			return "Once a day";
		}
		return "Every " + (minutes / 60) + " hours";
	}

	/** Parses what the player typed for a cash cap: 25m, 1.5b, 500k, 2,000,000. -1 if it isn't a number. */
	public static long parseGp(String text)
	{
		if (text == null)
		{
			return -1;
		}
		String t = text.trim().toLowerCase(java.util.Locale.ROOT).replace(",", "").replace(" ", "").replace("gp", "");
		if (t.isEmpty())
		{
			return -1;
		}
		long multiplier = 1;
		char last = t.charAt(t.length() - 1);
		if (last == 'k' || last == 'm' || last == 'b')
		{
			multiplier = last == 'k' ? 1_000L : last == 'm' ? 1_000_000L : 1_000_000_000L;
			t = t.substring(0, t.length() - 1);
		}
		try
		{
			double value = Double.parseDouble(t);
			if (value < 0 || Double.isNaN(value) || Double.isInfinite(value))
			{
				return -1;
			}
			return Math.min(Integer.MAX_VALUE, Math.round(value * multiplier));
		}
		catch (NumberFormatException e)
		{
			return -1;
		}
	}

	public static String gp(long value)
	{
		if (value >= 1_000_000_000L)
		{
			return trim(value / 1_000_000_000.0) + "b";
		}
		if (value >= 1_000_000L)
		{
			return trim(value / 1_000_000.0) + "m";
		}
		if (value >= 10_000L)
		{
			return trim(value / 1_000.0) + "k";
		}
		return String.format("%,d", value) + " gp";
	}

	private static String trim(double v)
	{
		String s = String.format(java.util.Locale.ROOT, "%.2f", v);
		s = s.replaceAll("0+$", "").replaceAll("\\.$", "");
		return s;
	}
}
