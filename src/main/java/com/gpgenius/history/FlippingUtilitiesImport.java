package com.gpgenius.history;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.gpgenius.tracking.Transaction;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Reads trade history saved by the Flipping Utilities plugin, from {@code .runelite/flipping/<name>.json}.
 *
 * <p>The format belongs to another plugin and can change without notice, so everything is parsed
 * leniently and anything that doesn't look like a trade is skipped. What we rely on, from that plugin's
 * source: the file holds {@code trades}, a list of items ({@code id}, {@code name}), each with a history
 * {@code h} whose {@code sO} list holds offer snapshots: {@code b} buy, {@code cQIT} quantity filled so
 * far in that offer, {@code p} average price per item, {@code t} time, {@code s} slot (-1 for trades
 * added by hand), {@code st} offer state and {@code vSQ} false once the player reset their stats.</p>
 *
 * <p>Snapshots of one offer are cumulative, so each becomes a trade of the quantity filled since the
 * one before. Imported trades get IDs made from their content, so importing twice adds nothing.</p>
 */
public final class FlippingUtilitiesImport
{
	public static final String DIRECTORY = "flipping";
	/** Files bigger than this aren't read; years of heavy flipping are a few megabytes. */
	static final long MAX_FILE_BYTES = 64L * 1024 * 1024;
	static final int MAX_TRADES = 50_000;

	private FlippingUtilitiesImport()
	{
	}

	/** What was read, and how much was skipped. */
	public static class Result
	{
		public final List<Transaction> trades = new ArrayList<>();
		public int skipped;
	}

	/** The history file for a character, or null if the name can't be a file name. */
	public static File fileFor(File runeliteDir, String displayName)
	{
		if (displayName == null || displayName.isEmpty() || displayName.contains("/") || displayName.contains("\\")
			|| displayName.contains(".."))
		{
			return null;
		}
		return new File(new File(runeliteDir, DIRECTORY), displayName + ".json");
	}

