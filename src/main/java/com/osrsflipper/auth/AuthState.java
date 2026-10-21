package com.osrsflipper.auth;

/** Where sign-in stands, for the side panel. */
public enum AuthState
{
	SIGNED_OUT,
	/** Waiting for the player to approve the code on the website. */
	LINKING,
	SIGNED_IN
}
