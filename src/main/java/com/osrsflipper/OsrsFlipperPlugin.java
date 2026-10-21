package com.osrsflipper;

import com.google.gson.Gson;
import com.google.inject.Provides;
import com.osrsflipper.api.FlipperApiClient;
import com.osrsflipper.api.Me;
import com.osrsflipper.api.Suggestion;
import com.osrsflipper.auth.AuthSession;
import com.osrsflipper.auth.AuthState;
import com.osrsflipper.auth.TokenFile;
import com.osrsflipper.ge.GrandExchangeWidgets;
import com.osrsflipper.market.AlertController;
import com.osrsflipper.market.ConfirmGuard;
import com.osrsflipper.market.GuardState;
import com.osrsflipper.market.PriceCache;
import com.osrsflipper.ge.OfferHotkeys;
import com.osrsflipper.suggestion.SuggestionController;
import com.osrsflipper.sync.TradeSync;
import com.osrsflipper.sync.UsageCounter;
import com.osrsflipper.tracking.AccountTrades;
import com.osrsflipper.tracking.Flip;
import com.osrsflipper.tracking.OfferSnapshot;
import com.osrsflipper.tracking.OfferStatus;
import com.osrsflipper.tracking.TradeStore;
import com.osrsflipper.tracking.Transaction;
import com.osrsflipper.ui.FlipperPanel;
import com.osrsflipper.ui.PanelActions;
import com.osrsflipper.ui.HighlightOverlay;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.GrandExchangeOffer;
import net.runelite.api.GrandExchangeOfferState;
import net.runelite.api.MenuEntry;
import net.runelite.api.Player;
import net.runelite.api.WorldType;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.GrandExchangeOfferChanged;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.widgets.Widget;
import net.runelite.api.gameval.InventoryID;
import net.runelite.client.Notifier;
import net.runelite.client.RuneLite;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.LinkBrowser;

@Slf4j
@PluginDescriptor(
	name = "OSRS Flipper",
	description = "Grand Exchange flip suggestions, highlights and profit tracking",
	tags = {"grand exchange", "ge", "flipping", "merching", "profit", "trading"}
)
public class OsrsFlipperPlugin extends Plugin
{
	/** Ticks after login during which RuneLite's EMPTY slot events are not trusted. */
	private static final int LOGIN_BURST_TICKS = 2;
	static final String VERSION = "0.2.0";
	private static final long HELLO_MILLIS = 5 * 60_000;
	private static final long SYNC_MILLIS = 30_000;
	private static final long USAGE_MILLIS = 10 * 60_000;

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private ItemManager itemManager;

	@Inject
	private OsrsFlipperConfig config;

	@Inject
	private Gson gson;

	@Inject
	private ScheduledExecutorService executor;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private KeyManager keyManager;

	@Inject
	private SuggestionController suggestions;

	@Inject
	private HighlightOverlay highlightOverlay;

	@Inject
	private OfferHotkeys hotkeys;

	@Inject
	private AuthSession auth;

	@Inject
	private FlipperApiClient api;

	@Inject
	private UsageCounter usage;

	@Inject
	private GrandExchangeWidgets ge;

	@Inject
	private PriceCache prices;

	@Inject
	private GuardState guard;

	@Inject
	private AlertController alerts;

	@Inject
	private Notifier notifier;

	private FlipperPanel panel;
	private NavigationButton navButton;
	private TradeStore store;

	private long accountHash = -1;
	private AccountTrades trades;
	private final List<GrandExchangeOfferChanged> pendingOffers = new ArrayList<>();
	private int loginTick = -1;
	private GameState lastGameState;
	private long sessionStartMillis;

	/** Client thread only. */
	private Me me;
	private String accountError;
	private long lastHelloMillis;
	private boolean helloInFlight;
	private long lastSyncMillis;
	private boolean syncInFlight;
	private long lastUsageMillis;

