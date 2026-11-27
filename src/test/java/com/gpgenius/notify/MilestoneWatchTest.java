package com.gpgenius.notify;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.gpgenius.api.Progress;
import org.junit.Test;

public class MilestoneWatchTest
{
	private static Progress.Milestones m(String key, String label)
	{
		Progress.Milestones m = new Progress.Milestones();
		m.profitBadge = key;
		m.profitBadgeLabel = label;
		return m;
	}

	@Test
	public void announcesOnlyNewBadgesAfterTheFirstAnswer()
	{
		MilestoneWatch w = new MilestoneWatch();
		assertNull(w.update(m("profit-1000000", "1M profit")));
		assertNull(w.update(m("profit-1000000", "1M profit")));
		assertEquals("5M profit", w.update(m("profit-5000000", "5M profit")));
		assertNull(w.update(null));
		assertNull(w.update(m("profit-5000000", "5M profit")));
		w.reset();
		assertNull(w.update(m("profit-10000000", "10M profit")));
	}

	@Test
	public void firstBadgeAfterNoneIsAnnounced()
	{
		MilestoneWatch w = new MilestoneWatch();
		assertNull(w.update(m(null, null)));
		assertEquals("1M profit", w.update(m("profit-1000000", "1M profit")));
	}

	@Test
	public void panelLine()
	{
		Progress.Milestones m = m("profit-100000000", "100M profit");
		m.nextProfitBadgeLabel = "250M profit";
		m.nextProfitFraction = 0.134;
		m.currentStreak = 4;
		assertEquals("★ 100M profit · next 250M profit (13%) · 4-day streak", m.line());
		Progress.Milestones none = m(null, null);
		none.nextProfitBadgeLabel = "1M profit";
		none.currentStreak = 1;
		assertEquals("Next: 1M profit (0%)", none.line());
	}
}
