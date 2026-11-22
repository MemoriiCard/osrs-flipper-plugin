package com.gpgenius.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.awt.Rectangle;
import org.junit.Test;

public class HighlightStyleTest
{
	@Test
	public void valuesStayInRange()
	{
		HighlightStyle s = new HighlightStyle(50, -3, 0, 400, false);
		assertEquals(HighlightStyle.MAX_THICKNESS, s.thickness);
		assertEquals(0, s.padding);
		// The outline can't be made invisible.
		assertEquals(10, s.outlinePercent);
		assertEquals(100, s.fillPercent);
	}

	@Test
	public void paddingGrowsTheAreaOnEverySide()
	{
		Rectangle area = new HighlightStyle(3, 4, 85, 20, false).area(new Rectangle(100, 50, 40, 20));
		assertEquals(new Rectangle(96, 46, 48, 28), area);
	}

	@Test
	public void steadyWithoutPulse()
	{
		HighlightStyle s = new HighlightStyle(3, 2, 100, 0, false);
		assertEquals(255, s.outlineAlpha(0));
		assertEquals(255, s.outlineAlpha(700));
		assertEquals(0, s.fillAlpha(700));
	}

	@Test
	public void pulseIsSlowAndNeverFades()
	{
		HighlightStyle s = new HighlightStyle(3, 2, 100, 50, true);
		int min = 255;
		int max = 0;
		for (long t = 0; t < HighlightStyle.PULSE_PERIOD_MILLIS; t += 20)
		{
			min = Math.min(min, s.outlineAlpha(t));
			max = Math.max(max, s.outlineAlpha(t));
		}
		assertEquals(255, max);
		assertTrue(min >= Math.round(255 * HighlightStyle.PULSE_FLOOR) - 1);
		// Under three flashes a second.
		assertTrue(HighlightStyle.PULSE_PERIOD_MILLIS > 333);
	}
}
