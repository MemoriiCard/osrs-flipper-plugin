package com.gpgenius.history;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.gpgenius.api.ItemHistory;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.junit.Test;

public class ItemHistoryControllerTest
{
	private final List<Integer> asked = new ArrayList<>();
	private final List<Consumer<ItemHistory>> pending = new ArrayList<>();
	private int changes;
	private final ItemHistoryController c = new ItemHistoryController((id, done) ->
	{
		asked.add(id);
		pending.add(done);
	});

	{
		c.setOnChange(() -> changes++);
	}

	private static ItemHistory line(String text)
	{
		ItemHistory h = new ItemHistory();
		h.line = text;
		return h;
	}

	@Test
	public void fetchesOncePerItemAndRemembersIt()
	{
		c.tick(4151, 0);
		c.tick(4151, 1_000);
		assertEquals(1, asked.size());
		assertNull(c.line());
		pending.get(0).accept(line("You: 2 flips"));
		assertEquals("You: 2 flips", c.line());

		c.tick(554, 2_000);
		assertNull(c.line());
		c.tick(4151, 3_000);
		// Back to the whip: shown from memory, no new request.
		assertEquals("You: 2 flips", c.line());
		assertEquals(2, asked.size());

		c.tick(4151, ItemHistoryController.REFRESH_MILLIS + 1);
		assertEquals(3, asked.size());
	}

	@Test
	public void failuresWaitAndNothingShowsWithoutAnItem()
	{
		c.tick(4151, 0);
		pending.get(0).accept(null);
		assertNull(c.line());
		c.tick(4151, 1_000);
		assertEquals(1, asked.size());
		c.tick(null, 2_000);
		assertNull(c.line());
		int before = changes;
		c.tick(null, 3_000);
		assertEquals(before, changes);
	}
}
