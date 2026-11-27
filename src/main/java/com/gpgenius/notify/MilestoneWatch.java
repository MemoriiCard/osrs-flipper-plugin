package com.gpgenius.notify;

import com.gpgenius.api.Progress;

/**
 * Notices when a progress refresh brings a new profit badge, so the plugin can say so once. The first
 * answer after starting only sets the baseline, so restarting the client never re-announces an old one.
 * Kept free of RuneLite types so it can be unit tested.
 */
public class MilestoneWatch
{
	private boolean seen;
	private String lastBadge;

	/** @return the new badge's label, or null when nothing new was earned */
	public String update(Progress.Milestones m)
	{
		if (m == null)
		{
			return null;
		}
		String badge = m.profitBadge;
		boolean fresh = seen && badge != null && !badge.equals(lastBadge);
		seen = true;
		lastBadge = badge;
		return fresh ? m.profitBadgeLabel : null;
	}

	public void reset()
	{
		seen = false;
		lastBadge = null;
	}
}