	@Override
	protected void startUp()
	{
		File dir = new File(RuneLite.RUNELITE_DIR, "osrs-flipper");
		store = new TradeStore(gson, dir);
		panel = new FlipperPanel(new Actions());
		panel.setAdvanced(config.viewMode() == OsrsFlipperConfig.ViewMode.ADVANCED);
		auth.start(new TokenFile(gson, dir), config::apiUrl, () -> clientThread.invokeLater(this::onAuthChanged));
		BufferedImage icon = ImageUtil.loadImageResource(getClass(), "nav_icon.png");
		navButton = NavigationButton.builder()
			.tooltip("OSRS Flipper")
			.icon(icon)
			.priority(7)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navButton);
		sessionStartMillis = System.currentTimeMillis();

		suggestions.setOnChange(this::updateSuggestionPanel);
		overlayManager.add(highlightOverlay);
		keyManager.registerKeyListener(hotkeys);

		clientThread.invokeLater(() ->
		{
			updateSuggestionPanel();
			updateAccountPanel();
			lastGameState = client.getGameState();
			if (lastGameState == GameState.LOGGED_IN)
			{
				loginTick = client.getTickCount() - LOGIN_BURST_TICKS - 1;
				loadAccount(client.getAccountHash());
			}
		});
	}

	@Override
	protected void shutDown()
	{
		clientToolbar.removeNavigation(navButton);
		overlayManager.remove(highlightOverlay);
		keyManager.unregisterKeyListener(hotkeys);
		suggestions.setOnChange(() -> { });
		clientThread.invokeLater(suggestions::reset);
		auth.stop();
		flushUsage();
		saveAsync();
		me = null;
		trades = null;
		accountHash = -1;
		pendingOffers.clear();
	}

	@Provides
	OsrsFlipperConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(OsrsFlipperConfig.class);
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		GameState state = event.getGameState();
		if (state == GameState.LOGGED_IN && lastGameState != GameState.LOADING)
		{
			loginTick = client.getTickCount();
			loadAccount(client.getAccountHash());
		}
		else if (state == GameState.LOGIN_SCREEN || state == GameState.HOPPING)
		{
			saveAsync();
			if (state == GameState.LOGIN_SCREEN)
			{
				trades = null;
				accountHash = -1;
				me = null;
				suggestions.reset();
				alerts.reset();
				prices.clear();
				guard.setReason(null);
				updatePanel();
				updateAccountPanel();
				updateAlertPanel();
			}
		}
		lastGameState = state;
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		Map<Integer, Integer> holdings = trades == null
			? Collections.emptyMap()
			: trades.getMatcher().getHoldings();
		suggestions.tick(holdings, trades == null ? Collections.emptyMap() : trades.getSlots());

		long now = System.currentTimeMillis();
		if (auth.getState() == AuthState.SIGNED_IN && trades != null)
		{
			if (!helloInFlight && now - lastHelloMillis >= HELLO_MILLIS)
			{
				hello();
			}
			if (config.syncTrades() && !syncInFlight && now - lastSyncMillis >= SYNC_MILLIS)
			{
				syncTrades();
			}
		}
		if (auth.getState() == AuthState.SIGNED_IN && client.getGameState() == GameState.LOGGED_IN)
		{
			prices.want(pricedItems());
			if (config.dumpAlerts() && me != null && me.accountPremium() && accountHash != -1)
			{
				alerts.poll(accountHash, coins(), config.minProfit(), config.f2pOnly(),
					client.getWorldType().contains(WorldType.MEMBERS), this::onNewAlert, this::updateAlertPanel);
			}
		}
		updateGuard();
		if (now - lastUsageMillis >= USAGE_MILLIS)
		{
			lastUsageMillis = now;
			flushUsage();
		}
	}

	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded event)
	{
		if (!config.confirmGuard() || guard.getReason() == null)
		{
			return;
		}
		Widget confirm = ge.confirmButton();
		MenuEntry entry = event.getMenuEntry();
		if (confirm != null && entry.getParam1() == confirm.getId() && "Confirm".equals(entry.getOption()))
		{
			// Still on the right-click menu; just not on left-click.
			entry.setDeprioritized(true);
		}
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		if (event.getContainerId() == InventoryID.INV)
		{
			suggestions.invalidate();
		}
	}

	@Subscribe
	public void onGrandExchangeOfferChanged(GrandExchangeOfferChanged event)
	{
		suggestions.invalidate();
		if (!config.trackTrades())
		{
			return;
		}
		if (trades == null)
		{
			// Offers can arrive before the account is known; replay them once it is.
			pendingOffers.add(event);
			return;
		}
		handleOffer(event);
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (OsrsFlipperConfig.GROUP.equals(event.getGroup()))
		{
			clientThread.invokeLater(() ->
			{
				panel.setAdvanced(config.viewMode() == OsrsFlipperConfig.ViewMode.ADVANCED);
				updateAlertPanel();
				suggestions.invalidate();
				updateSuggestionPanel();
				updatePanel();
				if ("apiUrl".equals(event.getKey()) && auth.getState() == AuthState.SIGNED_IN)
				{
					// Tokens belong to the old server.
					auth.signOut();
				}
			});
		}
	}

	private void handleOffer(GrandExchangeOfferChanged event)
	{
		GrandExchangeOffer offer = event.getOffer();
		OfferStatus status = toStatus(offer.getState());
		OfferSnapshot snapshot = new OfferSnapshot(
			event.getSlot(),
			offer.getItemId(),
			status,
			offer.getPrice(),
			offer.getTotalQuantity(),
			offer.getQuantitySold(),
			offer.getSpent());

		String name = status == OfferStatus.EMPTY ? "" : itemManager.getItemComposition(offer.getItemId()).getName();
		boolean trustEmpty = client.getGameState() == GameState.LOGGED_IN
			&& client.getTickCount() - loginTick > LOGIN_BURST_TICKS;

		int before = trades.getTransactions().size();
		Flip flip = trades.onOffer(snapshot, name, System.currentTimeMillis(), trustEmpty);
		if (flip != null)
		{
			log.debug("Flip completed: {} x{} profit {}", flip.itemName, flip.quantity, flip.profit);
		}
		if (trades.getTransactions().size() != before)
		{
			updatePanel();
			saveAsync();
		}
	}

	private void loadAccount(long hash)
	{
		if (hash == -1 || hash == accountHash)
		{
			return;
		}
		accountHash = hash;
		me = null;
		lastHelloMillis = 0;
		lastSyncMillis = 0;
		try
		{
			trades = store.load(hash);
			trades.ensureClientIds();
		}
		catch (IOException | RuntimeException e)
		{
			log.warn("Could not load saved trades, starting fresh", e);
			trades = new AccountTrades();
		}
		List<GrandExchangeOfferChanged> pending = new ArrayList<>(pendingOffers);
		pendingOffers.clear();
		for (GrandExchangeOfferChanged event : pending)
		{
			handleOffer(event);
		}
		updatePanel();
	}

	private void saveAsync()
	{
		final AccountTrades toSave = trades;
		final long hash = accountHash;
		if (toSave == null || hash == -1)
		{
			return;
		}
		// Serialize on the client thread so the background write sees a consistent copy.
		final String json = gson.toJson(toSave);
		executor.execute(() ->
		{
			try
			{
				store.saveJson(hash, json);
			}
			catch (IOException e)
			{
				log.warn("Could not save trades", e);
			}
		});
	}

	private void updatePanel()
	{
		final AccountTrades current = trades;
		final List<Flip> recent = new ArrayList<>();
		long session = 0;
		long allTime = 0;
		if (current != null)
		{
			List<Flip> flips = current.getFlips();
			int from = Math.max(0, flips.size() - Math.max(0, config.recentFlipCount()));
			for (int i = flips.size() - 1; i >= from; i--)
			{
				recent.add(flips.get(i));
			}
			session = current.profitSince(sessionStartMillis);
			allTime = current.profitSince(0);
		}
		final long sessionProfit = session;
		final long allTimeProfit = allTime;
		final boolean loggedIn = current != null;
		SwingUtilities.invokeLater(() -> panel.update(loggedIn, sessionProfit, allTimeProfit, recent));
	}

	/** Runs on the client thread. */
	private void updateSuggestionPanel()
	{
		final boolean enabled = config.suggestionsEnabled();
		final Suggestion current = suggestions.getCurrent();
		final String error = suggestions.getError();
		SwingUtilities.invokeLater(() -> panel.showSuggestion(enabled, current, error));
	}

	/** Items whose market price the slot colours and guard need: open offers, the offer being set up, the suggestion. */
	private Set<Integer> pricedItems()
	{
		Set<Integer> ids = new HashSet<>();
		for (GrandExchangeOffer offer : client.getGrandExchangeOffers())
		{
			if (offer != null && offer.getState() != GrandExchangeOfferState.EMPTY)
			{
				ids.add(offer.getItemId());
			}
		}
		if (ge.isSetupOpen())
		{
			ids.add(ge.setupItemId());
		}
		Suggestion current = suggestions.getCurrent();
		if (current != null && current.itemId != null)
		{
			ids.add(current.itemId);
		}
		return ids;
	}

	/** Client thread. Works out whether the offer being set up looks like a typo. */
	private void updateGuard()
	{
		String reason = null;
		if (config.confirmGuard() && ge.isSetupOpen())
		{
			int itemId = ge.setupItemId();
			reason = ConfirmGuard.check(ge.isSetupSell(), itemId, ge.setupPrice(), prices.get(itemId),
				suggestions.getCurrent(), config.confirmGuardPercent());
		}
		guard.setReason(reason);
	}

	private long coins()
	{
		net.runelite.api.ItemContainer inv = client.getItemContainer(InventoryID.INV);
		if (inv == null)
		{
			return 0;
		}
		long total = 0;
		for (net.runelite.api.Item item : inv.getItems())
		{
			if (item.getId() == com.osrsflipper.suggestion.RequestBuilder.Inventory.COINS)
			{
				total += item.getQuantity();
			}
		}
		return total;
	}

	/** Client thread. */
	private void onNewAlert(com.osrsflipper.api.DumpAlert alert)
	{
		notifier.notify(config.dumpNotification(), String.format("Dump alert: %s is %.0f%% below its usual price.",
			alert.name, alert.dropPercent));
	}

	/** Client thread. */
	private void updateAlertPanel()
	{
		final List<com.osrsflipper.api.DumpAlert> list = config.dumpAlerts() ? alerts.getAlerts() : Collections.emptyList();
		SwingUtilities.invokeLater(() -> panel.showAlerts(list));
	}

	/** Client thread. */
	private void onAuthChanged()
	{
		if (auth.getState() != AuthState.SIGNED_IN)
		{
			me = null;
		}
		lastHelloMillis = 0;
		lastSyncMillis = 0;
		suggestions.invalidate();
		updateAccountPanel();
		updateSuggestionPanel();
	}

	/** Client thread. Links the game account and refreshes plan and announcements. */
	private void hello()
	{
		if (accountHash == -1)
		{
			return;
		}
		helloInFlight = true;
		lastHelloMillis = System.currentTimeMillis();
		final long hash = accountHash;
		api.hello(Long.toString(hash), playerName(), VERSION,
			result -> clientThread.invokeLater(() ->
			{
				helloInFlight = false;
				if (hash == accountHash)
				{
					me = result;
					accountError = null;
					updateAccountPanel();
				}
			}),
			message -> clientThread.invokeLater(() ->
			{
				helloInFlight = false;
				accountError = message;
				updateAccountPanel();
			}));
	}

	/** Client thread. Uploads tracked trades the server doesn't have yet, one batch at a time. */
	private void syncTrades()
	{
		lastSyncMillis = System.currentTimeMillis();
		final AccountTrades current = trades;
		final List<Transaction> batch = TradeSync.pending(current.getTransactions(), TradeSync.BATCH);
		if (batch.isEmpty())
		{
			return;
		}
		syncInFlight = true;
		api.uploadTrades(TradeSync.build(accountHash, playerName(), batch),
			accepted -> clientThread.invokeLater(() ->
			{
				syncInFlight = false;
				TradeSync.markUploaded(batch);
				if (current == trades)
				{
					saveAsync();
					if (batch.size() == TradeSync.BATCH)
					{
						// More waiting: go again on the next tick.
						lastSyncMillis = 0;
					}
				}
			}),
			message -> clientThread.invokeLater(() ->
			{
				syncInFlight = false;
				log.debug("Trade upload failed: {}", message);
			}));
	}

	private void flushUsage()
	{
		Map<String, Integer> counts = usage.drain();
		if (!counts.isEmpty() && auth.getState() == AuthState.SIGNED_IN)
		{
			api.usage(counts);
		}
	}

	private String playerName()
	{
		Player player = client.getLocalPlayer();
		return player == null || player.getName() == null ? "" : player.getName();
	}

	/** Client thread. */
	private void updateAccountPanel()
	{
		final AuthState state = auth.getState();
		final Me current = me;
		final String error = accountError;
		final com.osrsflipper.api.DeviceStart linking = auth.getLinking();
		SwingUtilities.invokeLater(() -> panel.showAccount(state, linking, current, error));
	}

	/** Opens a page only if it's on the configured server, so the panel can't be made to open anything else. */
	private void browse(String url)
	{
		String server = auth.server();
		if (server != null && url != null && (url.equals(server) || url.startsWith(server + "/")))
		{
			LinkBrowser.browse(url);
		}
	}

	/** Button handlers. Called on the Swing thread; work hops to the client thread. */
	private class Actions implements PanelActions
	{
		@Override
		public void signIn()
		{
			usage.add("panel.sign_in");
			auth.beginLink(
				url -> SwingUtilities.invokeLater(() -> browse(url)),
				message -> clientThread.invokeLater(() ->
				{
					accountError = message;
					updateAccountPanel();
				}));
			clientThread.invokeLater(() ->
			{
				accountError = null;
				updateAccountPanel();
			});
		}

		@Override
		public void reopenSignIn()
		{
			com.osrsflipper.api.DeviceStart linking = auth.getLinking();
			if (linking != null)
			{
				browse(linking.verificationUriComplete);
			}
		}

		@Override
		public void cancelSignIn()
		{
			auth.cancelLink();
		}

		@Override
		public void signOut()
		{
			clientThread.invokeLater(() ->
			{
				flushUsage();
				auth.signOut();
			});
		}

		@Override
		public void openWebsite()
		{
			String server = auth.server();
			if (server != null)
			{
				usage.add("panel.website");
				browse(server + "/dashboard");
			}
		}

		@Override
		public void skip()
		{
			usage.add("panel.skip");
			clientThread.invokeLater(suggestions::skip);
		}

		@Override
		public void rate(int rating)
		{
			usage.add(rating > 0 ? "panel.rate_good" : "panel.rate_bad");
			clientThread.invokeLater(() -> suggestions.rate(rating));
		}

		@Override
		public void useAlert(com.osrsflipper.api.DumpAlert alert)
		{
			usage.add("panel.use_alert");
			clientThread.invokeLater(() -> suggestions.useAlert(alert));
		}

		@Override
		public void dismissAlert(String alertId)
		{
			clientThread.invokeLater(() ->
			{
				alerts.dismiss(alertId);
				updateAlertPanel();
			});
		}

		@Override
		public void explain()
		{
			usage.add("panel.explain");
			clientThread.invokeLater(() ->
			{
				Suggestion current = suggestions.getCurrent();
				if (current == null || current.itemId == null)
				{
					SwingUtilities.invokeLater(() -> panel.showExplanation(null));
					return;
				}
				api.explain(current.itemId,
					answer -> SwingUtilities.invokeLater(() -> panel.showExplanation(answer == null ? null : answer.answer)),
					message -> SwingUtilities.invokeLater(() -> panel.showExplanation(message)));
			});
		}
	}

	static OfferStatus toStatus(GrandExchangeOfferState state)
	{
		switch (state)
		{
			case BUYING:
				return OfferStatus.BUYING;
			case BOUGHT:
				return OfferStatus.BOUGHT;
			case CANCELLED_BUY:
				return OfferStatus.CANCELLED_BUY;
			case SELLING:
				return OfferStatus.SELLING;
			case SOLD:
				return OfferStatus.SOLD;
			case CANCELLED_SELL:
				return OfferStatus.CANCELLED_SELL;
			case EMPTY:
			default:
				return OfferStatus.EMPTY;
		}
	}
}
