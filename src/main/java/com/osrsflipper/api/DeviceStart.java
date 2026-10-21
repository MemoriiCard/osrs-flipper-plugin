package com.osrsflipper.api;

import com.google.gson.annotations.SerializedName;

/** Start of the sign-in flow: a code for the player to approve on the website. */
public class DeviceStart
{
	@SerializedName("device_code")
	public String deviceCode;
	@SerializedName("user_code")
	public String userCode;
	@SerializedName("verification_uri")
	public String verificationUri;
	@SerializedName("verification_uri_complete")
	public String verificationUriComplete;
	@SerializedName("expires_in")
	public int expiresIn;
	public int interval;
}
