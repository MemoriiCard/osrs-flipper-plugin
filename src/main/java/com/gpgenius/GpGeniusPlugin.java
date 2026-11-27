package com.gpgenius;

import com.google.gson.Gson;
import com.google.inject.Provides;
import com.gpgenius.api.FlipperApiClient;
import com.gpgenius.api.GoalRequest;
import com.gpgenius.api.Progress;
import com.gpgenius.api.Me;
import com.gpgenius.api.SlotPlan;
import com.gpgenius.api.Suggestion;
import com.gpgenius.auth.AuthSession;
import com.gpgenius.auth.AuthState;
import com.gpgenius.auth.DataConsent;
import com.gpgenius.auth.TokenFile;
import com.gpgenius.ge.GrandExchangeWidgets;
import com.gpgenius.market.AlertController;
import com.gpgenius.market.ConfirmGuard;
import com.gpgenius.market.GuardState;
import com.gpgenius.market.ChartController;
import com.gpgenius.market.PriceCache;
import com.gpgenius.ge.OfferHotkeys;
import com.gpgenius.history.FlippingUtilitiesImport;
import com.gpgenius.notify.CheckInDigest;
import com.gpgenius.notify.SoundCue;
import com.gpgenius.notify.SoundCues;
import com.gpgenius.setup.ProfileController;
import com.gpgenius.suggestion.RequestBuilder;
import com.gpgenius.suggestion.SuggestionController;
import com.gpgenius.sync.TradeSync;
import com.gpgenius.sync.UsageCounter;
import com.gpgenius.tracking.AccountTrades;
import com.gpgenius.tracking.Flip;
import com.gpgenius.tracking.OfferSnapshot;
import com.gpgenius.tracking.OfferStatus;
import com.gpgenius.tracking.TradeStore;
import com.gpgenius.tracking.Transaction;
import com.gpgenius.ui.FlipperPanel;
import com.gpgenius.ui.PanelActions;
import com.gpgenius.ui.HighlightOverlay;
import com.gpgenius.ui.Theme;
import com.gpgenius.market.SlotColor;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.time.LocalTime;
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
import net.runelite.api.SoundEffectID;
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
import net.runelite.client.config.Notification;
import net.runelite.client.config.NotificationSound;
import net.runelite.client.config.RuneLiteConfig;
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
	name = "GP Genius",
	description = "Grand Exchange flip suggestions, highlights and profit tracking",
	tags = {"grand exchange", "ge", "flipping", "merching", "profit", "trading", "money making", "gp"}
)
public class GpGeniusPlugin extends Plugin
{
	/** Ticks after login during which RuneLite's EMPTY slot events are not trusted. */
	private static final int LOGIN_BURST_TICKS = 2;
	public static final String VERSION = "0.5.0";
	private static final long HELLO_MILLIS = 5 * 60_000;
	private static final long SYNC_MILLIS = 30_000;
	/** Profit at a glance and the goal's pace refresh this often, and right after each trade upload. */
	private static final long PROGRESS_MILLIS = 2 * 60_000;
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
	private GpGeniusConfig config;

	@Inject
	private ConfigManager configManager;

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
	private net.runelite.client.eventbus.EventBus eventBus;

	/** The per-character "Leaderboard name" setting, kept in step with the website. */
	@Inject
	private com.gpgenius.leaderboard.LeaderboardSync leaderboardSync;

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

	@Inject
	private RuneLiteConfig runeLiteConfig;

	private FlipperPanel panel;
	/** Fast-start presets and saved profiles. Thread-safe. */
	private ProfileController profiles;
	private long lastProfileLoadMillis;
	/** Client thread only. Groups news about the player's offers into one reminder per check-in. */
	private final CheckInDigest digest = new CheckInDigest();
	/** Client thread only. When the optional sound cues play. */
	private final SoundCues cues = new SoundCues();
	private boolean importInFlight;
	private ChartController chart;
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
	/** The pop-out trade dashboard, made the first time it's opened. Swing thread only. */
	private com.gpgenius.ui.TradeDashboard tradeDashboard;
	private long lastUsageMillis;
	/** Client thread only. From the server while signed in. */
	private Progress progress;
	private long lastProgressMillis;
	private boolean progressInFlight;

