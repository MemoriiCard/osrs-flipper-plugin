package com.gpgenius.market;

import com.gpgenius.api.FlipperApiClient;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Getter;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.ItemManager;

/**
 * Keeps the watchlist's prices fresh (once a minute, from the same latest prices the server's
 * engine uses) and reports targets as they are reached. Free accounts watch 3 items, premium up
 * to 100. All methods run on the client thread.
 */
@Singleton
public class WatchlistController
{
	static final long POLL_MILLIS = 60_000;

	/** One watched item as the panel shows it. */
	public static final class Row
	{
		public final String name;
		/** -1 when no tradeable item matches what was typed. */
		public final int itemId;
		public final long buyTarget;
		public final long sellTarget;
		public final Long low;
		public final Long high;
		public final boolean buyReached;
		public final boolean sellReached;

		Row(String name, int itemId, Watchlist.Entry entry, MarketPrice price)
		{
			this.name = name;
			this.itemId = itemId;
			this.buyTarget = entry.buyTarget;
			this.sellTarget = entry.sellTarget;
			this.low = price == null ? null : price.low;
			this.high = price == null ? null : price.high;
			this.buyReached = Watchlist.buyReached(entry, price);
			this.sellReached = Watchlist.sellReached(entry, price);
		}
	}

	private final FlipperApiClient api;
	private final ClientThread clientThread;
	private final ItemManager itemManager;
	private final Watchlist watchlist = new Watchlist();
	/** Lower-case typed name -> item ID (or -1), so names are looked up once. */
	private final Map<String, Integer> resolved = new HashMap<>();
	private final Map<Integer, String> names = new HashMap<>();
	private final Map<Integer, MarketPrice> prices = new HashMap<>();

	@Getter
	private List<Row> rows = Collections.emptyList();
	/** Lines past the free limit that need premium to be watched. */
	@Getter
	private int overLimit;
	private String lastText;
	private boolean lastPremium;
	private long lastPollMillis;
	private boolean inFlight;

	@Inject
	public WatchlistController(FlipperApiClient api, ClientThread clientThread, ItemManager itemManager)
	{
		this.api = api;
		this.clientThread = clientThread;
		this.itemManager = itemManager;
	}

	/**
	 * @param onHit  called once per target reached, with the notification text
	 * @param onList called when the rows change
	 */
	public void tick(String text, boolean premium, long now, Consumer<String> onHit, Runnable onList)
	{
		String setting = text == null ? "" : text;
		boolean changed = !setting.equals(lastText) || premium != lastPremium;
		lastText = setting;
		lastPremium = premium;
		List<Watchlist.Entry> all = Watchlist.parse(setting, Watchlist.PREMIUM_ITEMS);
		int max = premium ? Watchlist.PREMIUM_ITEMS : Watchlist.FREE_ITEMS;
		List<Watchlist.Entry> entries = all.size() > max ? all.subList(0, max) : all;
		overLimit = all.size() - entries.size();
		if (entries.isEmpty())
		{
			if (!rows.isEmpty())
			{
				rows = Collections.emptyList();
				onList.run();
			}
			return;
		}
		if (changed)
		{
			rebuild(entries, Collections.emptyList(), onHit);
			onList.run();
		}
		if (inFlight || (!changed && now - lastPollMillis < POLL_MILLIS))
		{
			return;
		}
		Set<Integer> ids = new LinkedHashSet<>();
		for (Watchlist.Entry entry : entries)
		{
			int id = resolve(entry);
			if (id > 0)
			{
				ids.add(id);
			}
		}
		if (ids.isEmpty())
		{
			return;
		}
		inFlight = true;
		lastPollMillis = now;
		api.latestPrices(ids, list -> clientThread.invokeLater(() ->
		{
			inFlight = false;
			for (MarketPrice p : list)
			{
				prices.put(p.itemId, p);
			}
			rebuild(entries, list, onHit);
			onList.run();
		}), message -> clientThread.invokeLater(() -> inFlight = false));
	}

	private void rebuild(List<Watchlist.Entry> entries, List<MarketPrice> fresh, Consumer<String> onHit)
	{
		Set<Integer> freshIds = new LinkedHashSet<>();
		for (MarketPrice p : fresh)
		{
			freshIds.add(p.itemId);
		}
		List<Row> out = new ArrayList<>();
		for (Watchlist.Entry entry : entries)
		{
			int id = resolve(entry);
			MarketPrice price = id > 0 ? prices.get(id) : null;
			String name = id > 0 ? names.getOrDefault(id, entry.item) : entry.item;
			out.add(new Row(name, id, entry, price));
			if (id > 0 && freshIds.contains(id))
			{
				for (Watchlist.Hit hit : watchlist.check(id, entry, price))
				{
					onHit.accept(String.format("Watchlist: %s %s %,d gp (your %s target is %,d).", name,
						hit.buy ? "is selling at" : "is buying at", hit.price, hit.buy ? "buy" : "sell", hit.target));
				}
			}
		}
		rows = out;
	}

	private int resolve(Watchlist.Entry entry)
	{
		String key = entry.item.toLowerCase(Locale.ROOT);
		Integer known = resolved.get(key);
		if (known != null)
		{
			return known;
		}
		int id = entry.itemId();
		String name = null;
		if (id > 0)
		{
			try
			{
				net.runelite.api.ItemComposition item = itemManager.getItemComposition(id);
				name = item == null ? null : item.getName();
			}
			catch (RuntimeException e)
			{
				name = null;
			}
			if (name == null || "null".equalsIgnoreCase(name))
			{
				id = -1;
			}
		}
		else
		{
			// Exact name first, else the first tradeable item whose name starts with what was typed.
			for (var match : itemManager.search(entry.item))
			{
				if (match.getName().equalsIgnoreCase(entry.item))
				{
					id = match.getId();
					name = match.getName();
					break;
				}
				if (id <= 0 && match.getName().toLowerCase(Locale.ROOT).startsWith(key))
				{
					id = match.getId();
					name = match.getName();
				}
			}
		}
		resolved.put(key, id > 0 ? id : -1);
		if (id > 0 && name != null)
		{
			names.put(id, name);
		}
		return id > 0 ? id : -1;
	}

	public void reset()
	{
		rows = Collections.emptyList();
		prices.clear();
		watchlist.reset();
		lastText = null;
		lastPollMillis = 0;
	}
}
