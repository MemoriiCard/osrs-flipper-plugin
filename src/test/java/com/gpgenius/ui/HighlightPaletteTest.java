package com.gpgenius.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.awt.Color;
import org.junit.Test;

public class HighlightPaletteTest
{
	/** Machado, Oliveira and Fernandes (2009), full severity, applied to linear RGB. */
	private static final double[][] PROTAN = {
		{0.152286, 1.052583, -0.204868}, {0.114503, 0.786281, 0.099216}, {-0.003882, -0.048116, 1.051998}};
	private static final double[][] DEUTAN = {
		{0.367322, 0.860646, -0.227968}, {0.280085, 0.672501, 0.047413}, {-0.011820, 0.042940, 0.968881}};
	private static final double[][] TRITAN = {
		{1.255528, -0.076749, -0.178779}, {-0.078411, 0.930809, 0.147602}, {0.004733, 0.691367, 0.303900}};
	/** The brown behind GE slots and buttons. */
	private static final Color GE_BROWN = new Color(0x3E3529);

	@Test
	public void redGreenSafeKeepsFillAndOutbidApartForProtanAndDeutan()
	{
		HighlightPalette.Colors c = HighlightPalette.RED_GREEN_SAFE.colors(Theme.GRAND_EXCHANGE);
		for (double[][] m : new double[][][]{PROTAN, DEUTAN})
		{
			assertTrue(distance(c.good, c.bad, m) >= 100);
			assertTrue(distance(c.next, c.good, m) >= 60);
			assertTrue(distance(c.next, c.bad, m) >= 60);
		}
	}

	@Test
	public void blueYellowSafeKeepsColoursApartForTritan()
	{
		HighlightPalette.Colors c = HighlightPalette.BLUE_YELLOW_SAFE.colors(Theme.GRAND_EXCHANGE);
		assertTrue(distance(c.good, c.bad, TRITAN) >= 100);
		assertTrue(distance(c.next, c.good, TRITAN) >= 60);
		assertTrue(distance(c.next, c.bad, TRITAN) >= 60);
	}

	@Test
	public void defaultThemeColoursAreWhyTheSafePalettesExist()
	{
		// Green and red from the default theme nearly merge for deuteranopes.
		HighlightPalette.Colors c = HighlightPalette.THEME.colors(Theme.GRAND_EXCHANGE);
		assertTrue(distance(c.good, c.bad, DEUTAN) < 40);
	}

	@Test
	public void nextStepColourStandsOutOnTheGeInterface()
	{
		for (HighlightPalette p : HighlightPalette.values())
		{
			for (Theme t : Theme.values())
			{
				assertTrue(p + " on " + t, Theme.contrast(p.colors(t).next, GE_BROWN) >= 3);
			}
		}
	}

	@Test
	public void matchThemeFollowsTheTheme()
	{
		HighlightPalette.Colors c = HighlightPalette.THEME.colors(Theme.MATRIX);
		assertEquals(Theme.MATRIX.accent, c.next);
		assertEquals(Theme.MATRIX.loss, c.abort);
		assertEquals(Theme.GRAND_EXCHANGE.accent, HighlightPalette.THEME.colors(null).next);
	}

	private static double distance(Color a, Color b, double[][] m)
	{
		double[] x = simulate(a, m);
		double[] y = simulate(b, m);
		return Math.sqrt(sq(x[0] - y[0]) + sq(x[1] - y[1]) + sq(x[2] - y[2]));
	}

	private static double[] simulate(Color c, double[][] m)
	{
		double[] lin = {lin(c.getRed()), lin(c.getGreen()), lin(c.getBlue())};
		double[] out = new double[3];
		for (int i = 0; i < 3; i++)
		{
			out[i] = unlin(m[i][0] * lin[0] + m[i][1] * lin[1] + m[i][2] * lin[2]);
		}
		return out;
	}

	private static double lin(int v)
	{
		double s = v / 255.0;
		return s <= 0.04045 ? s / 12.92 : Math.pow((s + 0.055) / 1.055, 2.4);
	}

	private static double unlin(double v)
	{
		v = Math.max(0, Math.min(1, v));
		return 255 * (v <= 0.0031308 ? v * 12.92 : 1.055 * Math.pow(v, 1 / 2.4) - 0.055);
	}

	private static double sq(double v)
	{
		return v * v;
	}
}
