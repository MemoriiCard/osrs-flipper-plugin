package com.gpgenius.suggestion;

import com.gpgenius.api.SlotPlan;

/**
 * When to ask the server for a new slot plan, and the last one it sent. The plan is only fetched
 * alongside a suggestion request (so only when something changed, or on the slow refresh), and never
 * more often than MIN_GAP_MILLIS. Kept free of RuneLite types so it can be unit tested.
 */
public class SlotPlanState
{
	static final long MIN_GAP_MILLIS = 10_000;

	private SlotPlan plan;
	private long lastRequestMillis;
	private boolean inFlight;

	/** Whether a slot plan request should go out with this suggestion request. */
	public boolean due(long nowMillis)
	{
		return !inFlight && (lastRequestMillis == 0 || nowMillis - lastRequestMillis >= MIN_GAP_MILLIS);
	}

	public void sent(long nowMillis)
	{
		inFlight = true;
		lastRequestMillis = nowMillis;
	}

	public void received(SlotPlan plan)
	{
		inFlight = false;
		if (plan != null)
		{
			this.plan = plan;
		}
	}

	public void failed()
	{
		inFlight = false;
	}

	public SlotPlan getPlan()
	{
		return plan;
	}

	public void reset()
	{
		plan = null;
		inFlight = false;
		lastRequestMillis = 0;
	}
}
