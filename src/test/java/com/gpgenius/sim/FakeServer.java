package com.gpgenius.sim;

import com.gpgenius.api.TradeUpload;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The server's side of trade uploads, as far as the plugin can tell: it keeps each trade once by its
 * client ID (as POST /v1/trades does), can be down, and can lose its reply after storing a batch.
 */
public class FakeServer
{
	public final Map<String, TradeUpload.Trade> stored = new LinkedHashMap<>();
	public boolean up = true;
	/** Store the next batch but drop the reply, so the plugin thinks it failed. */
	public boolean loseNextReply;
	public int calls;

	/** @return how many new trades were accepted, or -1 when no answer came back */
	public int upload(TradeUpload upload)
	{
		calls++;
		if (!up)
		{
			return -1;
		}
		int accepted = 0;
		for (TradeUpload.Trade t : upload.trades)
		{
			if (stored.putIfAbsent(t.clientId, t) == null)
			{
				accepted++;
			}
		}
		if (loseNextReply)
		{
			loseNextReply = false;
			return -1;
		}
		return accepted;
	}
}
