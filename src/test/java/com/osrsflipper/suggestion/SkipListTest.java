package com.osrsflipper.suggestion;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import org.junit.Test;

public class SkipListTest
{
	@Test
	public void skipsExpire()
	{
		SkipList skips = new SkipList();
		skips.skip(4151, 0);
		assertEquals(Collections.singletonList(4151), skips.active(SkipList.SKIP_MILLIS - 1));
		assertTrue(skips.active(SkipList.SKIP_MILLIS).isEmpty());
	}

	@Test
	public void oldestSkipDroppedWhenFull()
	{
		SkipList skips = new SkipList();
		for (int i = 1; i <= SkipList.MAX_SKIPS + 1; i++)
		{
			skips.skip(i, i);
		}
		assertEquals(SkipList.MAX_SKIPS, skips.active(SkipList.MAX_SKIPS + 1).size());
		assertTrue(!skips.active(SkipList.MAX_SKIPS + 1).contains(1));
	}
}
