package com.osrsflipper.suggestion;

import com.osrsflipper.OsrsFlipperConfig;
import com.osrsflipper.api.FlipperApiClient;
import com.osrsflipper.api.Suggestion;
import com.osrsflipper.api.SuggestionRequest;
import com.osrsflipper.auth.AuthSession;
import com.osrsflipper.auth.AuthState;
import com.osrsflipper.tracking.OfferSnapshot;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Getter;
import lombok.Setter;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.GrandExchangeOffer;
import net.runelite.api.GrandExchangeOfferState;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.Player;
import net.runelite.api.WorldType;
import net.runelite.api.gameval.InventoryID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.ItemManager;

/**
 * Keeps one current suggestion for the logged-in account. Asks the server again when the player's
 * offers, inventory or settings change, and otherwise once a minute so prices stay fresh.
 *
 * <p>All methods run on the client thread.</p>
 */
@Singleton
public class SuggestionController
{
	static final long REFRESH_MILLIS = 60_000;
	static final long MIN_GAP_MILLIS = 2_000;
	/** How long a dump alert the player chose stays as the suggestion before normal ones resume. */
	static final long ALERT_PIN_MILLIS = 3 * 60_000;

	private final Client client;
	private final ClientThread clientThread;
	private final ItemManager itemManager;
	private final OsrsFlipperConfig config;
	private final FlipperApiClient api;
	private final AuthSession auth;
	private final SkipList skips = new SkipList();

	@Getter
	private Suggestion current;
	@Getter
	private String error;
	@Setter
	private Runnable onChange = () -> { };

	private boolean dirty = true;
	private boolean inFlight;
	private long lastRequestMillis;
	/** Bumped on logout or reset so replies to older requests are dropped. */
	private int generation;
	private long pinnedUntil;

	@Inject
	public SuggestionController(Client client, ClientThread clientThread, ItemManager itemManager,
		OsrsFlipperConfig config, FlipperApiClient api, AuthSession auth)
	{
		this.auth = auth;
		this.client = client;
		this.clientThread = clientThread;
		this.itemManager = itemManager;
		this.config = config;
		this.api = api;
	}

	/** Something the server bases suggestions on has changed. */
	public void invalidate()
	{
		dirty = true;
	}

	public void reset()
	{
		generation++;
		pinnedUntil = 0;
		current = null;
		error = null;
		inFlight = false;
		dirty = true;
		skips.clear();
		onChange.run();
	}

	public void skip()
	{
		if (current != null && current.itemId != null)
		{
			api.feedback(current.suggestionId, "skipped", null);
			skips.skip(current.itemId, System.currentTimeMillis());
			current = null;
			pinnedUntil = 0;
			dirty = true;
			onChange.run();
		}
	}

	/**
	 * Call every game tick.
	 *
	 * @param holdings tracked buys not yet sold, by item ID
	 * @param slots    the last tracked state of each GE slot, for how long offers have sat
	 */
	public void tick(Map<Integer, Integer> holdings, Map<Integer, OfferSnapshot> slots)
	{
		if (!config.suggestionsEnabled())
		{
			if (current != null || error != null)
			{
				current = null;
				error = null;
				onChange.run();
			}
			return;
		}
		if (client.getGameState() != GameState.LOGGED_IN || inFlight)
		{
			return;
		}
		if (auth.getState() != AuthState.SIGNED_IN)
		{
			if (current != null || error == null)
			{
				current = null;
				error = "Sign in from the side panel to get suggestions.";
				onChange.run();
			}
			return;
		}
		long now = System.currentTimeMillis();
		if (now < pinnedUntil && current != null && !hasOffer(current.itemId))
		{
			return;
		}
		pinnedUntil = 0;
		boolean due = dirty ? now - lastRequestMillis >= MIN_GAP_MILLIS : now - lastRequestMillis >= REFRESH_MILLIS;
		if (!due)
		{
			return;
		}

		SuggestionRequest request = RequestBuilder.build(settings(), offers(slots, now), inventory(), holdings,
			client.getWorldType().contains(WorldType.MEMBERS), skips.active(now));
		request.accountHash = Long.toString(client.getAccountHash());
		Player player = client.getLocalPlayer();
		request.displayName = player == null || player.getName() == null ? "" : player.getName();
		request.profile.allowSharedItems = config.allowSharedItems();
		dirty = false;
		inFlight = true;
		lastRequestMillis = now;
		final int requestGeneration = generation;
		api.suggest(request,
			suggestion -> clientThread.invokeLater(() -> finish(requestGeneration, suggestion, null)),
			message -> clientThread.invokeLater(() -> finish(requestGeneration, null, message)));
	}

