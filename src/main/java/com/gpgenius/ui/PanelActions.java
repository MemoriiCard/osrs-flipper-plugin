package com.gpgenius.ui;

/** What the side panel's buttons do. The plugin implements these; the panel only draws. */
public interface PanelActions
{
	/** Agrees to the data notice, then starts signing in if signed out. */
	void signIn();

	/** Agrees to the data notice while already signed in (after the notice changed). */
	void agree();

	void reopenSignIn();

	void cancelSignIn();

	void signOut();

	void openWebsite();

	/** Opens the help page (FAQ and the Discord link) in the browser. */
	default void openHelp()
	{
	}

	/** Sends "Report a problem"; the answer goes to FlipperPanel.showProblemReply. */
	default void reportProblem(String what, boolean includeErrors)
	{
	}

	void skip();

	/** Pro: ask why the item on show was suggested; the answer goes to FlipperPanel.showExplanation. */
	default void explain()
	{
	}

	/** Never suggest the current item again. */
	void block();

	/** 1 for good, -1 for bad. */
	void rate(int rating);

	/** Make a dump alert the current suggestion. */
	void useAlert(com.gpgenius.api.DumpAlert alert);

	void dismissAlert(String alertId);

	/** Opens last week's recap on the dashboard. */
	default void openRecap()
	{
	}

	/** Hides the recap card until next week's. */
	default void hideRecap()
	{
	}

	/** Closes an announcement (here and, for signed-in players, everywhere else). */
	void dismissAnnouncement(String announcementId);

	/** Opens an announcement's "Read more" link, if it's on the GP Genius server. */
	void openAnnouncementLink(String url);

	/** day, week or month */
	void chartPeriod(String period);

	/** Prefer the current item when it's a good flip. */
	void favourite();

	/** Copy past trades from the Flipping Utilities plugin's files on this computer. */
	void importHistory();

	/** Start with a fast-start preset, by its id. */
	void choosePreset(String presetId);

	void switchProfile(String profileId);

	/** Save the current settings as a new profile and switch to it. */
	void newProfile(String name);

	void renameProfile(String profileId, String name);

	void deleteProfile(String profileId);

	/** Close the preset picker without changing settings. */
	void skipSetup();

	/** Hide the welcome-back plan and go back to one step at a time. */
	void dismissPlan();

	/** The player is about to log off for about this many hours: switch to overnight picks. */
	void startOvernight(int hours);

	void stopOvernight();

	/** Open the pop-out trade dashboard. */
	void openTradeDashboard();

	/** Save the player's trades as a CSV file. */
	void exportTrades();

	/** Start a new profit session: "current" for this character, "all" for every one, or a character id. */
	void resetSession(String scope);

	/**
	 * Set the goal, replacing any earlier one.
	 *
	 * @param kind profit or stack
	 * @param deadline ISO-8601 with offset
	 * @param allCharacters false for just the character logged in now
	 * @param adapt let the goal pick the preset and check-in rhythm while it runs
	 */
	void setGoal(String kind, long target, String deadline, boolean allCharacters, boolean adapt);

	void clearGoal();

	/** The first-run guide opened: send what the plugin can see (coins, bank, world, slots in use). */
	default void startOnboarding()
	{
	}

	/** The player finished the first-run guide: apply the choices and fetch their first suggestion. */
	default void finishOnboarding(com.gpgenius.setup.Onboarding.Choices choices)
	{
	}

	/** Save which GP/hour figures to show and their windows (shared with the website). */
	default void saveEfficiencyPrefs(java.util.Map<String, java.util.Map<String, Object>> prefs)
	{
	}

	/** Show or hide idle slot time and unused cash on the GP/hour card. */
	default void showIdle(boolean show)
	{
	}

	/** Open a link in the browser, such as an item's price chart on the OSRS Wiki. */
	default void openUrl(String url)
	{
	}
}
