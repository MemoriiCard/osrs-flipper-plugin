package com.osrsflipper.tracking;

import com.google.gson.Gson;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * Loads and saves each account's trades as a JSON file named after RuneLite's account hash.
 */
public class TradeStore
{
	private final Gson gson;
	private final File directory;

	public TradeStore(Gson gson, File directory)
	{
		this.gson = gson;
		this.directory = directory;
	}

	public AccountTrades load(long accountHash) throws IOException
	{
		File file = fileFor(accountHash);
		if (!file.exists())
		{
			return new AccountTrades();
		}
		try (Reader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8))
		{
			AccountTrades trades = gson.fromJson(reader, AccountTrades.class);
			return trades == null ? new AccountTrades() : trades;
		}
	}

	public void save(long accountHash, AccountTrades trades) throws IOException
	{
		saveJson(accountHash, gson.toJson(trades));
	}

	/**
	 * Writes already-serialized trades, so callers can serialize on one thread and write on another.
	 */
	public void saveJson(long accountHash, String json) throws IOException
	{
		if (!directory.exists() && !directory.mkdirs())
		{
			throw new IOException("Could not create " + directory);
		}
		File file = fileFor(accountHash);
		File tmp = new File(directory, file.getName() + ".tmp");
		try (Writer writer = Files.newBufferedWriter(tmp.toPath(), StandardCharsets.UTF_8))
		{
			writer.write(json);
		}
		Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
	}

	private File fileFor(long accountHash)
	{
		return new File(directory, "trades-" + accountHash + ".json");
	}
}