	@Override
	protected void startUp()
	{
		migrateSettings();
		File dir = new File(RuneLite.RUNELITE_DIR, "gp-genius");
		store = new TradeStore(gson, dir);
		panel = new FlipperPanel(new Actions());
		panel.setAdvanced(config.viewMode() == GpGeniusConfig.ViewMode.ADVANCED);
		panel.setTheme(effectiveTheme());
		panel.setTextScale(config.textSize().scale);
		panel.showSetup(!config.setupDone());
		panel.setCompact(config.compactLayout());
		profiles = new ProfileController(config, configManager, api, this::updateProfilePanel);
		auth.start(new TokenFile(gson, dir), config::apiUrl, () -> clientThread.invokeLater(this::onAuthChanged));
		BufferedImage icon = ImageUtil.loadImageResource(getClass(), "nav_icon.png");
		navButton = NavigationButton.builder()
			.tooltip("GP Genius")
			.icon(icon)
			.priority(7)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navButton);
		sessionStartMillis = System.currentTimeMillis();

		suggestions.setOnChange(this::updateSuggestionPanel);
		chart = new ChartController(api, clientThread::invokeLater);
		chart.setOnChange(this::updateChartPanel);
		overlayManager.add(highlightOverlay);
		hotkeys.setShortcuts(new Shortcuts());
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
			eventBus.register(leaderboardSync);
			leaderboardSync.start();
		});
	}

	@Override
	protected void shutDown()
	{
		clientToolbar.removeNavigation(navButton);
		eventBus.unregister(leaderboardSync);
		SwingUtilities.invokeLater(() ->
		{
			if (tradeDashboard != null)
			{
				tradeDashboard.dispose();
				tradeDashboard = null;
			}
		});
		overlayManager.remove(highlightOverlay);
		keyManager.unregisterKeyListener(hotkeys);
		hotkeys.setShortcuts(null);
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
	GpGeniusConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(GpGeniusConfig.class);
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
				cues.reset();
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
		boolean connected = auth.getState() == AuthState.SIGNED_IN && consented();
		if (connected)
		{
			// Retry a failed load once a minute rather than every tick.
			if (!profiles.isLoaded() && now - lastProfileLoadMillis >= 60_000)
			{
				lastProfileLoadMillis = now;
				profiles.load();
			}
			profiles.tick(now);
		}
		if (connected && trades != null)
		{
			if (!helloInFlight && now - lastHelloMillis >= HELLO_MILLIS)
			{
				hello();
			}
			if (config.syncTrades() && !syncInFlight && now - lastSyncMillis >= SYNC_MILLIS)
			{
				syncTrades();
			}
			if (!progressInFlight && now - lastProgressMillis >= PROGRESS_MILLIS)
			{
				refreshProgress(null);
			}
		}
		if (connected && client.getGameState() == GameState.LOGGED_IN)
		{
			prices.want(pricedItems());
			watchOutbid();
			chart.tick(config.priceChart() ? chartItem() : null, now);
			if (config.dumpAlerts() && me != null && me.accountPremium() && accountHash != -1)
			{
				alerts.poll(accountHash, coins(), config.minProfit(),
					config.itemScope() == GpGeniusConfig.ItemScope.F2P,
					client.getWorldType().contains(WorldType.MEMBERS), this::onNewAlert, this::updateAlertPanel);
			}
		}
		else
		{
			chart.tick(null, now);
		}
		updateGuard();
		checkIn(now);
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
		boolean trusted = client.getGameState() == GameState.LOGGED_IN
			&& client.getTickCount() - loginTick > LOGIN_BURST_TICKS;
		digest.offerChanged(event.getSlot(), toStatus(event.getOffer().getState()),
			trusted && config.remindCompleted(), System.currentTimeMillis());
		GrandExchangeOfferState offerState = event.getOffer().getState();
		if (offerState == GrandExchangeOfferState.EMPTY)
		{
			cues.clearSlot(event.getSlot());
		}
		else if (trusted && (offerState == GrandExchangeOfferState.BOUGHT || offerState == GrandExchangeOfferState.SOLD))
		{
			playCue(SoundCue.OFFER_DONE);
		}
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
		if (GpGeniusConfig.GROUP.equals(event.getGroup()))
		{
			if (DataConsent.CONFIG_KEY.equals(event.getKey()))
			{
				clientThread.invokeLater(this::updateAccountPanel);
			}
			if ("theme".equals(event.getKey()) || "highContrast".equals(event.getKey()))
			{
				SwingUtilities.invokeLater(() ->
				{
					panel.setTheme(effectiveTheme());
					if (tradeDashboard != null)
					{
						tradeDashboard.setTheme(effectiveTheme());
					}
				});
			}
			if ("textSize".equals(event.getKey()))
			{
				SwingUtilities.invokeLater(() -> panel.setTextScale(config.textSize().scale));
			}
			if ("compactLayout".equals(event.getKey()))
			{
				SwingUtilities.invokeLater(() -> panel.setCompact(config.compactLayout()));
			}
			profiles.settingChanged(event.getKey(), System.currentTimeMillis());
			if ("setupDone".equals(event.getKey()) && !config.setupDone())
			{
				SwingUtilities.invokeLater(() -> panel.showSetup(true));
			}
			clientThread.invokeLater(() ->
			{
				panel.setAdvanced(config.viewMode() == GpGeniusConfig.ViewMode.ADVANCED);
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
			if (config.remindCompleted())
			{
				digest.flipProfit(flip.profit, System.currentTimeMillis());
			}
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
		lastProgressMillis = 0;
		// The player is here now; the first reminder comes a check-in window later at the soonest.
		digest.resetSlots();
		digest.seen(System.currentTimeMillis());
		suggestions.startPlan();
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
		final Suggestion.Plan plan = enabled ? suggestions.getPlan() : null;
		final boolean overnight = enabled && suggestions.isOvernight();
		final int overnightHours = suggestions.getOvernightHours();
		final boolean overnightAvailable = enabled && auth.getState() == AuthState.SIGNED_IN && consented()
			&& client.getGameState() == GameState.LOGGED_IN;
		final SlotPlan slotPlan = enabled && (config.slotManager() || overnight) ? suggestions.getSlotPlan() : null;
		if (cues.stepChanged(enabled ? SoundCues.stepKey(current) : null))
		{
			playCue(SoundCue.NEXT_STEP);
		}
		if (enabled && current != null && current.action != null)
		{
			digest.suggestion(current.action, current.name, current.headline(), config.remindReprice(),
				config.remindNextStep(), System.currentTimeMillis());
		}
		SwingUtilities.invokeLater(() ->
		{
			panel.showSuggestion(enabled, current, error);
			panel.showPlan(plan);
			panel.showSlots(slotPlan);
			panel.showOvernight(overnightAvailable, overnight, overnightHours);
		});
	}

	/** Swing thread. Opens (or brings forward) the trade dashboard and loads the latest trades. */
	private void showTradeDashboard()
	{
		if (auth.getState() != AuthState.SIGNED_IN || !consented())
		{
			panel.showExportMessage("Sign in first to see your trades.");
			return;
		}
		if (tradeDashboard == null)
		{
			tradeDashboard = new com.gpgenius.ui.TradeDashboard(new com.gpgenius.ui.TradeDashboard.Source()
			{
				@Override
				public void load(int days, Long beforeId, java.util.function.Consumer<com.gpgenius.api.TradeHistory> onSuccess,
					java.util.function.Consumer<String> onError)
				{
					api.history(days, beforeId, onSuccess, onError);
				}

				@Override
				public void export(int days)
				{
					exportTradesCsv(days);
				}
			});
			tradeDashboard.setTheme(effectiveTheme());
		}
		tradeDashboard.open();
	}

	/** Downloads the player's trades as CSV and asks where to save it. Any thread. */
	private void exportTradesCsv(int days)
	{
		if (auth.getState() != AuthState.SIGNED_IN || !consented())
		{
			SwingUtilities.invokeLater(() -> panel.showExportMessage("Sign in first to export your trades."));
			return;
		}
		api.exportTrades(days,
			csv -> SwingUtilities.invokeLater(() -> saveCsv(csv)),
			message -> SwingUtilities.invokeLater(() -> exportDone(message)));
	}

	/** Swing thread. */
	private void saveCsv(String csv)
	{
		java.awt.Component parent = tradeDashboard != null && tradeDashboard.isVisible() ? tradeDashboard : panel;
		File file = com.gpgenius.ui.TradeDashboardFiles.chooseSaveFile(parent,
			new File(RuneLite.RUNELITE_DIR, "gp-genius"), java.time.LocalDate.now());
		if (file == null)
		{
			exportDone(null);
			return;
		}
		executor.execute(() ->
		{
			String message;
			try
			{
				java.nio.file.Files.write(file.toPath(), csv == null ? new byte[0]
					: csv.getBytes(java.nio.charset.StandardCharsets.UTF_8));
				int rows = com.gpgenius.ui.TradeDashboardFiles.rows(csv);
				message = "Saved " + rows + (rows == 1 ? " trade" : " trades") + " to " + file.getName() + ".";
			}
			catch (IOException e)
			{
				log.debug("Couldn't save the export", e);
				message = "Couldn't save the file: " + e.getMessage();
			}
			final String done = message;
			SwingUtilities.invokeLater(() -> exportDone(done));
		});
	}

	/** Swing thread. Shows how an export went in the panel and, if open, the dashboard. */
	private void exportDone(String message)
	{
		panel.showExportMessage(message);
		if (tradeDashboard != null && message != null)
		{
			tradeDashboard.showStatus(message);
		}
	}

	private Theme effectiveTheme()
	{
		return Theme.effective(config.theme(), config.highContrast());
	}

	/** Client thread. Plays a sound cue if the player turned it on and it hasn't just played. */
	private void playCue(SoundCue cue)
	{
		if (!client.isClientThread())
		{
			clientThread.invokeLater(() -> playCue(cue));
			return;
		}
		if (!cues.play(cue, config.sounds(), config.soundCues(), System.currentTimeMillis()))
		{
			return;
		}
		int id;
		switch (cue)
		{
			case OFFER_DONE:
				id = SoundEffectID.GE_COIN_TINKLE;
				break;
			case OUTBID:
				id = SoundEffectID.GE_DECREMENT_PLOP;
				break;
			case NEXT_STEP:
			default:
				id = SoundEffectID.UI_BOOP;
				break;
		}
		// Game sound effects go up to 127.
		int volume = Math.max(1, Math.min(127, Math.round(config.soundCueVolume() * 127 / 100f)));
		client.playSoundEffect(id, volume);
	}

	/** Client thread. Sounds the outbid cue when an offer that would fill has just been passed by the market. */
	private void watchOutbid()
	{
		if (config.soundCues() == null || !config.soundCues().contains(SoundCue.OUTBID))
		{
			return;
		}
		GrandExchangeOffer[] offers = client.getGrandExchangeOffers();
		boolean outbid = false;
		for (int i = 0; i < offers.length; i++)
		{
			GrandExchangeOffer offer = offers[i];
			if (offer == null)
			{
				cues.clearSlot(i);
				continue;
			}
			GrandExchangeOfferState state = offer.getState();
			boolean buying = state == GrandExchangeOfferState.BUYING;
			SlotColor color = buying || state == GrandExchangeOfferState.SELLING
				? SlotColor.of(buying, offer.getPrice(), prices.get(offer.getItemId()))
				: SlotColor.NONE;
			outbid |= cues.slotColor(i, color);
		}
		if (outbid)
		{
			playCue(SoundCue.OUTBID);
		}
	}

	/** The optional panel hotkeys. Client thread. */
	private class Shortcuts implements OfferHotkeys.Shortcuts
	{
		@Override
		public void toggleHighlights()
		{
			configManager.setConfiguration(GpGeniusConfig.GROUP, "highlightOffers", !config.highlightOffers());
		}

		@Override
		public void refresh()
		{
			suggestions.invalidate();
		}

		@Override
		public void nextProfile()
		{
			profiles.switchToNext();
		}

		@Override
		public void toggleView()
		{
			GpGeniusConfig.ViewMode next = config.viewMode() == GpGeniusConfig.ViewMode.ADVANCED
				? GpGeniusConfig.ViewMode.SIMPLE
				: GpGeniusConfig.ViewMode.ADVANCED;
			configManager.setConfiguration(GpGeniusConfig.GROUP, "viewMode", next);
		}

		@Override
		public void toggleCompact()
		{
			configManager.setConfiguration(GpGeniusConfig.GROUP, "compactLayout", !config.compactLayout());
		}
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

	/** The item being set up in the GE, otherwise the suggested item. */
	private Integer chartItem()
	{
		if (ge.isSetupOpen() && ge.setupItemId() > 0)
		{
			return ge.setupItemId();
		}
		Suggestion current = suggestions.getCurrent();
		return current == null ? null : current.itemId;
	}

	/** Client thread. */
	private void updateChartPanel()
	{
		final com.gpgenius.api.PriceHistory history = chart.getHistory();
		final String period = chart.getPeriod();
		final String error = chart.getError();
		SwingUtilities.invokeLater(() -> panel.showChart(history, period, error));
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

	/** Client thread. Sends the check-in reminder when one is due; never while the player is at the GE. */
	private void checkIn(long now)
	{
		boolean atGe = client.getGameState() == GameState.LOGGED_IN && ge.isOpen();
		suggestions.noteGe(atGe, now);
		if (atGe)
		{
			digest.seen(now);
			return;
		}
		boolean quiet = config.quietHours()
			&& CheckInDigest.inQuietHours(LocalTime.now().getHour(), config.quietStart(), config.quietEnd());
		if (digest.due(now, checkInMinutes() * 60_000L, quiet))
		{
			notifier.notify(sounds(config.checkInNotification()), digest.drain(now));
		}
	}

	/**
	 * How often to remind the player to check in: the goal's rhythm while a goal that adapts is running
	 * for this character, otherwise the player's own setting.
	 */
	private int checkInMinutes()
	{
		Progress.Goal goal = progress == null ? null : progress.goal;
		if (goal != null && goal.adapt && goal.isLive() && goal.checkInMinutes != null && goal.checkInMinutes > 0
			&& (goal.accountId == null || goal.accountId.equals(progress.currentId)))
		{
			return goal.checkInMinutes;
		}
		return config.checkIn().minutes;
	}

	/** Client thread. Fetches profit at a glance, the goal and the suggested stack. */
	private void refreshProgress(String errorPrefix)
	{
		if (auth.getState() != AuthState.SIGNED_IN || !consented())
		{
			return;
		}
		progressInFlight = true;
		lastProgressMillis = System.currentTimeMillis();
		final long hash = accountHash;
		api.progress(hash == -1 ? null : Long.toString(hash),
			result -> clientThread.invokeLater(() ->
			{
				progressInFlight = false;
				if (hash == accountHash)
				{
					showProgress(result, null);
				}
			}),
			message -> clientThread.invokeLater(() ->
			{
				progressInFlight = false;
				log.debug("Progress refresh failed: {}", message);
				if (errorPrefix != null)
				{
					showProgress(progress, errorPrefix + message);
				}
			}));
	}

	/** Client thread. */
	private void showProgress(Progress result, String error)
	{
		progress = result;
		SwingUtilities.invokeLater(() -> panel.showProgress(result, error));
	}

	/** Client thread. Replies from session resets and goal changes carry the new progress. */
	private void onProgressReply(Progress result)
	{
		lastProgressMillis = System.currentTimeMillis();
		showProgress(result, null);
	}

	/** Carries settings from older versions over to their replacements. */
	private void migrateSettings()
	{
		if (config.f2pOnly())
		{
			configManager.setConfiguration(GpGeniusConfig.GROUP, "itemScope", GpGeniusConfig.ItemScope.F2P);
			configManager.unsetConfiguration(GpGeniusConfig.GROUP, "f2pOnly");
		}
	}

	/** Client thread. Reads Flipping Utilities' saved trades for this character off the client thread, then merges them. */
	private void importHistory()
	{
		if (trades == null || accountHash == -1)
		{
			SwingUtilities.invokeLater(() -> panel.showImportMessage("Log in first, on the character whose history you want."));
			return;
		}
		if (importInFlight)
		{
			return;
		}
		final String name = playerName();
		final File file = FlippingUtilitiesImport.fileFor(RuneLite.RUNELITE_DIR, name);
		if (file == null)
		{
			SwingUtilities.invokeLater(() -> panel.showImportMessage("Wait until your character has loaded, then try again."));
			return;
		}
		importInFlight = true;
		final AccountTrades target = trades;
		executor.execute(() ->
		{
			FlippingUtilitiesImport.Result result = null;
			String failure = null;
			if (!file.isFile())
			{
				failure = "No Flipping Utilities history found for " + name + " (looked for .runelite/flipping/"
					+ file.getName() + ").";
			}
			else
			{
				try
				{
					result = FlippingUtilitiesImport.read(file);
				}
				catch (IOException e)
				{
					log.debug("Flipping Utilities import failed", e);
					failure = e.getMessage();
				}
			}
			final FlippingUtilitiesImport.Result read = result;
			final String error = failure;
			clientThread.invokeLater(() ->
			{
				importInFlight = false;
				if (error != null || target != trades)
				{
					final String message = error != null ? error : "You logged out before the import finished.";
					SwingUtilities.invokeLater(() -> panel.showImportMessage(message));
					return;
				}
				for (Transaction t : read.trades)
				{
					if (t.itemName == null || t.itemName.isEmpty())
					{
						t.itemName = itemManager.getItemComposition(t.itemId).getName();
					}
				}
				int added = trades.importHistory(read.trades);
				usage.add("panel.import");
				if (added > 0)
				{
					saveAsync();
					updatePanel();
					suggestions.invalidate();
					lastSyncMillis = 0;
				}
				int already = read.trades.size() - added;
				final String message = read.trades.isEmpty()
					? "Flipping Utilities had no trades saved for " + name + "."
					: String.format("Imported %,d trades from Flipping Utilities%s.", added,
						already > 0 ? String.format(" (%,d were already here)", already) : "");
				SwingUtilities.invokeLater(() -> panel.showImportMessage(message));
			});
		});
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
			if (item.getId() == com.gpgenius.suggestion.RequestBuilder.Inventory.COINS)
			{
				total += item.getQuantity();
			}
		}
		return total;
	}

	/** Client thread. */
	private void onNewAlert(com.gpgenius.api.DumpAlert alert)
	{
		notifier.notify(sounds(config.dumpNotification()), String.format("Dump alert: %s is %.0f%% below its usual price.",
			alert.name, alert.dropPercent));
	}

	/** Client thread. */
	private void updateAlertPanel()
	{
		final List<com.gpgenius.api.DumpAlert> list = config.dumpAlerts() ? alerts.getAlerts() : Collections.emptyList();
		SwingUtilities.invokeLater(() -> panel.showAlerts(list));
	}

	/** Client thread. */
	private void onAuthChanged()
	{
		if (auth.getState() != AuthState.SIGNED_IN)
		{
			me = null;
			profiles.reset();
			updateProfilePanel();
		}
		lastProfileLoadMillis = 0;
		lastHelloMillis = 0;
		lastSyncMillis = 0;
		lastProgressMillis = 0;
		if (auth.getState() != AuthState.SIGNED_IN)
		{
			showProgress(null, null);
		}
		suggestions.invalidate();
		updateAccountPanel();
		updateSuggestionPanel();
	}

	/** Any thread. Redraws the profile bar and preset buttons. */
	private void updateProfilePanel()
	{
		final java.util.List<com.gpgenius.setup.Preset> presetList = profiles.getPresets();
		final java.util.List<com.gpgenius.api.Profiles.Profile> list = profiles.getProfiles();
		final String active = profiles.activeId();
		final String error = profiles.getError();
		SwingUtilities.invokeLater(() -> panel.showProfiles(presetList, list, active, error));
	}

	/**
	 * The notification as set, or the same without sound when the profile's sounds are off. A
	 * notification that uses RuneLite's defaults is rebuilt from them with the sound off.
	 */
	private Notification sounds(Notification n)
	{
		if (config.sounds() || n == null || !n.isEnabled())
		{
			return n;
		}
		if (n.isOverride() && n.isInitialized())
		{
			return n.withSound(NotificationSound.OFF);
		}
		RuneLiteConfig rl = runeLiteConfig;
		return new Notification(true, true, true, rl.enableTrayNotifications(), n.getTrayIconType(),
			rl.notificationRequestFocus(), NotificationSound.OFF, null, rl.notificationVolume(), rl.notificationTimeout(),
			rl.enableGameMessageNotification(), rl.flashNotification(), rl.notificationFlashColor(),
			rl.sendNotificationsWhenFocused());
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
				// New flips show up in profit at a glance and the goal right away.
				lastProgressMillis = 0;
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
		if (!counts.isEmpty() && auth.getState() == AuthState.SIGNED_IN && consented())
		{
			api.usage(counts);
		}
	}

	/** Whether the player agreed to the current data notice. Nothing is sent to the server until they do. */
	private boolean consented()
	{
		return DataConsent.agreed(config.dataConsent());
	}

	private void agreeToNotice()
	{
		configManager.setConfiguration(GpGeniusConfig.GROUP, DataConsent.CONFIG_KEY, DataConsent.agreedValue());
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
		final com.gpgenius.api.DeviceStart linking = auth.getLinking();
		final boolean consented = consented();
		SwingUtilities.invokeLater(() -> panel.showAccount(state, linking, current, error, consented));
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
			agreeToNotice();
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
		public void agree()
		{
			agreeToNotice();
			clientThread.invokeLater(() ->
			{
				lastHelloMillis = 0;
				lastSyncMillis = 0;
				suggestions.invalidate();
				updateAccountPanel();
				updateSuggestionPanel();
			});
		}

		@Override
		public void reopenSignIn()
		{
			com.gpgenius.api.DeviceStart linking = auth.getLinking();
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
		public void explain()
		{
			usage.add("panel.explain");
			clientThread.invokeLater(() ->
			{
				Integer itemId = suggestions.currentItemId();
				if (itemId == null)
				{
					return;
				}
				api.explain(itemId,
					answer -> SwingUtilities.invokeLater(() -> panel.showExplanation(answer)),
					message -> SwingUtilities.invokeLater(() -> panel.showExplanation(message)));
			});
		}

		@Override
		public void block()
		{
			usage.add("panel.block");
			clientThread.invokeLater(() ->
			{
				Integer itemId = suggestions.blockableItemId();
				if (itemId != null)
				{
					configManager.setConfiguration(GpGeniusConfig.GROUP, "blockedItems",
						RequestBuilder.withItemId(config.blockedItems(), itemId));
					suggestions.skip();
				}
			});
		}

		@Override
		public void favourite()
		{
			usage.add("panel.favourite");
			clientThread.invokeLater(() ->
			{
				Integer itemId = suggestions.blockableItemId();
				if (itemId != null)
				{
					configManager.setConfiguration(GpGeniusConfig.GROUP, "favouriteItems",
						RequestBuilder.withItemId(config.favouriteItems(), itemId));
				}
			});
		}

		@Override
		public void importHistory()
		{
			clientThread.invokeLater(GpGeniusPlugin.this::importHistory);
		}

		@Override
		public void choosePreset(String presetId)
		{
			com.gpgenius.setup.Preset preset = com.gpgenius.setup.Preset.byId(profiles.getPresets(), presetId);
			if (preset == null)
			{
				return;
			}
			usage.add("setup." + preset.key());
			profiles.choosePreset(preset);
		}

		@Override
		public void switchProfile(String profileId)
		{
			usage.add("profile.switch");
			profiles.switchTo(profileId);
		}

		@Override
		public void newProfile(String name)
		{
			usage.add("profile.new");
			profiles.create(name);
		}

		@Override
		public void renameProfile(String profileId, String name)
		{
			profiles.rename(profileId, name);
		}

		@Override
		public void deleteProfile(String profileId)
		{
			usage.add("profile.delete");
			profiles.delete(profileId);
		}

		@Override
		public void skipSetup()
		{
			usage.add("setup.skip");
			configManager.setConfiguration(GpGeniusConfig.GROUP, "setupDone", true);
		}

		@Override
		public void startOvernight(int hours)
		{
			usage.add("panel.overnight");
			clientThread.invokeLater(() -> suggestions.startOvernight(hours));
		}

		@Override
		public void stopOvernight()
		{
			clientThread.invokeLater(suggestions::stopOvernight);
		}

		@Override
		public void openTradeDashboard()
		{
			usage.add("panel.trade_dashboard");
			SwingUtilities.invokeLater(GpGeniusPlugin.this::showTradeDashboard);
		}

		@Override
		public void exportTrades()
		{
			usage.add("panel.export");
			exportTradesCsv(com.gpgenius.ui.TradeDashboardFiles.ALL_DAYS);
		}

		@Override
		public void resetSession(String scope)
		{
			usage.add("panel.reset_session");
			clientThread.invokeLater(() ->
			{
				boolean all = Progress.ALL.equals(scope);
				boolean mine = all || Progress.CURRENT.equals(scope)
					|| (progress != null && scope != null && scope.equals(progress.currentId));
				if (mine)
				{
					// This computer's own session total, shown while signed out, starts again too.
					sessionStartMillis = System.currentTimeMillis();
					updatePanel();
				}
				if (auth.getState() != AuthState.SIGNED_IN || !consented())
				{
					return;
				}
				String hash = accountHash == -1 ? null : Long.toString(accountHash);
				String id = all || Progress.CURRENT.equals(scope) ? null : scope;
				api.resetSession(Progress.CURRENT.equals(scope) ? hash : null, id, all,
					result -> clientThread.invokeLater(() -> onProgressReply(result)),
					message -> clientThread.invokeLater(() -> showProgress(progress, "Couldn't reset: " + message)));
			});
		}

		@Override
		public void setGoal(String kind, long target, String deadline, boolean allCharacters, boolean adapt)
		{
			usage.add("panel.set_goal");
			clientThread.invokeLater(() ->
			{
				if (auth.getState() != AuthState.SIGNED_IN || !consented())
				{
					showProgress(progress, "Sign in to set a goal.");
					return;
				}
				GoalRequest body = new GoalRequest();
				body.kind = kind;
				body.target = target;
				body.deadline = deadline;
				body.scope = allCharacters ? "all" : "character";
				body.accountHash = accountHash == -1 ? null : Long.toString(accountHash);
				body.adapt = adapt;
				api.setGoal(body,
					result -> clientThread.invokeLater(() -> onProgressReply(result)),
					message -> clientThread.invokeLater(() -> showProgress(progress, message)));
			});
		}

		@Override
		public void clearGoal()
		{
			usage.add("panel.clear_goal");
			clientThread.invokeLater(() -> api.deleteGoal(
				() -> clientThread.invokeLater(() -> refreshProgress(null)),
				message -> clientThread.invokeLater(() -> showProgress(progress, message))));
		}

		@Override
		public void dismissPlan()
		{
			usage.add("panel.hide_plan");
			clientThread.invokeLater(suggestions::dismissPlan);
		}

		@Override
		public void rate(int rating)
		{
			usage.add(rating > 0 ? "panel.rate_good" : "panel.rate_bad");
			clientThread.invokeLater(() -> suggestions.rate(rating));
		}

		@Override
		public void useAlert(com.gpgenius.api.DumpAlert alert)
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
		public void dismissAnnouncement(String announcementId)
		{
			usage.add("panel.close_announcement");
			api.dismissAnnouncement(announcementId);
		}

		@Override
		public void openAnnouncementLink(String url)
		{
			if (com.gpgenius.api.Announcements.opensOn(url, auth.server()))
			{
				usage.add("panel.announcement_link");
				browse(url);
			}
		}

		@Override
		public void chartPeriod(String period)
		{
			usage.add("panel.chart_" + period);
			clientThread.invokeLater(() -> chart.setPeriod(period));
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
