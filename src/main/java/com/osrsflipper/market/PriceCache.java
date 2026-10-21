package com.osrsflipper.market;

import com.osrsflipper.api.FlipperApiClient;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Latest market prices for the few items the player is trading right now, for slot colours and the
 * confirm guard. Refreshed once a minute, or sooner when a new item shows up.
 */
@Singleton
public class PriceCache
{
	static final long REFRESH_MILLIS = 60_000;
	private static final int MAX_ITEMS = 20;

	private final FlipperApiClient api;
	private final Map<Integer, MarketPrice> prices = new ConcurrentHashMap<>();
	private Set<Integer> lastIds = new TreeSet<>();
	private long lastFetchMillis;
	private volatile boolean inFlight;

	@Inject
	public PriceCache(FlipperApiClient api)
	{
		this.api = api;
	}

	public MarketPrice get(int itemId)
	{
		return prices.get(itemId);
	}

	/** Client thread. Fetches prices for these items if due. */
	public void want(Set<Integer> itemIds)
	{
		Set<Integer> ids = new TreeSet<>();
		for (Integer id : itemIds)
		{
			if (id != null && id > 0 && ids.size() < MAX_ITEMS)
			{
				ids.add(id);
			}
		}
		if (ids.isEmpty() || inFlight)
		{
			return;
		}
		long now = System.currentTimeMillis();
		boolean newItem = !lastIds.containsAll(ids);
		if (!newItem && now - lastFetchMillis < REFRESH_MILLIS)
		{
			return;
		}
		inFlight = true;
		lastFetchMillis = now;
		lastIds = ids;
		api.latestPrices(ids, list ->
		{
			for (MarketPrice p : list)
			{
				prices.put(p.itemId, p);
			}
			inFlight = false;
		}, message -> inFlight = false);
	}

	public void clear()
	{
		prices.clear();
		lastIds = new TreeSet<>();
		lastFetchMillis = 0;
	}
}
