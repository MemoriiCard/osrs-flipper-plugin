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
		assertEquals(10_000_000L, GeTax.total(2_147_000_000L, 2));
	}
}