	private void finish(int requestGeneration, Suggestion suggestion, String message)
	{
		if (requestGeneration != generation)
		{
			return;
		}
		inFlight = false;
		if (suggestion != null)
		{
			current = suggestion;
			error = null;
		}
		else
		{
			// Keep showing the last suggestion; the error explains why it may be stale.
			error = message;
		}
		onChange.run();
	}

	/** Makes a dump alert the current suggestion, so highlights and hotkeys walk the player through it. */
	public void useAlert(com.osrsflipper.api.DumpAlert alert)
	{
		Suggestion s = new Suggestion();
		s.action = Suggestion.BUY;
		s.itemId = alert.itemId;
		s.name = alert.name;
		s.price = alert.buyPrice;
		s.quantity = alert.quantity;
		s.expectedProfit = alert.expectedProfit;
		s.premium = true;
		s.reason = String.format("Dump alert: sold %.0f%% below its usual price. Buy at %,d and sell back near %,d.",
			alert.dropPercent, alert.buyPrice, alert.sellPrice);
		generation++;
		inFlight = false;
		current = s;
		error = null;
		pinnedUntil = System.currentTimeMillis() + ALERT_PIN_MILLIS;
		onChange.run();
	}

	private boolean hasOffer(Integer itemId)
	{
		if (itemId == null)
		{
			return false;
		}
		for (GrandExchangeOffer offer : client.getGrandExchangeOffers())
		{
			if (offer != null && offer.getState() != GrandExchangeOfferState.EMPTY && offer.getItemId() == itemId)
			{
				return true;
			}
		}
		return false;
	}

	/** The item the Block button would block: the current buy suggestion's, or null. */
	public Integer blockableItemId()
	{
		return current != null && current.itemId != null && current.is(Suggestion.BUY) ? current.itemId : null;
	}

	/** Thumbs up (1) or down (-1) on the current suggestion. */
	public void rate(int rating)
	{
		if (current != null)
		{
			api.feedback(current.suggestionId, null, rating);
		}
	}

	private RequestBuilder.Settings settings()
	{
		RequestBuilder.Settings s = new RequestBuilder.Settings();
		s.risk = config.riskLevel().name().toLowerCase(Locale.ROOT);
		s.timeframeMinutes = config.checkIn().minutes;
		s.f2pOnly = config.f2pOnly();
		s.blockedItems = config.blockedItems();
		s.minProfit = config.minProfit();
		s.reservedSlots = config.reservedSlots();
		s.sellOnly = config.sellOnly();
		return s;
	}

	private List<SuggestionRequest.Offer> offers(Map<Integer, OfferSnapshot> tracked, long now)
	{
		List<SuggestionRequest.Offer> offers = new ArrayList<>();
		GrandExchangeOffer[] slots = client.getGrandExchangeOffers();
		for (int i = 0; i < slots.length; i++)
		{
			GrandExchangeOffer offer = slots[i];
			if (offer == null || offer.getState() == GrandExchangeOfferState.EMPTY || offer.getPrice() <= 0)
			{
				continue;
			}
			GrandExchangeOfferState state = offer.getState();
			SuggestionRequest.Offer o = new SuggestionRequest.Offer();
			o.slot = i;
			o.itemId = offer.getItemId();
			o.isBuy = state == GrandExchangeOfferState.BUYING || state == GrandExchangeOfferState.BOUGHT
				|| state == GrandExchangeOfferState.CANCELLED_BUY;
			o.price = offer.getPrice();
			o.totalQuantity = Math.max(1, offer.getTotalQuantity());
			o.filledQuantity = offer.getQuantitySold();
			o.active = state == GrandExchangeOfferState.BUYING || state == GrandExchangeOfferState.SELLING;
			OfferSnapshot seen = tracked.get(i);
			if (seen != null && seen.itemId == o.itemId && seen.price == o.price && seen.lastActivityMillis > 0)
			{
				o.idleMinutes = (int) Math.max(0, (now - seen.lastActivityMillis) / 60_000);
			}
			offers.add(o);
		}
		return offers;
	}

	/** Inventory by unnoted item ID. */
	private Map<Integer, Long> inventory()
	{
		Map<Integer, Long> totals = new HashMap<>();
		ItemContainer container = client.getItemContainer(InventoryID.INV);
		if (container == null)
		{
			return totals;
		}
		for (Item item : container.getItems())
		{
			if (item.getId() <= 0 || item.getQuantity() <= 0)
			{
				continue;
			}
			totals.merge(itemManager.canonicalize(item.getId()), (long) item.getQuantity(), Long::sum);
		}
		return totals;
	}
}
