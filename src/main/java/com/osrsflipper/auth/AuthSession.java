package com.osrsflipper.auth;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.osrsflipper.OsrsFlipperPlugin;
import com.osrsflipper.api.DeviceStart;
import com.osrsflipper.api.Tokens;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Signs the plugin in to the OSRS Flipper website and keeps a valid access token.
 *
 * <p>Sign-in is a device flow: the server hands out a short code, the player approves it on the
 * website (already signed in there), and the plugin collects a 15-minute access token and a
 * refresh token. Refresh tokens rotate on every use and the server ends the session if an old one
 * is replayed, so refreshes here are strictly one at a time.</p>
 */
@Slf4j
@Singleton
public class AuthSession
{
	private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
	/** Refresh a little before the server's expiry so requests don't race it. */
	private static final long EXPIRY_MARGIN_MILLIS = 60_000;

	private final OkHttpClient http;
	private final Gson gson;
	private final ScheduledExecutorService executor;

	private TokenFile tokenFile;
	private Supplier<String> baseUrl = () -> null;
	private Runnable onChange = () -> { };

	private AuthState state = AuthState.SIGNED_OUT;
	private DeviceStart linking;
	private ScheduledFuture<?> poll;
	private String accessToken;
	private long accessExpiresAt;
	private String refreshToken;
	/** The server that issued the tokens. They are only ever sent there. */
	private String tokenServer;
	private boolean refreshing;
	private final List<Waiter> waiting = new ArrayList<>();

	private static class Waiter
	{
		final Consumer<String> then;
		final Consumer<String> fail;

		Waiter(Consumer<String> then, Consumer<String> fail)
		{
			this.then = then;
			this.fail = fail;
		}
	}

	@Inject
	public AuthSession(OkHttpClient http, Gson gson, ScheduledExecutorService executor)
	{
		// Every request says which plugin version sent it, so the server can ask outdated copies to update.
		this.http = http.newBuilder()
			.addInterceptor(chain -> chain.proceed(chain.request().newBuilder()
				.header("X-Plugin-Version", OsrsFlipperPlugin.VERSION)
				.build()))
			.build();
		this.gson = gson;
		this.executor = executor;
	}

	public synchronized void start(TokenFile file, Supplier<String> baseUrl, Runnable onChange)
	{
		this.tokenFile = file;
		this.baseUrl = baseUrl;
		this.onChange = onChange;
		TokenFile.Stored stored = file.load();
		refreshToken = stored == null ? null : stored.refreshToken;
		tokenServer = stored == null ? null : stored.server;
		state = refreshToken == null ? AuthState.SIGNED_OUT : AuthState.SIGNED_IN;
	}

	public synchronized void stop()
	{
		cancelPoll();
		linking = null;
		onChange = () -> { };
		accessToken = null;
		if (state == AuthState.LINKING)
		{
			state = AuthState.SIGNED_OUT;
		}
	}

	public synchronized AuthState getState()
	{
		// After the server address changes the saved sign-in belongs to another server, so it reads
		// as signed out until the player signs in again.
		if (state == AuthState.SIGNED_IN && !boundTo(server()))
		{
			return AuthState.SIGNED_OUT;
		}
		return state;
	}

	/** Holds the lock. */
	private boolean boundTo(String server)
	{
		return server != null && server.equals(tokenServer);
	}

	/** The code to approve while linking, or null. */
	public synchronized DeviceStart getLinking()
	{
		return linking;
	}

	/** The server address, or null if the configured one isn't allowed. */
	public String server()
	{
		return ServerUrl.normalize(baseUrl.get());
	}

	/** Asks the server for a sign-in code. onCode gets the page to open in the browser. */
	public void beginLink(Consumer<String> onCode, Consumer<String> onError)
	{
		String server = server();
		if (server == null)
		{
			onError.accept("The server address in settings must start with https://.");
			return;
		}
		Request request = new Request.Builder()
			.url(server + "/v1/auth/device")
			.post(RequestBody.create(JSON, "{}"))
			.build();
		http.newCall(request).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				onError.accept("Can't reach the OSRS Flipper server.");
			}

