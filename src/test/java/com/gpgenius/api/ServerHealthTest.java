package com.gpgenius.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.time.ZoneOffset;
import org.junit.Test;

public class ServerHealthTest
{
	private static final long T0 = 1_700_000_000_000L; // 22:13 UTC

	@Test
	public void oneBlipIsNotAnOutage()
	{
		ServerHealth h = new ServerHealth(ZoneOffset.UTC);
		h.failed(ServerHealth.NO_ANSWER, T0);
		assertFalse(h.isDown());
		assertNull(h.notice("https://gpgenius.gg"));
		h.answered(200, T0 + 1000);
		h.failed(ServerHealth.NO_ANSWER, T0 + 2000);
		assertFalse(h.isDown());
	}

	@Test
	public void noInternetAndServerDownSayDifferentThings()
	{
		ServerHealth h = new ServerHealth(ZoneOffset.UTC);
		h.failed(ServerHealth.NO_ANSWER, T0);
		h.failed(ServerHealth.NO_ANSWER, T0 + 5000);
		String offline = h.notice("https://gpgenius.gg");
		assertTrue(offline, offline.contains("since 22:13") && offline.contains("internet"));
		assertTrue(offline.contains("https://gpgenius.gg/status") && offline.contains("nothing is lost"));

		ServerHealth s = new ServerHealth(ZoneOffset.UTC);
		s.answered(502, T0);
		s.answered(503, T0 + 1);
		assertTrue(s.isDown());
		assertTrue(s.notice(null).contains("down or restarting"));
		assertFalse(s.notice(null).contains("/status"));
	}

	@Test
	public void anyRealAnswerEndsTheOutage()
	{
		ServerHealth h = new ServerHealth(ZoneOffset.UTC);
		h.failed(504, T0);
		h.failed(504, T0 + 1);
		h.answered(429, T0 + 2);
		assertFalse(h.isDown());
		assertTrue(h.mayTry(T0 + 3));
		assertEquals(0, h.backoffMillis());
	}

	@Test
	public void automaticRequestsBackOffUpToTwoMinutes()
	{
		ServerHealth h = new ServerHealth(ZoneOffset.UTC);
		assertTrue(h.mayTry(T0));
		long now = T0;
		long[] expected = {5_000, 10_000, 20_000, 40_000, 80_000, 120_000, 120_000};
		for (long wait : expected)
		{
			h.failed(ServerHealth.NO_ANSWER, now);
			assertEquals(wait, h.backoffMillis());
			assertFalse(h.mayTry(now + wait - 1));
			assertTrue(h.mayTry(now + wait));
			now += wait;
		}
		for (int i = 0; i < 100; i++)
		{
			h.failed(ServerHealth.NO_ANSWER, now);
		}
		assertEquals(ServerHealth.MAX_BACKOFF_MILLIS, h.backoffMillis());
	}

	@Test
	public void outageCodes()
	{
		assertTrue(ServerHealth.isOutage(0) && ServerHealth.isOutage(502) && ServerHealth.isOutage(503));
		assertFalse(ServerHealth.isOutage(500) || ServerHealth.isOutage(401) || ServerHealth.isOutage(429));
	}
}
