package com.gpgenius.api;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Whether the GP Genius server is answering, from the plugin's own requests. When it isn't, the panel
 * says so plainly (since when, what to check, that nothing is lost) and automatic requests back off
 * so a down server isn't asked every few seconds by every player.
 *
 * <p>Thread-safe: requests finish on OkHttp's threads, the panel reads it on the client thread.</p>
 */
public class ServerHealth
{
	/** No HTTP status: the request never got an answer (no internet, DNS, timeout, refused). */
	public static final int NO_ANSWER = 0;
	static final long FIRST_BACKOFF_MILLIS = 5_000;
	static final long MAX_BACKOFF_MILLIS = 120_000;
	private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm");

	private final ZoneId zone;
	private long downSince;
	private long lastFailure;
	private int failures;
	private int lastCode;

	public ServerHealth()
	{
		this(ZoneId.systemDefault());
	}

	ServerHealth(ZoneId zone)
	{
		this.zone = zone;
	}

	/** Statuses that mean "the server or the proxy in front of it is down", not "the request was wrong". */
	public static boolean isOutage(int code)
	{
		return code == NO_ANSWER || code == 502 || code == 503 || code == 504;
	}

	/** Any answer that isn't an outage: the server is up again. */
	public synchronized void answered(int code, long now)
	{
		if (isOutage(code))
		{
			failed(code, now);
			return;
		}
		downSince = 0;
		failures = 0;
		lastCode = 0;
	}

	public synchronized void failed(int code, long now)
	{
		if (failures == 0)
		{
			downSince = now;
		}
		failures++;
		lastFailure = now;
		lastCode = code;
	}

	/** True after two failed requests in a row, so one dropped packet doesn't put up a banner. */
	public synchronized boolean isDown()
	{
		return failures >= 2;
	}

	/** How long automatic requests wait after the last failure: 5s, 10s, 20s ... up to 2 minutes. */
	public synchronized long backoffMillis()
	{
		if (failures == 0)
		{
			return 0;
		}
		int doublings = Math.min(failures - 1, 10);
		return Math.min(MAX_BACKOFF_MILLIS, FIRST_BACKOFF_MILLIS << doublings);
	}

	/** Whether an automatic request may go now. A player's own button press always goes. */
	public synchronized boolean mayTry(long now)
	{
		return failures == 0 || now - lastFailure >= backoffMillis();
	}

	/** The message for the panel while the server is down, or null when it's fine. */
	public synchronized String notice(String server)
	{
		if (!isDown())
		{
			return null;
		}
		String since = CLOCK.format(Instant.ofEpochMilli(downSince).atZone(zone));
		String status = server == null ? "" : " Status: " + server + "/status.";
		String kept = " Your trades are saved on this computer and upload when it's back; nothing is lost.";
		if (lastCode == NO_ANSWER)
		{
			return "Can't reach the GP Genius server since " + since + ". Check your internet connection;"
				+ " if that's fine, the server may be down." + status + kept;
		}
		return "The GP Genius server is down or restarting (since " + since + "). It usually comes back within"
			+ " a few minutes, and GP Genius keeps trying." + status + kept;
	}
}
