package com.gpgenius.suggestion;

import com.gpgenius.api.Suggestion;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Whether to ask the server for a "welcome back" plan, and the plan it sent.
 *
 * <p>A plan starts when the player logs in or comes back to the GE after at least one check-in
 * window away, and lasts while it still has more than one step. It is a list to read: the player
 * still sets up and confirms every offer, one highlighted step at a time. Kept free of RuneLite
 * types so it can be unit tested.</p>
 */
public class PlanState
{
	private boolean active;
	private boolean dismissed;
	private Suggestion.Plan plan;
	private long lastAtGeMillis;

	/** The player just logged in. */
	public void start()
	{
		active = true;
		dismissed = false;
		plan = null;
	}

	/**
	 * Call every tick with whether the GE is open.
	 *
	 * @return true when this starts a plan
	 */
	public boolean atGe(boolean open, long nowMillis, long windowMillis)
	{
		if (!open)
		{
			return false;
		}
		boolean away = lastAtGeMillis > 0 && nowMillis - lastAtGeMillis >= windowMillis;
		lastAtGeMillis = nowMillis;
		if (away && !active)
		{
			start();
			return true;
		}
		return false;
	}

	/** Whether the next request should ask for a plan. */
	public boolean wanted()
	{
		return active && !dismissed;
	}

	/** Buys planned after the current step, so the next request keeps them while they're still good. */
	public List<Integer> plannedItemIds()
	{
		List<Integer> ids = new ArrayList<>();
		if (plan == null || plan.steps == null)
		{
			return ids;
		}
		for (Suggestion step : plan.steps)
		{
			if (step != null && step.is(Suggestion.BUY) && step.itemId != null && !ids.contains(step.itemId))
			{
				ids.add(step.itemId);
			}
		}
		return ids;
	}

	/** Takes the server's answer. A plan of one step or none ends the plan. */
	public void onResult(Suggestion.Plan result)
	{
		if (!wanted())
		{
			return;
		}
		if (result == null || result.steps == null || result.steps.size() < 2)
		{
			active = false;
			plan = null;
			return;
		}
		plan = result;
	}

	/** The player hid the plan; back to one step at a time until the next time they come back. */
	public void dismiss()
	{
		dismissed = true;
		active = false;
		plan = null;
	}

	public void reset()
	{
		active = false;
		dismissed = false;
		plan = null;
		lastAtGeMillis = 0;
	}

	/** The plan to show, or null. */
	public Suggestion.Plan getPlan()
	{
		return wanted() ? plan : null;
	}

	public List<Suggestion> steps()
	{
		Suggestion.Plan shown = getPlan();
		return shown == null || shown.steps == null ? Collections.emptyList() : shown.steps;
	}
}
