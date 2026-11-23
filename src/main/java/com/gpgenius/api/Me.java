package com.gpgenius.api;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;

/** The signed-in player's plan, beta features and announcements, from /v1/plugin/hello. */
public class Me
{
	@SerializedName("display_name")
	public String displayName;
	public String email;
	public boolean premium;
	public int seats;
	@SerializedName("premium_until")
	public String premiumUntil;
	public List<String> features = new ArrayList<>();
	public List<Announcement> announcements = new ArrayList<>();
	@SerializedName("current_account")
	public GameAccount currentAccount;
	/** Why the free trial hasn't started yet: needs_discord or discord_too_new. Null otherwise. */
	@SerializedName("trial_waiting")
	public String trialWaiting;
	@SerializedName("trial_discord_min_age_days")
	public int trialDiscordMinAgeDays;
	/** The plan for the current game account: free, premium or pro. Older servers don't send it. */
	public String tier;
	@SerializedName("tier_name")
	public String tierName;
	@SerializedName("tier_summary")
	public String tierSummary;
	@SerializedName("refresh_seconds")
	public int refreshSeconds;
	public boolean explanations;

	public static class GameAccount
	{
		@SerializedName("display_name")
		public String displayName;
		public boolean premium;
	}

	public static class Announcement
	{
		public String id;
		public String message;
		/** info, warning or critical. */
		public String level;
		public String title;
		/** An absolute link to more detail (the status page, a release note), or null. */
		@SerializedName("link_url")
		public String linkUrl;
		/** Older servers don't send it; their announcements could always be closed locally. */
		public Boolean dismissible;
		@SerializedName("ends_at")
		public String endsAt;

		public boolean canDismiss()
		{
			return id != null && !Boolean.FALSE.equals(dismissible);
		}

		public boolean isCritical()
		{
			return "critical".equals(level);
		}

		public boolean isWarning()
		{
			return "warning".equals(level);
		}
	}

	public boolean hasFeature(String key)
	{
		return features != null && features.contains(key);
	}

	/** What the free plan line says: what free covers, plus what the waiting trial needs, if anything. */
	public String freePlanText()
	{
		String text = "Suggestions from popular free-to-play items. Premium unlocks every item, members items on members worlds, and dump alerts.";
		if (tier != null)
		{
			text = "Suggestions from popular free-to-play items, refreshed " + Tier.every(refreshSeconds)
				+ ". Premium unlocks every item on fresher prices, members items on members worlds, and dump alerts.";
		}
		if ("needs_discord".equals(trialWaiting))
		{
			return text + " Link your Discord account on the website to start your free premium trial.";
		}
		if ("discord_too_new".equals(trialWaiting))
		{
			return text + " Your free trial needs a Discord account at least " + Math.max(1, trialDiscordMinAgeDays)
				+ " days old. It starts once yours is.";
		}
		return text;
	}

	/** Whether the current game account is on Pro (the top plan). */
	public boolean pro()
	{
		return Tier.PRO.equals(tier);
	}

	/** The paid plan's line: seats, the end date if any, and how often suggestions refresh. */
	public String paidPlanText()
	{
		String text = seats + " game account" + (seats == 1 ? "" : "s")
			+ (premiumUntil != null && premiumUntil.length() >= 10 ? ", until " + premiumUntil.substring(0, 10) : "") + ".";
		if (tier == null)
		{
			return text;
		}
		return text + " " + Tier.line(tier, tierSummary, refreshSeconds);
	}

	/** Whether the game account the player is logged in to gets premium suggestions. */
	public boolean accountPremium()
	{
		return currentAccount != null ? currentAccount.premium : premium;
	}
}
