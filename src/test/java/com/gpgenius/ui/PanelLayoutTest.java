package com.gpgenius.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import com.gpgenius.ui.PanelLayout.Part;
import java.util.EnumSet;
import org.junit.Test;

public class PanelLayoutTest
{
	@Test
	public void detailedShowsEverything()
	{
		PanelLayout.View v = PanelLayout.view(PanelLayout.DETAILED, false, null);
		for (Part p : Part.values())
		{
			assertTrue(p.name(), v.shows(p));
		}
		assertFalse(v.trimmed());
		assertEquals(v, PanelLayout.View.DEFAULT);
	}

	@Test
	public void compactMatchesTheOldCompactPanel()
	{
		PanelLayout.View v = PanelLayout.view(PanelLayout.COMPACT, false, null);
		assertFalse(v.shows(Part.CHART));
		assertFalse(v.shows(Part.FLIPS));
		assertTrue(v.shows(Part.PROFIT) && v.shows(Part.LIMITS) && v.shows(Part.ALERTS));
		// The old switch and its hotkey still mean Compact, whatever preset is picked.
		assertEquals(PanelLayout.COMPACT, PanelLayout.view(PanelLayout.MINIMAL, true, null).layout);
	}

	@Test
	public void minimalAndCustom()
	{
		PanelLayout.View min = PanelLayout.view(PanelLayout.MINIMAL, false, EnumSet.of(Part.CHART));
		assertTrue(min.shows(Part.PRICE) && min.shows(Part.PROFIT));
		assertFalse(min.shows(Part.CHART) || min.shows(Part.DETAILS) || min.shows(Part.GOAL));
		assertTrue(min.gap() < PanelLayout.View.DEFAULT.gap());

		PanelLayout.View custom = PanelLayout.view(PanelLayout.CUSTOM, false, EnumSet.of(Part.CHART, Part.FLIP_PRICES));
		assertTrue(custom.shows(Part.CHART) && custom.shows(Part.FLIP_PRICES));
		assertFalse(custom.shows(Part.PROFIT));
		assertFalse(PanelLayout.view(PanelLayout.CUSTOM, false, null).shows(Part.PROFIT));
		assertEquals(PanelLayout.DETAILED, PanelLayout.view(null, false, null).layout);
	}
}
