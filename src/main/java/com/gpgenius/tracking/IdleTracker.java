package com.gpgenius.tracking;

/**
 * Idle GE slot time and unused cash while the player is logged in, for the GP/hour card. A slot is
 * idle while it is empty or holds a finished offer nobody collected; it is working while an offer is
 * buying or selling. Unused cash is the coins carried that no buy offer is using.
 *
 * <p>Worked out here from what the client shows live, because the server only sees finished trades.
 * Plain Java, so it can be tested without a client.
 */
public final class IdleTracker
{
	public static final int MAX_SLOTS = 8;

	private long startedAt = -1;
	private int slotsAvailable = MAX_SLOTS;
	private final boolean[] busy = new boolean[MAX_SLOTS];
	private final long[] idleSince = new long[MAX_SLOTS];
	private final long[] idleTotal = new long[MAX_SLOTS];
	private long coinsCarried;
	private long coinsInBuyOffers;

	/** Starts counting again from now, e.g. at login. Every slot starts idle until an offer shows up. */
	public synchronized void start(long now, int slotsAvailable)
	{
		startedAt = now;
		this.slotsAvailable = Math.max(1, Math.min(MAX_SLOTS, slotsAvailable));
		for (int i = 0; i < MAX_SLOTS; i++)
		{
			busy[i] = false;
			idleSince[i] = now;
			idleTotal[i] = 0;
		}
	}

	public synchronized void stop()
	{
		startedAt = -1;
	}

	public synchronized boolean running()
	{
		return startedAt >= 0;
	}

	/** A slot's offer changed: busy is true while it is buying or selling. */
	public synchronized void slot(int slot, boolean working, long now)
	{
		if (startedAt < 0 || slot < 0 || slot >= MAX_SLOTS || busy[slot] == working)
		{
			return;
		}
		if (working)
		{
			idleTotal[slot] += Math.max(0, now - idleSince[slot]);
		}
		else
		{
			idleSince[slot] = now;
		}
		busy[slot] = working;
	}

	public synchronized void cash(long carried, long inBuyOffers)
	{
		coinsCarried = Math.max(0, carried);
		coinsInBuyOffers = Math.max(0, inBuyOffers);
	}

	/** How long the slot has been idle since counting started. */
	public synchronized long idleMillis(int slot, long now)
	{
		if (startedAt < 0 || slot < 0 || slot >= slotsAvailable)
		{
			return 0;
		}
		return idleTotal[slot] + (busy[slot] ? 0 : Math.max(0, now - idleSince[slot]));
	}

	/** Share of slot time spent idle since counting started, 0 to 1. */
	public synchronized double idleShare(long now)
	{
		long elapsed = elapsed(now);
		if (elapsed <= 0)
		{
			return 0;
		}
		long idle = 0;
		for (int i = 0; i < slotsAvailable; i++)
		{
			idle += idleMillis(i, now);
		}
		return Math.min(1.0, idle / (double) (elapsed * slotsAvailable));
	}

	public synchronized long elapsed(long now)
	{
		return startedAt < 0 ? 0 : Math.max(0, now - startedAt);
	}

	public synchronized int slotsAvailable()
	{
		return slotsAvailable;
	}

	/** Coins carried that no buy offer is using. */
	public synchronized long unusedCash()
	{
		return coinsCarried;
	}

	/** Unused coins as a share of all coins (carried plus in buy offers), 0 to 1. */
	public synchronized double unusedShare()
	{
		long total = coinsCarried + coinsInBuyOffers;
		return total <= 0 ? 0 : coinsCarried / (double) total;
	}

	/** "1h 05m", "12m", "45s". */
	public static String duration(long millis)
	{
		long seconds = Math.max(0, millis / 1000);
		if (seconds < 60)
		{
			return seconds + "s";
		}
		long minutes = seconds / 60;
		if (minutes < 60)
		{
			return minutes + "m";
		}
		return String.format("%dh %02dm", minutes / 60, minutes % 60);
	}
}
