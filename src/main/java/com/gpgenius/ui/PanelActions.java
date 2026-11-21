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

	void explain();

	/** Make a dump alert the current suggestion. */
	void useAlert(com.gpgenius.api.DumpAlert alert);

	void dismissAlert(String alertId);

	/** day, week or month */
	void chartPeriod(String period);

	/** Prefer the current item when it's a good flip. */
	void favourite();

	/** Copy past trades from the Flipping Utilities plugin's files on this computer. */
	void importHistory();

	/** Save the settings from the first-run wizard or a preset. */
	void applySetup(com.gpgenius.setup.Preset.Settings settings);

	/** Close the first-run wizard without changing settings. */
	void skipSetup();

	/** Hide the welcome-back plan and go back to one step at a time. */
	void dismissPlan();
}
