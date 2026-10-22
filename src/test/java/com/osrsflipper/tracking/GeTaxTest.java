package com.osrsflipper.tracking;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class GeTaxTest
{
	@Test
	public void roundsDown()
	{
		assertEquals(0, GeTax.perItem(49));
		assertEquals(1, GeTax.perItem(50));
		assertEquals(1, GeTax.perItem(99));
		assertEquals(20, GeTax.perItem(1000));
	}

	@Test
	public void capsAtFiveMillionPerItem()
	{
		assertEquals(5_000_000L, GeTax.perItem(2_147_000_000L));
		assertEquals(10_000_000L, GeTax.total(4151, 2_147_000_000L, 2));
	}

	@Test
	public void bondsAndToolsAreExempt()
	{
		assertEquals(0, GeTax.perItem(13190, 9_000_000L));
		assertEquals(0, GeTax.total(2347, 1000, 5));
		assertEquals(20, GeTax.perItem(4151, 1000));
	}
}
