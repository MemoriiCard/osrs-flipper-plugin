package com.gpgenius.setup;

import com.gpgenius.GpGeniusConfig.CheckIn;
import com.gpgenius.GpGeniusConfig.RiskLevel;
import com.gpgenius.api.Profiles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * A fast-start preset as the player sees it: a name, one line about it, and the risk and check-in
 * time it starts with. What a preset does to suggestions is decided on the server; the plugin only
 * sends the preset's id. The built-in list is shown until the server's list arrives.
 */
public final class Preset
{
	public static final List<Preset> BUILT_IN = Collections.unmodifiableList(Arrays.asList(
		new Preset("active", "Active", "Quick flips that fill in minutes, for when you're at the GE and checking often.", RiskLevel.MEDIUM, CheckIn.MINUTES_15),
		new Preset("afk", "AFK", "Steady offers you can leave for a couple of hours while you skill or boss.", RiskLevel.MEDIUM, CheckIn.HOURS_2),
		new Preset("overnight", "Overnight", "Patient, low-risk offers with wide margins, for checking once or twice a day.", RiskLevel.LOW, CheckIn.HOURS_8),
		new Preset("low_budget", "Low budget", "Cheap, fast-moving items so a small stack of coins keeps working.", RiskLevel.LOW, CheckIn.MINUTES_30),
		new Preset("high_volume", "High volume", "Busy items in big quantities, spread across many slots.", RiskLevel.MEDIUM, CheckIn.MINUTES_30)));

	public final String id;
	public final String label;
	public final String blurb;
	public final RiskLevel risk;
	public final CheckIn checkIn;

	public Preset(String id, String label, String blurb, RiskLevel risk, CheckIn checkIn)
	{
		this.id = id;
		this.label = label;
		this.blurb = blurb;
		this.risk = risk;
		this.checkIn = checkIn;
	}

	@Override
	public String toString()
	{
		return label;
	}

	/** The server's presets, or the built-in list when the server sent none it could use. */
	public static List<Preset> fromServer(List<Profiles.PresetInfo> infos)
	{
		List<Preset> out = new ArrayList<>();
		if (infos != null)
		{
			for (Profiles.PresetInfo info : infos)
			{
				if (info != null && info.id != null && info.label != null)
				{
					out.add(new Preset(info.id, info.label, info.blurb == null ? "" : info.blurb,
						risk(info.risk), nearestCheckIn(info.checkInMinutes)));
				}
			}
		}
		return out.isEmpty() ? BUILT_IN : Collections.unmodifiableList(out);
	}

	/** The preset with this id, or null. */
	public static Preset byId(List<Preset> presets, String id)
	{
		for (Preset p : presets)
		{
			if (p.id.equals(id))
			{
				return p;
			}
		}
		return null;
	}

	public static RiskLevel risk(String name)
	{
		if (name != null)
		{
			for (RiskLevel r : RiskLevel.values())
			{
				if (r.name().equalsIgnoreCase(name.trim()))
				{
					return r;
				}
			}
		}
		return RiskLevel.MEDIUM;
	}

	/** The check-in setting closest to a number of minutes. */
	public static CheckIn nearestCheckIn(int minutes)
	{
		CheckIn best = CheckIn.MINUTES_30;
		int bestGap = Integer.MAX_VALUE;
		for (CheckIn c : CheckIn.values())
		{
			int gap = Math.abs(c.minutes - minutes);
			if (gap < bestGap)
			{
				best = c;
				bestGap = gap;
			}
		}
		return best;
	}

	/** Lower-case id for usage counters. */
	public String key()
	{
		return id.toLowerCase(Locale.ROOT);
	}
}
