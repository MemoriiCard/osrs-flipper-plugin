package com.gpgenius.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.google.gson.Gson;
import java.time.ZoneOffset;
import org.junit.Test;

public class SlotPlanTest
{
	private static final String JSON = "{"
		+ "\"steps\":["
		+ "{\"slot\":0,\"action\":\"collect\",\"item_id\":554,\"name\":\"Fire rune\",\"is_buy\":true,\"price\":5,\"quantity\":1000,\"order\":1},"
		+ "{\"slot\":1,\"action\":\"relist\",\"item_id\":4151,\"name\":\"Abyssal whip\",\"is_buy\":true,\"price\":1560001,\"quantity\":6,\"order\":2},"
		+ "{\"slot\":2,\"action\":\"cut\",\"item_id\":11832,\"name\":\"Bandos chestplate\",\"is_buy\":false,\"price\":19000000,\"quantity\":1,\"loss\":true,\"order\":3},"
		+ "{\"slot\":0,\"action\":\"sell\",\"item_id\":554,\"name\":\"Fire rune\",\"is_buy\":false,\"price\":6,\"quantity\":1000,\"order\":4}],"
		+ "\"slots\":["
		+ "{\"slot\":0,\"action\":\"sell\",\"item_id\":554,\"name\":\"Fire rune\",\"is_buy\":false},"
		+ "{\"slot\":1,\"action\":\"relist\",\"item_id\":4151,\"name\":\"Abyssal whip\",\"is_buy\":true},"
		+ "{\"slot\":2,\"action\":\"keep\",\"item_id\":11832,\"name\":\"Bandos chestplate\",\"is_buy\":false},"
		+ "{\"slot\":3,\"action\":\"reserved\"},{\"slot\":4,\"action\":\"empty\"}],"
		+ "\"holds\":[],\"total_slots\":8,\"coins_used\":0,\"coins_left\":0,\"expected_profit\":0,"
		+ "\"next_check_in_at\":\"2026-11-22T14:35:00Z\"}";

	@Test
	public void readsTheServersPlanAndWritesTheTodoList()
	{
		SlotPlan plan = new Gson().fromJson(JSON, SlotPlan.class);
		assertEquals(8, plan.totalSlots);
		assertEquals("Collect Fire rune (slot 1)", plan.steps.get(0).todo());
		assertEquals("Abort and relist Abyssal whip buy at 1,560,001 x 6", plan.steps.get(1).todo());
		assertEquals("Abort and sell Bandos chestplate at 19,000,000 x 1", plan.steps.get(2).todo());
		assertEquals("Sell Fire rune at 6 x 1,000", plan.steps.get(3).todo());
		assertEquals("Selling Fire rune", plan.slots.get(0).status());
		assertEquals("Selling Bandos chestplate: leave it", plan.slots.get(2).status());
		assertEquals("Kept free for you", plan.slots.get(3).status());
		assertEquals("Free", plan.slots.get(4).status());
		assertEquals("14:35", plan.nextCheckIn(ZoneOffset.UTC));
	}

	@Test
	public void missingOrBadCheckInTimeIsNull()
	{
		SlotPlan plan = new SlotPlan();
		assertNull(plan.nextCheckIn(ZoneOffset.UTC));
		plan.nextCheckInAt = "soon";
		assertNull(plan.nextCheckIn(ZoneOffset.UTC));
	}

	@Test
	public void cutBuyJustStopsIt()
	{
		SlotPlan.Step s = new SlotPlan.Step();
		s.action = SlotPlan.CUT;
		s.isBuy = true;
		s.slot = 5;
		s.name = "Fire rune";
		assertEquals("Abort Fire rune buy (slot 6)", s.todo());
	}
}
