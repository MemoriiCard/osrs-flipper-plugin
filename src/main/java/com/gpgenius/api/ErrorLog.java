package com.gpgenius.api;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import javax.inject.Singleton;

/**
 * The plugin's last few errors (failed requests, files it couldn't save), kept in memory and sent to the
 * server with the regular check-in so support sees what went wrong before the player explains it. The same
 * error is counted, not repeated. Messages are scrubbed of anything personal before they're kept, and
 * nothing is sent while "Send error reports" is off.
 */
@Singleton
public class ErrorLog
{
	public static final int MAX = 10;

	private static final Pattern EMAIL = Pattern.compile("[\\w.+-]+@[\\w-]+\\.[\\w.-]+");
	private static final Pattern IPV4 = Pattern.compile("\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b");
	private static final Pattern WIN_USER = Pattern.compile("(?i)([a-z]:\\\\users\\\\)[^\\\\/\\s]+");
	private static final Pattern NIX_USER = Pattern.compile("(/(?:home|Users)/)[^/\\s]+");
	private static final Pattern SECRET = Pattern.compile("(?i)(bearer|token|code|key|secret|password)([=: ]+)\\S+");
	private static final Pattern LONG_TOKEN = Pattern.compile("\\b[A-Za-z0-9_\\-]{24,}\\b");
	private static final Pattern IDS = Pattern.compile("/[0-9a-fA-F-]{8,}|/\\d+");

	/** One kind of error since the last report. */
	public static final class Entry
	{
		public final String kind;
		public final String message;
		public int count;
		public long at;

		Entry(String kind, String message, long at)
		{
			this.kind = kind;
			this.message = message;
			this.count = 1;
			this.at = at;
		}
	}

	private final Map<String, Entry> entries = new LinkedHashMap<>();

	/** Remembers an error. Safe from any thread. */
	public synchronized void record(String kind, String message)
	{
		record(kind, message, System.currentTimeMillis());
	}

	synchronized void record(String kind, String message, long now)
	{
		String k = kind(kind);
		String m = scrub(message);
		String key = k + "\n" + m;
		Entry e = entries.get(key);
		if (e != null)
		{
			e.count++;
			e.at = now;
			return;
		}
		if (entries.size() >= MAX)
		{
			// Drop the oldest so the newest problems are the ones that get through.
			String oldest = entries.keySet().iterator().next();
			entries.remove(oldest);
		}
		entries.put(key, new Entry(k, m, now));
	}

	public synchronized boolean isEmpty()
	{
		return entries.isEmpty();
	}

	/** Everything since the last report, and forgets it. Put it back with {@link #restore} if sending fails. */
	public synchronized List<Entry> drain()
	{
		List<Entry> out = new ArrayList<>(entries.values());
		entries.clear();
		return out;
	}

	public synchronized void restore(List<Entry> unsent)
	{
		for (Entry e : unsent)
		{
			String key = e.kind + "\n" + e.message;
			if (!entries.containsKey(key) && entries.size() < MAX)
			{
				entries.put(key, e);
			}
		}
	}

	/** "HTTP_500:/v1/suggestions/1234/feedback" -> "http_500:/v1/suggestions/{id}/feedback". */
	public static String kind(String kind)
	{
		String k = kind == null ? "" : kind.trim().toLowerCase();
		int query = k.indexOf('?');
		if (query >= 0)
		{
			k = k.substring(0, query);
		}
		k = IDS.matcher(k).replaceAll("/{id}");
		k = k.replaceAll("[^a-z0-9_:/{}.\\-]", "");
		if (k.isEmpty())
		{
			return "unknown";
		}
		return k.length() > 64 ? k.substring(0, 64) : k;
	}

	/** Removes emails, IP addresses, user names in file paths and anything that looks like a code or token. */
	public static String scrub(String message)
	{
		if (message == null)
		{
			return "";
		}
		String m = message.replaceAll("\\s+", " ").trim();
		m = EMAIL.matcher(m).replaceAll("<email>");
		m = IPV4.matcher(m).replaceAll("<ip>");
		m = WIN_USER.matcher(m).replaceAll("$1<user>");
		m = NIX_USER.matcher(m).replaceAll("$1<user>");
		m = SECRET.matcher(m).replaceAll("$1$2<hidden>");
		m = LONG_TOKEN.matcher(m).replaceAll("<hidden>");
		return m.length() > 300 ? m.substring(0, 300) : m;
	}
}
