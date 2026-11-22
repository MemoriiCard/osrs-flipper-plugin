package com.gpgenius.api;

import com.google.gson.annotations.SerializedName;

/** Body of POST /v1/goal. */
public class GoalRequest
{
	/** profit or stack */
	public String kind = "profit";
	public long target;
	/** ISO-8601 with offset. */
	public String deadline;
	/** all, or character for the one named by account_hash. */
	public String scope = "all";
	@SerializedName("account_hash")
	public String accountHash;
	public boolean adapt = true;
}
