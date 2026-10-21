package com.osrsflipper.ui;

import java.awt.Color;

/**
 * Colour themes for the side panel, chart and in-game highlights. Every theme is dark: backgrounds
 * stay near black so the panel never outshines the game, and text meets strong contrast on every
 * surface (checked in ThemeTest).
 */
public enum Theme
{
	GRAND_EXCHANGE("Grand Exchange",
		0x15120E, 0x201C16, 0x2B261E, 0x3A3328, 0xEEE4CD, 0xA99D85, 0xE3B341, 0x1A1408,
		0x6CC24A, 0xF26D62, 0xF2A93B, 0xE3B341, 0x6FA8DC),
	RUNELITE("RuneLite",
		0x1B1B1B, 0x262626, 0x313131, 0x3E3E3E, 0xE8E8E8, 0xA8A8A8, 0xE89B2B, 0x1A1205,
		0x5CC245, 0xFF7E73, 0xF2C04B, 0xE89B2B, 0x5DA9E9),
	MATRIX("Matrix",
		0x030805, 0x08120B, 0x0E1C12, 0x173823, 0xB9F6C8, 0x63AE79, 0x3DFF72, 0x021006,
		0x3DFF72, 0xFF4D6A, 0xE8FF4D, 0x3DFF72, 0x4DD9FF),
	AMETHYST("Amethyst",
		0x110D18, 0x1B1526, 0x251D33, 0x372C4B, 0xEDE5FA, 0xA898C4, 0xB485FF, 0x150A26,
		0x5DD39E, 0xFF6B8B, 0xFFB86B, 0xB485FF, 0x5DC8FF),
	CYAN("Neon Cyan",
		0x071216, 0x0D1D23, 0x13282F, 0x1F3D47, 0xDDF7FA, 0x82B2BB, 0x2EE6F0, 0x02181B,
		0x4BE38A, 0xFF5C6C, 0xFFC857, 0x2EE6F0, 0xFF9F5A),
	DRAGON("Dragon",
		0x150B0B, 0x211212, 0x2D1818, 0x452525, 0xF6E4E0, 0xBC968F, 0xFF7A5C, 0x140402,
		0x6CC24A, 0xFF7088, 0xFFC04D, 0xFF7A5C, 0x5DB5E8);

	private final String displayName;
	/** Panel background. */
	public final Color background;
	/** Cards. */
	public final Color surface;
	/** Buttons and rows on a card, and hover states. */
	public final Color raised;
	public final Color border;
	public final Color text;
	public final Color muted;
	/** Brand colour: titles, primary buttons, highlights. */
	public final Color accent;
	/** Text on an accent fill. */
	public final Color onAccent;
	public final Color profit;
	public final Color loss;
	public final Color warning;
	public final Color chartHigh;
	public final Color chartLow;

	Theme(String displayName, int background, int surface, int raised, int border, int text, int muted,
		int accent, int onAccent, int profit, int loss, int warning, int chartHigh, int chartLow)
	{
		this.displayName = displayName;
		this.background = new Color(background);
		this.surface = new Color(surface);
		this.raised = new Color(raised);
		this.border = new Color(border);
		this.text = new Color(text);
		this.muted = new Color(muted);
		this.accent = new Color(accent);
		this.onAccent = new Color(onAccent);
		this.profit = new Color(profit);
		this.loss = new Color(loss);
		this.warning = new Color(warning);
		this.chartHigh = new Color(chartHigh);
		this.chartLow = new Color(chartLow);
	}

	/** The colour for an action badge. */
	public Color action(String action)
	{
		if ("sell".equals(action))
		{
			return profit;
		}
		if ("abort".equals(action))
		{
			return loss;
		}
		if ("wait".equals(action))
		{
			return muted;
		}
		return accent;
	}

	public static Color withAlpha(Color c, int alpha)
	{
		return new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
	}

	/** WCAG relative luminance. */
	static double luminance(Color c)
	{
		return 0.2126 * channel(c.getRed()) + 0.7152 * channel(c.getGreen()) + 0.0722 * channel(c.getBlue());
	}

	static double contrast(Color a, Color b)
	{
		double la = luminance(a);
		double lb = luminance(b);
		return (Math.max(la, lb) + 0.05) / (Math.min(la, lb) + 0.05);
	}

	private static double channel(int v)
	{
		double s = v / 255.0;
		return s <= 0.04045 ? s / 12.92 : Math.pow((s + 0.055) / 1.055, 2.4);
	}

	@Override
	public String toString()
	{
		return displayName;
	}
}