			@Override
			public void onResponse(Call call, Response response)
			{
				DeviceStart start;
				try (ResponseBody body = response.body())
				{
					if (!response.isSuccessful() || body == null)
					{
						onError.accept(errorMessage(response.code(), body));
						return;
					}
					start = gson.fromJson(body.charStream(), DeviceStart.class);
				}
				catch (RuntimeException e)
				{
					onError.accept("The server sent a reply this version can't read.");
					return;
				}
				if (start == null || start.deviceCode == null || start.verificationUriComplete == null)
				{
					onError.accept("The server sent a reply this version can't read.");
					return;
				}
				synchronized (AuthSession.this)
				{
					cancelPoll();
					linking = start;
					state = AuthState.LINKING;
					long interval = Math.max(2, start.interval);
					long deadline = System.currentTimeMillis() + start.expiresIn * 1000L;
					poll = executor.scheduleWithFixedDelay(() -> pollLink(start.deviceCode, deadline),
						interval, interval, TimeUnit.SECONDS);
				}
				onChange.run();
				onCode.accept(start.verificationUriComplete);
			}
		});
	}

	public void cancelLink()
	{
		synchronized (this)
		{
			cancelPoll();
			linking = null;
			if (state == AuthState.LINKING)
			{
				state = refreshToken == null ? AuthState.SIGNED_OUT : AuthState.SIGNED_IN;
			}
		}
		onChange.run();
	}

	private void pollLink(String deviceCode, long deadline)
	{
		synchronized (this)
		{
			if (state != AuthState.LINKING || linking == null || !deviceCode.equals(linking.deviceCode))
			{
				cancelPoll();
				return;
			}
		}
		if (System.currentTimeMillis() > deadline)
		{
			cancelLink();
			return;
		}
		String server = server();
		if (server == null)
		{
			cancelLink();
			return;
		}
		JsonObject json = new JsonObject();
		json.addProperty("device_code", deviceCode);
		Request request = new Request.Builder()
			.url(server + "/v1/auth/device/token")
			.post(RequestBody.create(JSON, json.toString()))
			.build();
		// Polling runs on the executor already, and waits are short, so a blocking call is fine here.
		try (Response response = http.newCall(request).execute(); ResponseBody body = response.body())
		{
			if (response.code() == 428 || response.code() == 429)
			{
				return;
			}
			if (!response.isSuccessful() || body == null)
			{
				cancelLink();
				return;
			}
			Tokens tokens = gson.fromJson(body.charStream(), Tokens.class);
			synchronized (this)
			{
				cancelPoll();
				linking = null;
				accept(tokens, server);
			}
			onChange.run();
		}
		catch (IOException | RuntimeException e)
		{
			log.debug("Sign-in poll failed", e);
		}
	}

	public void signOut()
	{
		String token;
		String server = server();
		synchronized (this)
		{
			token = boundTo(server) ? accessToken : null;
			clearTokens();
		}
		if (token != null && server != null)
		{
			Request request = new Request.Builder()
				.url(server + "/v1/auth/logout")
				.header("Authorization", "Bearer " + token)
				.post(RequestBody.create(JSON, "{}"))
				.build();
			http.newCall(request).enqueue(new Callback()
			{
				@Override
				public void onFailure(Call call, IOException e)
				{
				}

				@Override
				public void onResponse(Call call, Response response)
				{
					response.close();
				}
			});
		}
		onChange.run();
	}

	/**
	 * Sends a request to the server with a valid access token, refreshing it first if needed and
	 * once more if the server says it has expired.
	 *
	 * @param path    such as "/v1/suggestion"
	 * @param body    JSON body, or null for GET
	 * @param handler gets the response on an OkHttp thread; it must close it
	 */
	public void send(String path, String body, Consumer<Response> handler, Consumer<String> onError)
	{
		String server = server();
		if (server == null)
		{
			onError.accept("The server address in settings must start with https://.");
			return;
		}
		synchronized (this)
		{
			if (!boundTo(server))
			{
				onError.accept("Sign in to OSRS Flipper first.");
				return;
			}
		}
		withToken(null, token -> enqueue(server, path, body, token, true, handler, onError), onError);
	}

	private void enqueue(String server, String path, String body, String token, boolean retry,
		Consumer<Response> handler, Consumer<String> onError)
	{
		Request.Builder builder = new Request.Builder()
			.url(server + path)
			.header("Authorization", "Bearer " + token);
		if (body != null)
		{
			builder.post(RequestBody.create(JSON, body));
		}
		http.newCall(builder.build()).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				log.debug("Request to {} failed", path, e);
				onError.accept("Can't reach the OSRS Flipper server.");
			}

			@Override
			public void onResponse(Call call, Response response)
			{
				if (response.code() == 401 && retry)
				{
					response.close();
					withToken(token, fresh -> enqueue(server, path, body, fresh, false, handler, onError), onError);
					return;
				}
				handler.accept(response);
			}
		});
	}

	/**
	 * Runs then with a usable access token. With stale set, that token was just rejected, so a
	 * different one is needed.
	 */
	private void withToken(String stale, Consumer<String> then, Consumer<String> fail)
	{
		String ready = null;
		boolean startRefresh = false;
		synchronized (this)
		{
			if (refreshToken == null)
			{
				fail.accept("Sign in to OSRS Flipper first.");
				return;
			}
			boolean fresh = accessToken != null && System.currentTimeMillis() < accessExpiresAt;
			if (fresh && (stale == null || !stale.equals(accessToken)))
			{
				ready = accessToken;
			}
			else
			{
				waiting.add(new Waiter(then, fail));
				if (!refreshing)
				{
					refreshing = true;
					startRefresh = true;
				}
			}
		}
		if (ready != null)
		{
			then.accept(ready);
		}
		else if (startRefresh)
		{
			refresh();
		}
	}

	private void refresh()
	{
		String server = server();
		String token;
		synchronized (this)
		{
			token = boundTo(server) ? refreshToken : null;
		}
		if (server == null || token == null)
		{
			finishRefresh(null, "Sign in to OSRS Flipper first.");
			return;
		}
		JsonObject json = new JsonObject();
		json.addProperty("refresh_token", token);
		Request request = new Request.Builder()
			.url(server + "/v1/auth/refresh")
			.post(RequestBody.create(JSON, json.toString()))
			.build();
		http.newCall(request).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				finishRefresh(null, "Can't reach the OSRS Flipper server.");
			}

			@Override
			public void onResponse(Call call, Response response)
			{
				try (ResponseBody body = response.body())
				{
					if (response.code() == 401)
					{
						synchronized (AuthSession.this)
						{
							clearTokens();
						}
						onChange.run();
						finishRefresh(null, "You were signed out. Sign in again from the side panel.");
						return;
					}
					if (!response.isSuccessful() || body == null)
					{
						finishRefresh(null, errorMessage(response.code(), body));
						return;
					}
					Tokens tokens = gson.fromJson(body.charStream(), Tokens.class);
					synchronized (AuthSession.this)
					{
						accept(tokens, server);
					}
					finishRefresh(tokens.accessToken, null);
				}
				catch (RuntimeException e)
				{
					finishRefresh(null, "The server sent a reply this version can't read.");
				}
			}
		});
	}

	private void finishRefresh(String token, String error)
	{
		List<Waiter> done;
		synchronized (this)
		{
			refreshing = false;
			done = new ArrayList<>(waiting);
			waiting.clear();
		}
		for (Waiter w : done)
		{
			if (token != null)
			{
				w.then.accept(token);
			}
			else
			{
				w.fail.accept(error);
			}
		}
	}

	/** Holds the lock. */
	private void accept(Tokens tokens, String server)
	{
		if (tokens == null || tokens.accessToken == null || tokens.refreshToken == null)
		{
			throw new JsonParseException("missing tokens");
		}
		accessToken = tokens.accessToken;
		accessExpiresAt = System.currentTimeMillis() + Math.max(0, tokens.expiresIn * 1000L - EXPIRY_MARGIN_MILLIS);
		refreshToken = tokens.refreshToken;
		tokenServer = server;
		state = AuthState.SIGNED_IN;
		final String toSave = refreshToken;
		final TokenFile file = tokenFile;
		executor.execute(() ->
		{
			try
			{
				file.save(toSave, server);
			}
			catch (IOException e)
			{
				log.warn("Could not save sign-in", e);
			}
		});
	}

	/** Holds the lock. */
	private void clearTokens()
	{
		cancelPoll();
		linking = null;
		accessToken = null;
		accessExpiresAt = 0;
		refreshToken = null;
		tokenServer = null;
		state = AuthState.SIGNED_OUT;
		if (tokenFile != null)
		{
			final TokenFile file = tokenFile;
			executor.execute(file::clear);
		}
	}

	private void cancelPoll()
	{
		if (poll != null)
		{
			poll.cancel(false);
			poll = null;
		}
	}

	/** The server's "detail" message when it sent one, else a generic line with the status code. */
	public String errorMessage(int code, ResponseBody body)
	{
		if (body != null)
		{
			try
			{
				JsonObject json = gson.fromJson(body.charStream(), JsonObject.class);
				if (json != null && json.has("detail") && json.get("detail").isJsonPrimitive())
				{
					String detail = json.get("detail").getAsString();
					if (!detail.isEmpty() && detail.length() <= 300)
					{
						return detail;
					}
				}
			}
			catch (RuntimeException e)
			{
				// Fall through to the generic message.
			}
		}
		if (code == 429)
		{
			return "Too many requests. Wait a minute and try again.";
		}
		if (code == 426)
		{
			return "This version of OSRS Flipper is out of date. Update it and restart RuneLite.";
		}
		return "The server returned an error (" + code + ").";
	}
}