	public static Result read(File file) throws IOException
	{
		if (file.length() > MAX_FILE_BYTES)
		{
			throw new IOException("The Flipping Utilities file is too big to import.");
		}
		try (Reader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8))
		{
			return parse(reader);
		}
	}

	@SuppressWarnings("deprecation")
	public static Result parse(Reader reader) throws IOException
	{
		JsonElement root;
		try
		{
			// The instance method works on every Gson version RuneLite has shipped.
			root = new JsonParser().parse(reader);
		}
		catch (RuntimeException e)
		{
			throw new IOException("The Flipping Utilities file isn't valid JSON.", e);
		}
		Result result = new Result();
		JsonArray items = array(root.isJsonObject() ? root.getAsJsonObject().get("trades") : null);
		if (items == null)
		{
			throw new IOException("No trades found in the Flipping Utilities file.");
		}
		for (JsonElement element : items)
		{
			try
			{
				readItem(element.getAsJsonObject(), result);
			}
			catch (RuntimeException e)
			{
				result.skipped++;
			}
			if (result.trades.size() >= MAX_TRADES)
			{
				break;
			}
		}
		result.trades.sort(Comparator.comparingLong(t -> t.timestampMillis));
		return result;
	}

	private static void readItem(JsonObject item, Result result)
	{
		int itemId = integer(item.get("id"), 0);
		String name = string(item.get("name"));
		JsonObject history = item.has("h") && item.get("h").isJsonObject() ? item.getAsJsonObject("h") : null;
		JsonArray events = array(history == null ? null : history.get("sO"));
		if (events == null)
		{
			return;
		}

		List<Event> offers = new ArrayList<>();
		for (JsonElement e : events)
		{
			Event event = Event.from(e, itemId);
			if (event == null)
			{
				result.skipped++;
			}
			else if (event.valid)
			{
				offers.add(event);
			}
		}
		offers.sort(Comparator.comparingLong(e -> e.timeMillis));

		// The last snapshot of the offer in each slot, for working out what each new snapshot added.
		Map<String, Event> last = new HashMap<>();
		for (Event e : offers)
		{
			int baseQuantity = 0;
			long baseCoins = 0;
			if (e.slot >= 0)
			{
				String key = e.slot + (e.buy ? "b" : "s") + e.itemId;
				Event before = last.get(key);
				if (before != null && !before.terminal() && e.quantity >= before.quantity)
				{
					baseQuantity = before.quantity;
					baseCoins = before.coins();
				}
				last.put(key, e);
			}
			int quantity = e.quantity - baseQuantity;
			long coins = e.coins() - baseCoins;
			if (quantity <= 0 || coins < 0 || e.itemId <= 0)
			{
				continue;
			}
			Transaction t = new Transaction(e.itemId, name, e.buy, quantity, coins, e.timeMillis, Math.max(-1, e.slot));
			t.imported = true;
			t.clientId = UUID.nameUUIDFromBytes(("flipping-utilities:" + e.itemId + ":" + e.buy + ":" + e.slot + ":"
				+ e.timeMillis + ":" + e.quantity + ":" + e.price).getBytes(StandardCharsets.UTF_8)).toString();
			result.trades.add(t);
			if (result.trades.size() >= MAX_TRADES)
			{
				return;
			}
		}
	}

	/** One saved offer snapshot. */
	private static final class Event
	{
		int itemId;
		boolean buy;
		int quantity;
		int price;
		long timeMillis;
		int slot;
		String state;
		boolean valid;

		static Event from(JsonElement element, int itemId)
		{
			if (element == null || !element.isJsonObject())
			{
				return null;
			}
			JsonObject o = element.getAsJsonObject();
			Event e = new Event();
			e.itemId = integer(o.get("id"), itemId);
			e.buy = bool(o.get("b"), false);
			e.quantity = integer(o.get("cQIT"), 0);
			e.price = integer(o.get("p"), 0);
			e.timeMillis = time(o.get("t"));
			e.slot = integer(o.get("s"), -1);
			e.state = string(o.get("st"));
			e.valid = bool(o.get("vSQ"), true);
			if (e.timeMillis <= 0 || e.price < 0)
			{
				return null;
			}
			return e;
		}

		long coins()
		{
			return (long) quantity * price;
		}

		boolean terminal()
		{
			return "BOUGHT".equals(state) || "SOLD".equals(state) || "CANCELLED_BUY".equals(state)
				|| "CANCELLED_SELL".equals(state);
		}
	}

	/** Instants as Gson writes them by reflection ({seconds, nanos}), epoch seconds or millis, or ISO-8601. */
	static long time(JsonElement element)
	{
		try
		{
			if (element == null || element.isJsonNull())
			{
				return 0;
			}
			if (element.isJsonObject())
			{
				JsonObject o = element.getAsJsonObject();
				long seconds = o.has("seconds") ? o.get("seconds").getAsLong() : 0;
				long nanos = o.has("nanos") ? o.get("nanos").getAsLong() : 0;
				return seconds * 1000 + nanos / 1_000_000;
			}
			if (element.getAsJsonPrimitive().isNumber())
			{
				long value = element.getAsLong();
				return value > 100_000_000_000L ? value : value * 1000;
			}
			return Instant.parse(element.getAsString()).toEpochMilli();
		}
		catch (RuntimeException e)
		{
			return 0;
		}
	}

	private static JsonArray array(JsonElement element)
	{
		return element != null && element.isJsonArray() ? element.getAsJsonArray() : null;
	}

	private static int integer(JsonElement element, int fallback)
	{
		try
		{
			return element == null || element.isJsonNull() ? fallback : element.getAsInt();
		}
		catch (RuntimeException e)
		{
			return fallback;
		}
	}

	private static boolean bool(JsonElement element, boolean fallback)
	{
		try
		{
			return element == null || element.isJsonNull() ? fallback : element.getAsBoolean();
		}
		catch (RuntimeException e)
		{
			return fallback;
		}
	}

	private static String string(JsonElement element)
	{
		try
		{
			return element == null || element.isJsonNull() ? "" : element.getAsString();
		}
		catch (RuntimeException e)
		{
			return "";
		}
	}
}
