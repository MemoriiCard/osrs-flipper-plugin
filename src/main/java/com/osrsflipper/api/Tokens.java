package com.osrsflipper.api;

import com.google.gson.annotations.SerializedName;

/** Server reply with a fresh access token and the next refresh token. */
public class Tokens
{
	@SerializedName("access_token")
	public String accessToken;
	@SerializedName("refresh_token")
	public String refreshToken;
	@SerializedName("expires_in")
	public int expiresIn;
}
