package com.osrsflipper.auth;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * Keeps the refresh token in a file only this computer's user can read. Access tokens are never
 * written to disk, and the token is kept out of RuneLite's config so it never syncs to a profile.
 */
public class TokenFile
{
	private static final String NAME = "auth.json";

	private final Gson gson;
	private final File directory;

	static class Stored
	{
		String refreshToken;
	}

	public TokenFile(Gson gson, File directory)
	{
		this.gson = gson;
		this.directory = directory;
	}

	/** The saved refresh token, or null when signed out or the file can't be read. */
	public String load()
	{
		File file = new File(directory, NAME);
		if (!file.exists())
		{
			return null;
		}
		try (Reader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8))
		{
			Stored stored = gson.fromJson(reader, Stored.class);
			return stored == null || stored.refreshToken == null || stored.refreshToken.isEmpty() ? null : stored.refreshToken;
		}
		catch (IOException | JsonParseException e)
		{
			return null;
		}
	}

	public void save(String refreshToken) throws IOException
	{
		if (!directory.exists() && !directory.mkdirs())
		{
			throw new IOException("Could not create " + directory);
		}
		Stored stored = new Stored();
		stored.refreshToken = refreshToken;
		File tmp = new File(directory, NAME + ".tmp");
		try (Writer writer = Files.newBufferedWriter(tmp.toPath(), StandardCharsets.UTF_8))
		{
			writer.write(gson.toJson(stored));
		}
		ownerOnly(tmp);
		Files.move(tmp.toPath(), new File(directory, NAME).toPath(), StandardCopyOption.REPLACE_EXISTING,
			StandardCopyOption.ATOMIC_MOVE);
	}

	public void clear()
	{
		File file = new File(directory, NAME);
		if (file.exists() && !file.delete())
		{
			file.deleteOnExit();
		}
	}

	private static void ownerOnly(File file)
	{
		// Best effort: works on Linux and macOS, and is ignored where the file system can't express it.
		file.setReadable(false, false);
		file.setReadable(true, true);
		file.setWritable(false, false);
		file.setWritable(true, true);
	}
}
