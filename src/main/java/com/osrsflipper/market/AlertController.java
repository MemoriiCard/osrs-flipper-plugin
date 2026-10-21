package com.osrsflipper.market;

import com.osrsflipper.api.DumpAlert;
import com.osrsflipper.api.FlipperApiClient;
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
 * Polls for dump alerts (items just sold far below their usual price) once a minute for premium
 * accounts, and reports each new one once. All methods run on the client thread.
 */
@Singleton
public class AlertController
{
	static final long POLL_MILLIS = 60_000;
	private static final int MAX_SEEN = 500;

	private final FlipperApiClient api;
	private final ClientThread clientThread;

	@Getter
	private List<DumpAlert> alerts = Collections.emptyList();
	@Getter
	private boolean premiumRequired;
	private final Set<String> seen = new HashSet<>();
	private long lastPollMillis;
	private boolean inFlight;

	@Inject
	public AlertController(FlipperApiClient api, ClientThread clientThread)
	{
		this.api = api;
		this.clientThread = clientThread;
	}

	/**
	 * @param onNew  called once for each alert not seen before
	 * @param onList called when the list changes
	 */
	public void poll(long accountHash, long coins, long minProfit, boolean f2pOnly, boolean members,
		Consumer<DumpAlert> onNew, Runnable onList)
	{
		long now = System.currentTimeMillis();
		if (inFlight || now - lastPollMillis < POLL_MILLIS)
		{
			return;
		}
		inFlight = true;
		lastPollMillis = now;
		api.alerts(Long.toString(accountHash), coins, minProfit, f2pOnly, members,
			result -> clientThread.invokeLater(() ->
			{
				inFlight = false;
				premiumRequired = result != null && result.premiumRequired;
				List<DumpAlert> list = result == null || result.alerts == null ? Collections.emptyList() : new ArrayList<>(result.alerts);
				for (DumpAlert alert : list)
				{
					if (alert.id != null && seen.add(alert.id))
					{
						onNew.accept(alert);
					}
				}
				if (seen.size() > MAX_SEEN)
				{
					seen.clear();
					for (DumpAlert alert : list)
					{
						seen.add(alert.id);
					}
				}
				alerts = list;
				onList.run();
			}),
			message -> clientThread.invokeLater(() -> inFlight = false));
	}

	public void dismiss(String alertId)
	{
		List<DumpAlert> list = new ArrayList<>(alerts);
		list.removeIf(a -> a.id != null && a.id.equals(alertId));
		alerts = list;
	}

	public void reset()
	{
		alerts = Collections.emptyList();
		premiumRequired = false;
		lastPollMillis = 0;
	}
}
