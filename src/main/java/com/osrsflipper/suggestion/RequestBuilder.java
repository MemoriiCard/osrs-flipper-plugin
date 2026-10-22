package com.osrsflipper.suggestion;

import com.osrsflipper.api.SuggestionRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Turns what the client can see (offers, inventory, tracked buys, settings) into a suggestion request.
 * Kept free of RuneLite types so it can be unit tested.
 */
public final class RequestBuilder
{
	static final int MAX_BLOCKED = 5000;

	private RequestBuilder()
	{
	}

	/**
	 * @param offers     the eight GE slots; empty slots are skipped
	 * @param inventory  canonical (unnoted) item ID to quantity, including coins
	 * @param holdings   tracked buys not yet sold, by item ID
	 */
	public static SuggestionRequest build(
		Settings settings,
		List<SuggestionRequest.Offer> offers,
		Map<Integer, Long> inventory,
		Map<Integer, Integer> holdings,
		boolean members,
		List<Integer> skipped)
	{
		SuggestionRequest request = new SuggestionRequest();

		SuggestionRequest.Profile profile = request.profile;
		profile.risk = settings.risk;
		profile.timeframeMinutes = settings.timeframeMinutes;
		profile.f2pOnly = settings.f2pOnly;
		profile.blockedItemIds = parseItemIds(settings.blockedItems);
		profile.minProfitPerFlip = Math.max(0, settings.minProfit);

		SuggestionRequest.Account account = request.account;
		account.coins = inventory.getOrDefault(Inventory.COINS, 0L);
		account.offers = new ArrayList<>(offers);
		account.held = sellable(inventory, holdings);
		account.members = members;
		account.reservedSlots = settings.reservedSlots;
		account.sellOnly = settings.sellOnly;
		account.skipItemIds = new ArrayList<>(skipped);
		return request;
	}

	/**
	 * Items the plugin saw the player buy that are now in the inventory. Only those are offered for
	 * sale, so the player's own gear and supplies are never suggested as sells.
	 */
	static Map<Integer, Integer> sellable(Map<Integer, Long> inventory, Map<Integer, Integer> holdings)
	{
		Map<Integer, Integer> held = new HashMap<>();
		for (Map.Entry<Integer, Integer> e : holdings.entrySet())
		{
			long inInventory = inventory.getOrDefault(e.getKey(), 0L);
			int quantity = (int) Math.min(inInventory, e.getValue());
			if (quantity > 0 && e.getKey() != Inventory.COINS)
			{
				held.put(e.getKey(), quantity);
			}
		}
		return held;
	}

	/** Parses "4151, 11802,abc" into [4151, 11802], ignoring anything that isn't an item ID. */
	/** The blocked-items setting with one more item ID on the end. */
	public static String withItemId(String text, int itemId)
	{
		List<Integer> ids = parseItemIds(text);
		if (!ids.contains(itemId) && ids.size() < MAX_BLOCKED)
		{
			ids.add(itemId);
		}
		StringBuilder out = new StringBuilder();
		for (int id : ids)
		{
			out.append(out.length() == 0 ? "" : ",").append(id);
		}
		return out.toString();
	}

	static List<Integer> parseItemIds(String text)
	{
		List<Integer> ids = new ArrayList<>();
		if (text == null)
		{
			return ids;
		}
		for (String part : text.split("[,\\s]+"))
		{
			if (part.isEmpty() || ids.size() >= MAX_BLOCKED)
			{
				continue;
			}
			try
			{
				int id = Integer.parseInt(part);
				if (id > 0 && !ids.contains(id))
				{
					ids.add(id);
				}
			}
			catch (NumberFormatException ignored)
			{
				// Not an ID; skip it.
			}
		}
		return ids;
	}

	/** The settings that shape a request, copied from the plugin config. */
	public static class Settings
	{
		public String risk = "medium";
		public int timeframeMinutes = 30;
		public boolean f2pOnly;
		public String blockedItems = "";
		public int minProfit;
		public int reservedSlots;
		public boolean sellOnly;
	}

	/** Item IDs used when reading the inventory. */
	public static final class Inventory
	{
		public static final int COINS = 995;

		private Inventory()
		{
		}
	}
}
