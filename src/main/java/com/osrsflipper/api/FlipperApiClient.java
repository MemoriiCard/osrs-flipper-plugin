package com.osrsflipper.api;

import com.google.gson.Gson;
import java.io.IOException;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Talks to the OSRS Flipper server. Requests run on OkHttp's threads; callbacks are too, so callers
 * must hop back to the client or Swing thread themselves.
 */
@Slf4j
@Singleton
public class FlipperApiClient
{
	private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

	private final OkHttpClient http;
	private final Gson gson;

	@Inject
	public FlipperApiClient(OkHttpClient http, Gson gson)
	{
		this.http = http;
		this.gson = gson;
	}

	public void suggest(String baseUrl, SuggestionRequest body, Consumer<Suggestion> onSuccess, Consumer<String> onError)
	{
		HttpUrl url = HttpUrl.parse(baseUrl);
		if (url == null)
		{
			onError.accept("The server URL in settings isn't valid.");
			return;
		}
		Request request = new Request.Builder()
			.url(url.newBuilder().addPathSegments("v1/suggestion").build())
			.post(RequestBody.create(JSON, gson.toJson(body)))
			.build();
		http.newCall(request).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				log.debug("Suggestion request failed", e);
				onError.accept("Can't reach the OSRS Flipper server.");
			}

			@Override
			public void onResponse(Call call, Response response)
			{
				try (ResponseBody responseBody = response.body())
				{
					if (!response.isSuccessful() || responseBody == null)
					{
						log.debug("Suggestion request returned {}", response.code());
						onError.accept("The server returned an error (" + response.code() + ").");
						return;
					}
					Suggestion suggestion = gson.fromJson(responseBody.charStream(), Suggestion.class);
					if (suggestion == null || suggestion.action == null)
					{
						onError.accept("The server sent an empty suggestion.");
						return;
					}
					onSuccess.accept(suggestion);
				}
				catch (RuntimeException e)
				{
					log.debug("Bad suggestion response", e);
					onError.accept("The server sent a suggestion this version can't read.");
				}
			}
		});
	}
}
