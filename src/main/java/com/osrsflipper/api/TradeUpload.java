package com.osrsflipper.api;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;

/** Body of POST /v1/trades. The server ignores trades it already has, by client_id. */
public class TradeUpload
{
	@SerializedName("account_hash")
	public String accountHash;
	@SerializedName("display_name")
	public String displayName;
	public List<Trade> trades = new ArrayList<>();

	public static class Trade
	{
		@SerializedName("client_id")
		public String clientId;
		@SerializedName("item_id")
		public int itemId;
		@SerializedName("is_buy")
		public boolean isBuy;
		public int quantity;
		public long total;
		@SerializedName("timestamp_ms")
		public long timestampMs;
	}
}
