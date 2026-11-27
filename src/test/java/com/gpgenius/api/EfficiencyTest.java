package com.gpgenius.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.gson.Gson;
import org.junit.Test;

public class EfficiencyTest
{
	@Test
	public void readsTheServersReply()
	{
		String json = "{\"prefs\":{\"overall\":{\"enabled\":false,\"days\":0},\"session\":{\"enabled\":true},"
			+ "\"day\":{\"enabled\":true,\"hours\":6},\"slots\":{\"enabled\":true,\"hours\":24}},"
			+ "\"overall\":null,"
			+ "\"session\":{\"label\":\"This session\",\"profit\":120000,\"flips\":3,\"hours\":2.5,\"since\":null,\"per_hour\":48000},"
			+ "\"day\":{\"label\":\"Last 6h\",\"profit\":50000,\"flips\":1,\"hours\":0.1,\"since\":null,\"per_hour\":null},"
			+ "\"slots\":[{\"slot\":0,\"label\":\"Slot 1\",\"profit\":9000,\"flips\":1,\"hours\":24,\"since\":null,\"per_hour\":375}],"
			+ "\"slots_label\":\"By slot, last 24h\"}";
		Efficiency e = new Gson().fromJson(json, Efficiency.class);
		assertNull(e.overall);
		assertEquals(2, e.rates().size());
		assertEquals(Long.valueOf(48000), e.session.perHour);
		assertNull(e.day.perHour);
		assertEquals(375L, (long) e.slots.get(0).perHour);
		assertFalse(e.enabled("overall"));
		assertTrue(e.enabled("session"));
		assertEquals(6, e.window("day", "hours", 24));
		assertEquals(24, e.window("missing", "hours", 24));
	}
}
