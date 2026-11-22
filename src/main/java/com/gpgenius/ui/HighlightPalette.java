package com.gpgenius.ui;

import java.awt.Color;

/**
 * Colours for the in-game highlights. Colour is never the only signal: the overlay also draws a
 * dashed outline on slots that won't fill and a cross on offers to abort, so every palette still
 * reads in greyscale.
 *
 * <p>The colour-blind palettes come from Okabe and Ito ("Color Universal Design") and Paul Tol's
 * schemes, and HighlightPaletteTest checks them under simulated colour blindness.
 */
public enum HighlightPalette
{
	/** Follows the panel theme. */
	THEME("Match theme", 0, 0, 0, 0),
	/** Protanopia and deuteranopia: no red against green. */
	RED_GREEN_SAFE("Red-green safe", 0xF0E442, 0xD55E00, 0x56B4E9, 0xE69F00),
	/** Tritanopia: no blue against green or yellow against violet. */
	BLUE_YELLOW_SAFE("Blue-yellow safe", 0x33BBEE, 0xEE3377, 0x009988, 0xCC3311),
	/** Saturated colours that stand out on the GE's brown interface. */
	HIGH_VISIBILITY("High visibility", 0xFFFF00, 0xFF00FF, 0x00FF66, 0xFF2020),
	/** White only; shapes carry the meaning. */
	MONOCHROME("White (shapes only)", 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF);

	private final String label;
	private final int next;
	private final int abort;
	private final int good;
	private final int bad;

	HighlightPalette(String label, int next, int abort, int good, int bad)
	{
		this.label = label;
		this.next = next;
		this.abort = abort;
		this.good = good;
		this.bad = bad;
	}

	/** The resolved colours for one theme. */
	public static final class Colors
	{
		/** The thing to click next. */
		public final Color next;
		/** An offer to abort. */
		public final Color abort;
		/** An active offer that will still fill. */
		public final Color good;
		/** An active offer the market has moved past. */
		public final Color bad;

		Colors(Color next, Color abort, Color good, Color bad)
		{
			this.next = next;
			this.abort = abort;
			this.good = good;
			this.bad = bad;
		}
	}

	public Colors colors(Theme theme)
	{
		if (this == THEME)
		{
			Theme t = theme == null ? Theme.GRAND_EXCHANGE : theme;
			return new Colors(t.accent, t.loss, t.profit, t.loss);
		}
		return new Colors(new Color(next), new Color(abort), new Color(good), new Color(bad));
	}

	@Override
	public String toString()
	{
		return label;
	}
}
