package com.gpgenius.auth;

/**
 * The data notice the player agrees to before the plugin sends anything to the server. Agreeing is
 * remembered per notice version, so a change to what's shared asks again.
 */
public final class DataConsent
{
	public static final int VERSION = 2;
	public static final String CONFIG_KEY = "dataConsent";

	/** Shown in the side panel above "Agree & sign in". Keep in step with the website's privacy page. */
	public static final String NOTICE = "To give suggestions and show your history on the website, this plugin "
		+ "sends the server your Grand Exchange offers and trades, the coins in your inventory and any items "
		+ "it saw you buy there, your character name, how you respond to suggestions, which panel features "
		+ "you use, and short error reports with personal details removed. It never sends your Jagex login, "
		+ "bank PIN, chat, or anything about other players. Turn off 'Sync flips to website' or 'Send error "
		+ "reports' in settings to stop those, and delete your data any time on the website. Until you agree, "
		+ "flips are tracked on this computer only.";

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
