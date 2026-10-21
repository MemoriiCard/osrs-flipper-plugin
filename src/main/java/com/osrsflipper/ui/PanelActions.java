package com.osrsflipper.ui;

/** What the side panel's buttons do. The plugin implements these; the panel only draws. */
public interface PanelActions
{
	void signIn();

	void reopenSignIn();

	void cancelSignIn();

	void signOut();

	void openWebsite();

	void skip();

	/** 1 for good, -1 for bad. */
	void rate(int rating);

	void explain();

	/** Make a dump alert the current suggestion. */
	void useAlert(com.osrsflipper.api.DumpAlert alert);

	void dismissAlert(String alertId);
}
