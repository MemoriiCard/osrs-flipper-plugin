package com.gpgenius.market;

import com.gpgenius.api.Conversions;
import com.gpgenius.api.FlipperApiClient;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Getter;
import net.runelite.client.callback.ClientThread;

/**
 * Polls for set and decant flips every few minutes while the player has them switched on (premium).
 * The server only returns ones it is confident will sell through. All methods run on the client thread.
 */
@Singleton
public class ConversionController
{
	static final long POLL_MILLIS = 5 * 60_000;

	private final FlipperApiClient api;
	private final ClientThread clientThread;

	@Getter
	private List<Conversions.Opportunity> opportunities = Collections.emptyList();
	@Getter
	private boolean premiumRequired;
	private final Set<String> seen = new HashSet<>();
	private long lastPollMillis;
	private long lastMinProfit = -1;
	private boolean inFlight;

	@Inject
	public ConversionController(FlipperApiClient api, ClientThread clientThread)
	{
		this.api = api;
		this.clientThread = clientThread;
	}

	public void poll(long accountHash, long coins, long minProfit, int timeframeMinutes, boolean members, long now,
		Consumer<Conversions.Opportunity> onNew, Runnable onList)
	{
		boolean changed = minProfit != lastMinProfit;
		if (inFlight || (!changed && now - lastPollMillis < POLL_MILLIS))
		{
			return;
		}
		inFlight = true;
		lastPollMillis = now;
		lastMinProfit = minProfit;
		api.conversions(Long.toString(accountHash), coins, minProfit, timeframeMinutes, members,
			result -> clientThread.invokeLater(() ->
			{
				inFlight = false;
				premiumRequired = result != null && result.premiumRequired;
				List<Conversions.Opportunity> list = result == null || result.opportunities == null
					? Collections.emptyList() : new ArrayList<>(result.opportunities);
				Set<String> current = new HashSet<>();
				for (Conversions.Opportunity o : list)
				{
					current.add(o.title);
					if (o.title != null && !seen.contains(o.title))
					{
						onNew.accept(o);
					}
				}
				// Forget ones that went away, so they notify again if they come back.
				seen.clear();
				seen.addAll(current);
				opportunities = list;
				onList.run();
			}),
			message -> clientThread.invokeLater(() -> inFlight = false));
	}

	public void reset()
	{
		opportunities = Collections.emptyList();
		premiumRequired = false;
		seen.clear();
		lastPollMillis = 0;
		lastMinProfit = -1;
	}
}
