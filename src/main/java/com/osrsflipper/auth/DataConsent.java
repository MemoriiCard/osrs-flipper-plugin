package com.osrsflipper.auth;

/**
 * The data notice the player agrees to before the plugin sends anything to the server. Agreeing is
 * remembered per notice version, so a change to what's shared asks again.
 */
public final class DataConsent
{
	public static final int VERSION = 1;
	public static final String CONFIG_KEY = "dataConsent";

	/** Shown in the side panel above "Agree & sign in". Keep in step with the website's privacy page. */
	public static final String NOTICE = "To give suggestions and show your history on the website, this plugin "
		+ "sends the server your Grand Exchange offers and trades, the coins in your inventory, your "
		+ "character name, and how you respond to suggestions. It never sends your Jagex login, bank PIN, "
		+ "chat, or anything about other players. Turn off 'Sync flips to website' in settings to stop "
		+ "uploading trades, and delete your data any time on the website. Until you agree, flips are "
		+ "tracked on this computer only.";

	private DataConsent()
	{
	}

	public static String agreedValue()
	{
		return "agreed:" + VERSION;
	}

	public static boolean agreed(String stored)
	{
		return agreedValue().equals(stored);
	}
}
