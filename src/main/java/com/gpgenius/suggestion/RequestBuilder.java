package com.gpgenius.suggestion;

import com.gpgenius.api.SuggestionRequest;
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
		boolean fromPreset = settings.preset != null || settings.profileId != null;
		request.preset = settings.preset;
		request.profileId = settings.profileId;
		// With a preset, "no minimum" means "the preset's minimum", so leave it out.
		profile.minProfitPerFlip = settings.minProfit > 0 || !fromPreset ? (long) Math.max(0, settings.minProfit) : null;
		profile.membersOnly = settings.membersOnly && !settings.f2pOnly;
		profile.coinCap = settings.coinCap > 0 ? (long) settings.coinCap : null;
		profile.maxItemPercent = settings.maxItemPercent > 0 ? Math.min(100, settings.maxItemPercent) : null;
		profile.minItemPrice = settings.minItemPrice > 0 ? (long) settings.minItemPrice : null;
		profile.maxItemPrice = settings.maxItemPrice > 0 ? (long) settings.maxItemPrice : null;
		profile.favouriteItemIds = parseItemIds(settings.favouriteItems);
		profile.categories = new ArrayList<>(settings.categories);

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

	/** An item-ID list setting (blocked or favourite items) with one more item ID on the end. */
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

	/** Parses "4151, 11802,abc" into [4151, 11802], ignoring anything that isn't an item ID. */
	public static List<Integer> parseItemIds(String text)
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
		public boolean membersOnly;
		/** 0 means no cap, here and for the prices below. */
		public int coinCap;
		public int maxItemPercent;
		public int minItemPrice;
		public int maxItemPrice;
		public String favouriteItems = "";
		/** Server category names, such as "runes". Empty means every category. */
		public List<String> categories = new ArrayList<>();
		/** The fast-start preset or saved profile in use; null for neither. */
		public String preset;
		public String profileId;
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
