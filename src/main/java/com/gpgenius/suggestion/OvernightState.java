package com.gpgenius.suggestion;

/**
 * Overnight mode: the player is setting up the GE before logging off. While it's on, suggestions and
 * the slot plan ask the server for slower, safer picks sized to the time away. The server decides
 * what those are; this only remembers that the player asked, and for how long.
 *
 * <p>It switches itself off after {@link #SETUP_MILLIS}, so a player who forgets about it gets normal
 * suggestions again when they come back. Kept free of RuneLite types so it can be unit tested.</p>
 */
public class OvernightState
{
	public static final int MIN_HOURS = 4;
	public static final int MAX_HOURS = 14;
	public static final int DEFAULT_HOURS = 8;
	/** Hours offered in the panel. */
	public static final int[] CHOICES = {6, 8, 10, 12};
	/** Long enough to place every offer, short enough that it's off by the time the player is back. */
	static final long SETUP_MILLIS = 90 * 60_000L;

	private boolean on;
	private int hours = DEFAULT_HOURS;
	private long startedMillis;

	public static int clamp(int hours)
	{
		return Math.max(MIN_HOURS, Math.min(MAX_HOURS, hours));
	}

	public void start(int hours, long nowMillis)
	{
		this.on = true;
		this.hours = clamp(hours);
		this.startedMillis = nowMillis;
	}

	public void stop()
	{
		on = false;
	}

	/** Whether overnight mode is on now. Turns itself off once the setup time has passed. */
	public boolean active(long nowMillis)
	{
		if (on && nowMillis - startedMillis >= SETUP_MILLIS)
		{
			on = false;
		}
		return on;
	}

	public int getHours()
	{
		return hours;
	}
}
