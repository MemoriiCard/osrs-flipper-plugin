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

	void skip();

	/** Never suggest the current item again. */
	void block();

	/** 1 for good, -1 for bad. */
	void rate(int rating);

	/** Make a dump alert the current suggestion. */
	void useAlert(com.gpgenius.api.DumpAlert alert);

	void dismissAlert(String alertId);

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
}
