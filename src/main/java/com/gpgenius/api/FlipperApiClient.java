package com.gpgenius.api;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.gpgenius.auth.AuthSession;
import com.gpgenius.market.MarketPrice;
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
 * Talks to the GP Genius server as the signed-in player. Requests run on OkHttp's threads, and
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

	/** Pro: a short plain-language "why this item?" from the AI market analyst. */
	public void explain(int itemId, Consumer<String> onSuccess, Consumer<String> onError)
	{
		JsonObject body = new JsonObject();
		body.addProperty("item_id", itemId);
		post("/v1/explain", body, Explanation.class, e -> onSuccess.accept(e == null ? null : e.answer), onError);
	}

	/** The plan for every GE slot. Same body as a suggestion request. */
	public void slots(SuggestionRequest body, Consumer<SlotPlan> onSuccess, Consumer<String> onError)
	{
		post("/v1/slots", body, SlotPlan.class, onSuccess, onError);
	}

	/** The player's own trades over the last {@code days}, with totals and chart series. */
	public void history(int days, Long beforeId, Consumer<TradeHistory> onSuccess, Consumer<String> onError)
	{
		String path = "/v1/history?days=" + Math.max(1, Math.min(3650, days))
			+ (beforeId == null ? "" : "&before_id=" + beforeId.longValue());
		get(path, TradeHistory.class, onSuccess, onError);
	}

	/** The player's own trades over the last {@code days} as CSV text, tax and gross and net profit included. */
	public void exportTrades(int days, Consumer<String> onSuccess, Consumer<String> onError)
	{
		get("/v1/history/export.csv?days=" + Math.max(1, Math.min(3650, days)), String.class, onSuccess, onError);
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

	/** Closes an announcement for this player on the website and in every plugin they use. */
	public void dismissAnnouncement(String announcementId)
	{
		if (announcementId == null || !announcementId.matches("[0-9a-fA-F-]{36}"))
		{
			return;
		}
		post("/v1/announcements/" + announcementId + "/dismiss", new JsonObject(), null, ignored -> { }, this::logOnly);
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

	/** @param period day, week or month */
	public void priceHistory(int itemId, String period, Consumer<PriceHistory> onSuccess, Consumer<String> onError)
	{
		get("/v1/prices/" + itemId + "/history?period=" + period, PriceHistory.class, onSuccess, onError);
	}

	/** Saved profiles, which one is active, and the fast-start presets. */
	public void profiles(Consumer<Profiles.Listing> onSuccess, Consumer<String> onError)
	{
		get("/v1/profiles", Profiles.Listing.class, onSuccess, onError);
	}

	public void createProfile(Profiles.Create body, Consumer<Profiles.Profile> onSuccess, Consumer<String> onError)
	{
		post("/v1/profiles", body, Profiles.Profile.class, onSuccess, onError);
	}

	public void updateProfile(String id, Profiles.Update body, Consumer<Profiles.Profile> onSuccess, Consumer<String> onError)
	{
		post("/v1/profiles/" + id, body, Profiles.Profile.class, onSuccess, onError);
	}

	public void activateProfile(String id, Consumer<Profiles.Profile> onSuccess, Consumer<String> onError)
	{
		post("/v1/profiles/" + id + "/activate", new JsonObject(), Profiles.Profile.class, onSuccess, onError);
	}

	public void deleteProfile(String id, Runnable onSuccess, Consumer<String> onError)
	{
		post("/v1/profiles/" + id + "/delete", new JsonObject(), null, ignored -> onSuccess.run(), onError);
	}

	/** Profit at a glance, the goal and the suggested stack. accountHash may be null. */
	/** The player's own history with one item, for the "You: ..." line under the suggestion. */
	public void itemHistory(int itemId, Consumer<ItemHistory> onSuccess, Consumer<String> onError)
	{
		get("/v1/items/" + itemId + "/history", ItemHistory.class, onSuccess, onError);
	}

	public void progress(String accountHash, Consumer<Progress> onSuccess, Consumer<String> onError)
	{
		String path = "/v1/progress";
		if (accountHash != null && accountHash.matches("-?\\d{1,20}"))
		{
			path += "?account_hash=" + accountHash;
		}
		get(path, Progress.class, onSuccess, onError);
	}

	/** Starts a new profit session for this character, or every character when all is true. */
	public void resetSession(String accountHash, String accountId, boolean all, Consumer<Progress> onSuccess, Consumer<String> onError)
	{
		JsonObject body = new JsonObject();
		if (accountHash != null)
		{
			body.addProperty("account_hash", accountHash);
		}
		if (accountId != null && accountId.matches("[0-9a-fA-F-]{36}"))
		{
			body.addProperty("account_id", accountId);
		}
		body.addProperty("all", all);
		post("/v1/session/reset", body, Progress.class, onSuccess, onError);
	}

	public void setGoal(GoalRequest body, Consumer<Progress> onSuccess, Consumer<String> onError)
	{
		post("/v1/goal", body, Progress.class, onSuccess, onError);
	}

	public void deleteGoal(Runnable onSuccess, Consumer<String> onError)
	{
		post("/v1/goal/delete", new JsonObject(), null, ignored -> onSuccess.run(), onError);
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
			if (type == String.class)
			{
				onSuccess.accept(type.cast(body.string()));
				return;
			}
			onSuccess.accept(gson.fromJson(body.charStream(), type));
		}
		catch (java.io.IOException e)
		{
			log.debug("Couldn't read the response", e);
			onError.accept("The download was cut off. Try again.");
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

	/** Reply from /v1/explain. */
	public static class Explanation
	{
		public String answer;
	}

	/** Reply from /v1/alerts. */
	public static class Alerts
	{
		@com.google.gson.annotations.SerializedName("premium_required")
		public boolean premiumRequired;
		public List<DumpAlert> alerts;
	}
}
