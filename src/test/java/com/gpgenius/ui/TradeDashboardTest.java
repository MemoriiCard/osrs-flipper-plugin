package com.gpgenius.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.gson.Gson;
import com.gpgenius.api.TradeHistory;
import com.gpgenius.api.TradeHistoryTest;
import java.io.File;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.Test;

public class TradeDashboardTest
{
	private static TradeHistory history()
	{
		return new Gson().fromJson(TradeHistoryTest.JSON, TradeHistory.class);
	}

	@Test
	public void chartSeriesComeStraightFromTheTrades()
	{
		TradeHistory h = history();
		List<TradeCharts.Point> running = TradeDashboard.running(h, ZoneOffset.UTC);
		assertEquals(2, running.size());
		assertEquals("22 Nov 11:00", running.get(1).label);
		assertEquals(67882, running.get(1).value);
		List<TradeCharts.Point> split = TradeDashboard.split(h.summary);
		assertEquals(99900, split.get(0).value);
		assertEquals(-32018, split.get(1).value);
		assertEquals(67882, split.get(2).value);
		assertEquals(1, TradeDashboard.buckets(h.distribution, true).get(0).value);
		assertEquals("Abyssal whip", TradeDashboard.items(h.topItems).get(0).label);
		assertTrue(TradeDashboard.split(new TradeHistory.Summary()).isEmpty());
	}

	@Test
	public void tableCellsKeepValuesForSorting()
	{
		TradeHistory.Trade t = history().trades.get(0);
		assertEquals(Instant.parse("2026-11-22T10:00:00Z"), TradeDashboard.cell(t, 0));
		assertEquals(60L, TradeDashboard.cell(t, 2));
		assertEquals("Abyssal whip", TradeDashboard.cell(t, 4));
		assertEquals(32000L, TradeDashboard.cell(t, 10));
		assertEquals(100000L, TradeDashboard.cell(t, 11));
		assertEquals(68000L, TradeDashboard.cell(t, 12));
		assertEquals(TradeDashboard.COLUMNS.length, 13);
	}

	@Test
	public void statusLineExplainsWhatIsShown()
	{
		TradeHistory h = history();
		assertTrue(TradeDashboard.statusLine(1, h).startsWith("Showing the latest 1 of 2"));
		assertTrue(TradeDashboard.statusLine(2, h).startsWith("2 trades."));
		assertTrue(TradeDashboard.statusLine(0, new TradeHistory()).startsWith("No trades"));
	}

	@Test
	public void ticksCoverTheRangeAndIncludeZero()
	{
		List<Long> ticks = TradeCharts.ticks(-120, 900, 4);
		assertTrue(ticks.contains(0L));
		assertTrue(ticks.get(0) <= -120);
		assertTrue(ticks.get(ticks.size() - 1) >= 900);
		assertEquals("-25K", TradeCharts.shortGp(-25_000));
	}

	@Test
	public void exportFileNames()
	{
		assertEquals("gp-genius-trades-2026-11-22.csv", TradeDashboardFiles.defaultName(LocalDate.of(2026, 11, 22)));
		assertEquals("a.csv", TradeDashboardFiles.withCsv(new File("a")).getName());
		assertEquals("b.CSV", TradeDashboardFiles.withCsv(new File("b.CSV")).getName());
		assertEquals(2, TradeDashboardFiles.rows("h\nx\ny\n"));
		assertEquals(0, TradeDashboardFiles.rows("h\n"));
	}
}
