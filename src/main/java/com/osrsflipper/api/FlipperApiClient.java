package com.osrsflipper.api;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.osrsflipper.auth.AuthSession;
import com.osrsflipper.market.MarketPrice;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Talks to the OSRS Flipper server as the signed-in player. Requests run on OkHttp's threads, and
 * so do callbacks, so callers must hop back to the client or Swing thread themselves.
 */
@Slf4j
@Singleton
public class FlipperApiClient
{
	private final AuthSession auth;
	private final Gson gson;

	@Inject
	public FlipperApiClient(AuthSession auth, Gson gson)
	{
		this.auth = auth;
		this.gson = gson;
	}

	public void suggest(SuggestionRequest body, Consumer<Suggestion> onSuccess, Consumer<String> onError)
	{
		post("/v1/suggestion", body, Suggestion.class, s ->
		{
			if (s == null || s.action == null)
			{
				onError.accept("The server sent an empty suggestion.");
				return;
			}
			onSuccess.accept(s);
		}, onError);
	}

	/** Links the game account to the player and returns their plan and announcements. */
	public void hello(String accountHash, String displayName, String version, Consumer<Me> onSuccess, Consumer<String> onError)
	{
		JsonObject body = new JsonObject();
		body.addProperty("account_hash", accountHash);
		body.addProperty("display_name", displayName == null ? "" : displayName);
		body.addProperty("plugin_version", version);
		post("/v1/plugin/hello", body, Me.class, onSuccess, onError);
	}

	public void uploadTrades(TradeUpload body, Consumer<Integer> onSuccess, Consumer<String> onError)
	{
		post("/v1/trades", body, JsonObject.class, json -> onSuccess.accept(
			json != null && json.has("accepted") ? json.get("accepted").getAsInt() : 0), onError);
	}

	/** outcome is "skipped" or "taken", rating is 1 or -1; either may be null. */
	public void feedback(String suggestionId, String outcome, Integer rating)
	{
		if (suggestionId == null || !suggestionId.matches("[0-9a-fA-F-]{36}"))
		{
			return;
		}
		JsonObject body = new JsonObject();
		if (outcome != null)
		{
			body.addProperty("outcome", outcome);
		}
		if (rating != null)
		{
			body.addProperty("rating", rating);
		}
		post("/v1/suggestions/" + suggestionId + "/feedback", body, null, ignored -> { }, this::logOnly);
	}

	public void usage(Map<String, Integer> counts)
	{
		JsonObject body = new JsonObject();
		body.add("counts", gson.toJsonTree(counts));
		post("/v1/usage", body, null, ignored -> { }, this::logOnly);
	}

	public void explain(int itemId, Consumer<Explanation> onSuccess, Consumer<String> onError)
	{
		JsonObject body = new JsonObject();
		body.addProperty("item_id", itemId);
		post("/v1/explain", body, Explanation.class, onSuccess, onError);
	}

	public void alerts(String accountHash, long coins, long minProfit, boolean f2pOnly, boolean members,
		Consumer<Alerts> onSuccess, Consumer<String> onError)
	{
		String path = "/v1/alerts?account_hash=" + accountHash + "&coins=" + Math.max(0, coins)
			+ "&min_profit=" + Math.max(0, minProfit) + "&f2p_only=" + f2pOnly + "&members=" + members;
		get(path, Alerts.class, onSuccess, onError);
	}

	public void latestPrices(Collection<Integer> itemIds, Consumer<List<MarketPrice>> onSuccess, Consumer<String> onError)
	{
		StringBuilder path = new StringBuilder("/v1/prices/latest");
		char sep = '?';
		for (Integer id : itemIds)
		{
			path.append(sep).append("ids=").append(id.intValue());
			sep = '&';
		}
		get(path.toString(), MarketPrice[].class,
			prices -> onSuccess.accept(prices == null ? java.util.Collections.emptyList() : Arrays.asList(prices)), onError);
	}

	private <T> void post(String path, Object body, Class<T> type, Consumer<T> onSuccess, Consumer<String> onError)
	{
		auth.send(path, gson.toJson(body), response -> read(response, type, onSuccess, onError), onError);
	}

	private <T> void get(String path, Class<T> type, Consumer<T> onSuccess, Consumer<String> onError)
	{
		auth.send(path, null, response -> read(response, type, onSuccess, onError), onError);
	}

	private <T> void read(Response response, Class<T> type, Consumer<T> onSuccess, Consumer<String> onError)
	{
		try (ResponseBody body = response.body())
		{
			if (!response.isSuccessful())
			{
				log.debug("{} returned {}", response.request().url().encodedPath(), response.code());
				onError.accept(auth.errorMessage(response.code(), body));
				return;
			}
			if (type == null || body == null || response.code() == 204)
			{
				onSuccess.accept(null);
				return;
			}
			onSuccess.accept(gson.fromJson(body.charStream(), type));
		}
		catch (RuntimeException e)
		{
			log.debug("Bad response", e);
			onError.accept("The server sent a reply this version can't read.");
		}
	}

	private void logOnly(String message)
	{
		log.debug("Background request failed: {}", message);
	}

	/** Reply to "why this item?". */
	public static class Explanation
	{
		public String answer;
		public String source;
		@com.google.gson.annotations.SerializedName("remaining_today")
		public int remainingToday;
	}

	/** Reply from /v1/alerts. */
	public static class Alerts
	{
		@com.google.gson.annotations.SerializedName("premium_required")
		public boolean premiumRequired;
		public List<DumpAlert> alerts;
	}
}
