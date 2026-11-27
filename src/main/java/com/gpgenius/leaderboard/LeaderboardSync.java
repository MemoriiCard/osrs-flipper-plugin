package com.gpgenius.leaderboard;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.gpgenius.GpGeniusConfig;
import com.gpgenius.auth.AuthSession;
import com.gpgenius.auth.AuthState;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Keeps the "Leaderboard name" setting in step with the website, per character (see SharingState).
 * The plugin registers it on the event bus while it runs. Failures are quiet: the setting is a
 * convenience, and the website's /leaderboards/sharing page always works.
 */
@Slf4j
@Singleton
public class LeaderboardSync
{
	public static final String KEY = "leaderboardName";
	// The plugin says hello (which tells the server about the character) shortly after login.
	private static final long RETRY_SECONDS = 20;

	private final Client client;
	private final ConfigManager configManager;
	private final AuthSession auth;
	private final Gson gson;
	private final ScheduledExecutorService executor;
	private final SharingState state = new SharingState();

	@Inject
	public LeaderboardSync(Client client, ConfigManager configManager, AuthSession auth, Gson gson,
		ScheduledExecutorService executor)
	{
		this.client = client;
		this.configManager = configManager;
		this.auth = auth;
		this.gson = gson;
		this.executor = executor;
	}

	/** Call on the client thread when the plugin starts, in case a character is already logged in. */
	public void start()
	{
		if (client.getGameState() == GameState.LOGGED_IN)
		{
			loggedIn(client.getAccountHash());
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGGED_IN)
		{
			loggedIn(client.getAccountHash());
		}
		else if (event.getGameState() == GameState.LOGIN_SCREEN)
		{
			synchronized (state)
			{
				state.loggedOut();
			}
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!GpGeniusConfig.GROUP.equals(event.getGroup()) || !KEY.equals(event.getKey()))
		{
			return;
		}
		LeaderboardName value = current();
		long hash;
		synchronized (state)
		{
			if (state.settingChanged(value) != SharingState.Action.PUSH)
			{
				return;
			}
			hash = state.character();
		}
		push(hash, value);
	}

	private void loggedIn(long hash)
	{
		SharingState.Action action;
		synchronized (state)
		{
			action = state.loggedIn(hash);
		}
		if (action == SharingState.Action.PUSH)
		{
			push(hash, current());
		}
		else if (action == SharingState.Action.FETCH)
		{
			fetch(hash, true);
		}
	}

	private LeaderboardName current()
	{
		LeaderboardName value = configManager.getConfiguration(GpGeniusConfig.GROUP, KEY, LeaderboardName.class);
		return value == null ? LeaderboardName.ANONYMOUS : value;
	}

	private void fetch(long hash, boolean retry)
	{
		if (auth.getState() != AuthState.SIGNED_IN)
		{
			return;
		}
		auth.send("/v1/leaderboards/sharing?account_hash=" + hash, null, response ->
		{
			Sharing reply = read(response);
			if (reply == null)
			{
				if (retry && response.code() == 404)
				{
					executor.schedule(() -> fetch(hash, false), RETRY_SECONDS, TimeUnit.SECONDS);
				}
				return;
			}
			LeaderboardName value = LeaderboardName.fromWire(reply.visibility);
			boolean update;
			synchronized (state)
			{
				update = state.fromServer(hash, value, current());
			}
			if (update)
			{
				configManager.setConfiguration(GpGeniusConfig.GROUP, KEY, value);
			}
		}, message -> log.debug("Leaderboard setting not read: {}", message));
	}

	private void push(long hash, LeaderboardName value)
	{
		if (hash == -1 || auth.getState() != AuthState.SIGNED_IN)
		{
			return;
		}
		JsonObject body = new JsonObject();
		body.addProperty("account_hash", Long.toString(hash));
		body.addProperty("visibility", value.wire);
		auth.send("/v1/leaderboards/sharing", gson.toJson(body), response ->
		{
			if (read(response) == null)
			{
				log.debug("Leaderboard setting not saved: HTTP {}", response.code());
			}
		}, message -> log.debug("Leaderboard setting not saved: {}", message));
	}

	private Sharing read(Response response)
	{
		try (ResponseBody body = response.body())
		{
			if (!response.isSuccessful() || body == null)
			{
				return null;
			}
			return gson.fromJson(body.charStream(), Sharing.class);
		}
		catch (RuntimeException e)
		{
			log.debug("Bad leaderboard reply", e);
			return null;
		}
	}

	/** Reply from /v1/leaderboards/sharing. */
	static class Sharing
	{
		String visibility;
		@com.google.gson.annotations.SerializedName("shown_as")
		String shownAs;
	}
}
