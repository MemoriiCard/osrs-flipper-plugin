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
}
