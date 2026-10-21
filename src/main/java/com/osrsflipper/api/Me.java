package com.osrsflipper.api;

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

	public static class GameAccount
	{
		@SerializedName("display_name")
		public String displayName;
		public boolean premium;
	}

	public static class Announcement
	{
		public String message;
		public String level;
	}

	public boolean hasFeature(String key)
	{
		return features != null && features.contains(key);
	}

	/** Whether the game account the player is logged in to gets premium suggestions. */
	public boolean accountPremium()
	{
		return currentAccount != null ? currentAccount.premium : premium;
	}
}
