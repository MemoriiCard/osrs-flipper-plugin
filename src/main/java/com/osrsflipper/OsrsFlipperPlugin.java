package com.osrsflipper;

import com.google.gson.Gson;
import com.google.inject.Provides;
import com.osrsflipper.tracking.AccountTrades;
import com.osrsflipper.tracking.Flip;
import com.osrsflipper.tracking.OfferSnapshot;
import com.osrsflipper.tracking.OfferStatus;
import com.osrsflipper.tracking.TradeStore;
import com.osrsflipper.ui.FlipperPanel;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.GrandExchangeOffer;
import net.runelite.api.GrandExchangeOfferState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GrandExchangeOfferChanged;
import net.runelite.client.RuneLite;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;

@Slf4j
@PluginDescriptor(
	name = "OSRS Flipper",
	description = "Tracks your Grand Exchange flips and profit",
	tags = {"grand exchange", "ge", "flipping", "merching", "profit", "trading"}
)
public class OsrsFlipperPlugin extends Plugin
{
	/** Ticks after login during which RuneLite's EMPTY slot events are not trusted. */
	private static final int LOGIN_BURST_TICKS = 2;

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

	private FlipperPanel panel;
	private NavigationButton navButton;
	private TradeStore store;

	private long accountHash = -1;
	private AccountTrades trades;
	private final List<GrandExchangeOfferChanged> pendingOffers = new ArrayList<>();
	private int loginTick = -1;
	private GameState lastGameState;
	private long sessionStartMillis;

	@Override
	protected void startUp()
	{
		store = new TradeStore(gson, new File(RuneLite.RUNELITE_DIR, "osrs-flipper"));
		panel = new FlipperPanel();
		BufferedImage icon = ImageUtil.loadImageResource(getClass(), "nav_icon.png");
		navButton = NavigationButton.builder()
			.tooltip("OSRS Flipper")
			.icon(icon)
			.priority(7)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navButton);
		sessionStartMillis = System.currentTimeMillis();

		clientThread.invokeLater(() ->
		{
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
		saveAsync();
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
				updatePanel();
			}
		}
		lastGameState = state;
	}

	@Subscribe
	public void onGrandExchangeOfferChanged(GrandExchangeOfferChanged event)
	{
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
			updatePanel();
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
		try
		{
			trades = store.load(hash);
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
