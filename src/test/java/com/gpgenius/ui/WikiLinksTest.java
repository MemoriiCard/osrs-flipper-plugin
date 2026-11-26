package com.gpgenius.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class WikiLinksTest
{
	@Test
	public void priceChartIsTheWikisItemPage()
	{
		assertEquals("https://prices.runescape.wiki/osrs/item/4151", WikiLinks.priceChartUrl(4151));
		assertNull(WikiLinks.priceChartUrl(0));
		assertNull(WikiLinks.priceChartUrl(-3));
		assertNull(WikiLinks.priceChartUrl(1_000_000));
	}
}
