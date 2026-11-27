package com.gpgenius.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Test;

public class ErrorLogTest
{
	@Test
	public void scrubRemovesAnythingPersonal()
	{
		String m = ErrorLog.scrub("Couldn't write C:\\Users\\NickSmith\\.runelite\\gp-genius\\trades.json "
			+ "for nick@example.com via 203.0.113.7 token=abc123 refresh eyJhbGciOiJIUzI1NiJ9abcdefghijkl at 12:30:45");
		assertFalse(m.contains("NickSmith"));
		assertFalse(m.contains("nick@example.com"));
		assertFalse(m.contains("203.0.113.7"));
		assertFalse(m.contains("abc123"));
		assertFalse(m.contains("eyJhbGci"));
		assertTrue(m.contains("C:\\Users\\<user>"));
		assertTrue(m.contains("12:30:45"));
		assertTrue(ErrorLog.scrub("/home/nick/.runelite/x").startsWith("/home/<user>/"));
		assertTrue(ErrorLog.scrub(new String(new char[500]).replace('\0', 'a')).length() <= 300);
	}

	@Test
	public void kindsGroupTheSameRequest()
	{
		assertEquals("http_500:/v1/suggestions/{id}/feedback",
			ErrorLog.kind("HTTP_500:/v1/suggestions/3f2a9c1e-1111-2222-3333-444455556666/feedback?x=1"));
		assertEquals("unknown", ErrorLog.kind(null));
	}

	@Test
	public void repeatsAreCountedAndOnlyTheNewestAreKept()
	{
		ErrorLog log = new ErrorLog();
		log.record("trade_save", "disk full", 1);
		log.record("trade_save", "disk full", 2);
		for (int i = 0; i < ErrorLog.MAX + 3; i++)
		{
			log.record("k" + i, "", 10 + i);
		}
		List<ErrorLog.Entry> sent = log.drain();
		assertEquals(ErrorLog.MAX, sent.size());
		assertEquals("k3", sent.get(0).kind);
		assertTrue(log.isEmpty());

		log.record("trade_save", "disk full", 3);
		log.record("trade_save", "disk full", 4);
		List<ErrorLog.Entry> again = log.drain();
		assertEquals(2, again.get(0).count);

		// A report that couldn't be sent goes back for next time.
		log.restore(again);
		assertFalse(log.isEmpty());
	}

	@Test
	public void onlyRealProblemsAreReported()
	{
		assertTrue(FlipperApiClient.worthReporting(500));
		assertTrue(FlipperApiClient.worthReporting(422));
		assertFalse(FlipperApiClient.worthReporting(401));
		assertFalse(FlipperApiClient.worthReporting(404));
		assertFalse(FlipperApiClient.worthReporting(429));
	}
}
