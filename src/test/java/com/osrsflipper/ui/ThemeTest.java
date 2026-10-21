package com.osrsflipper.ui;

import static org.junit.Assert.assertTrue;
import java.awt.Color;
import org.junit.Test;

public class ThemeTest
{
	@Test
	public void everyThemeIsDark()
	{
		for (Theme t : Theme.values())
		{
			for (Color c : new Color[]{t.background, t.surface, t.raised})
			{
				assertTrue(t + " surface too bright", Theme.luminance(c) < 0.04);
			}
		}
	}

	@Test
	public void textIsReadableOnEverySurface()
	{
		for (Theme t : Theme.values())
		{
			for (Color surface : new Color[]{t.background, t.surface, t.raised})
			{
				assertTrue(t + " text", Theme.contrast(t.text, surface) >= 7);
				assertTrue(t + " muted", Theme.contrast(t.muted, surface) >= 4.5);
				for (Color c : new Color[]{t.accent, t.profit, t.loss, t.warning, t.chartHigh, t.chartLow})
				{
					assertTrue(t + " colour " + c, Theme.contrast(c, surface) >= 4.5);
				}
			}
			assertTrue(t + " on accent", Theme.contrast(t.onAccent, t.accent) >= 7);
		}
	}
}
