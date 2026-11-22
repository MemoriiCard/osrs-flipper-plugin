package com.gpgenius.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.gson.Gson;
import java.time.Instant;
import org.junit.Test;

public class TradeHistoryTest
{
	public static final String JSON = "{\"since\":\"2026-10-23T12:00:00Z\",\"until\":\"2026-11-22T12:00:00Z\","
		+ "\"summary\":{\"trades\":2,\"wins\":1,\"losses\":1,\"quantity\":2,\"cost\":1501000,\"revenue\":1600900,"
		+ "\"tax\":32018,\"gross_profit\":99900,\"net_profit\":67882,\"best\":68000,\"worst\":-118,"
		+ "\"avg_hold_minutes\":60.0,\"win_rate\":0.5,\"return_on_coins\":0.045},"
		+ "\"running\":[{\"at\":\"2026-11-22T10:00:00Z\",\"net_profit\":-118},{\"at\":\"2026-11-22T11:00:00.123456Z\",\"net_profit\":67882}],"
		+ "\"distribution\":[{\"label\":\"Loss under 10k\",\"trades\":1,\"net_profit\":-118},{\"label\":\"10k-100k\",\"trades\":1,\"net_profit\":68000}],"
		+ "\"hold_times\":[{\"label\":\"1-4 h\",\"trades\":2,\"net_profit\":67882}],"
		+ "\"top_items\":[{\"item_id\":4151,\"item\":\"Abyssal whip\",\"trades\":1,\"quantity\":1,\"gross_profit\":100000,\"tax\":32000,\"net_profit\":68000}],"
		+ "\"worst_items\":[],"
		+ "\"trades\":[{\"id\":7,\"account_id\":\"0b6c\",\"account\":\"Main\",\"item_id\":4151,\"item\":\"Abyssal whip\","
		+ "\"quantity\":1,\"buy_price\":1500000,\"sell_price\":1600000,\"opened_at\":\"2026-11-22T10:00:00Z\","
		+ "\"closed_at\":\"2026-11-22T11:00:00Z\",\"hold_minutes\":60,\"cost\":1500000,\"revenue\":1600000,"
		+ "\"tax\":32000,\"gross_profit\":100000,\"net_profit\":68000,\"imported\":false}],"
		+ "\"next_before_id\":null}";

	@Test
	public void readsTheServersReply()
	{
		TradeHistory h = new Gson().fromJson(JSON, TradeHistory.class);
		assertEquals(2, h.summary.trades);
		assertEquals(32018, h.summary.tax);
		assertEquals(99900, h.summary.grossProfit);
		assertEquals(67882, h.summary.netProfit);
		TradeHistory.Trade t = h.trades.get(0);
		assertEquals(100000, t.grossProfit);
		assertEquals(t.grossProfit - t.tax, t.netProfit);
		assertEquals(Instant.parse("2026-11-22T10:00:00Z"), t.opened());
		assertTrue(t.closed().isAfter(t.opened()));
		assertNull(h.nextBeforeId);
		assertEquals(Instant.parse("2026-11-22T11:00:00.123456Z"), TradeHistory.parse(h.running.get(1).at));
		assertNull(TradeHistory.parse("not a time"));
	}

	@Test
	public void holdTimesReadNaturally()
	{
		assertEquals("45 min", TradeHistory.hold(45));
		assertEquals("3h 05m", TradeHistory.hold(185));
		assertEquals("2d 4h", TradeHistory.hold(2 * 1440 + 250));
		assertEquals("0 min", TradeHistory.hold(-3));
	}

	@Test
	public void overnightPlansAreMarked()
	{
		SlotPlan plan = new Gson().fromJson("{\"mode\":\"overnight\",\"steps\":[],\"slots\":[]}", SlotPlan.class);
		assertTrue(plan.isOvernight());
		assertTrue(!new Gson().fromJson("{\"steps\":[]}", SlotPlan.class).isOvernight());
	}
}
