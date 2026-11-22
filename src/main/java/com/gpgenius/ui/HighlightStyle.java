package com.gpgenius.ui;

import java.awt.Rectangle;

/**
 * How big and how strong the next-step highlight is drawn. Plain numbers so it can be unit tested;
 * the overlay builds one from the settings each frame.
 */
public final class HighlightStyle
{
	public static final int MIN_THICKNESS = 1;
	public static final int MAX_THICKNESS = 8;
	public static final int MAX_PADDING = 12;
	/** One full pulse, slow enough not to flicker (well under 3 flashes a second). */
	static final long PULSE_PERIOD_MILLIS = 1_400;
	/** The pulse never drops below this share of the chosen opacity, so the outline never vanishes. */
	static final double PULSE_FLOOR = 0.55;

	/** Outline width in pixels. */
	public final int thickness;
	/** Pixels added around the target on every side. */
	public final int padding;
	/** Outline opacity, 0 to 100. */
	public final int outlinePercent;
	/** Fill opacity, 0 to 100. */
	public final int fillPercent;
	public final boolean pulse;

	public HighlightStyle(int thickness, int padding, int outlinePercent, int fillPercent, boolean pulse)
	{
		this.thickness = clamp(thickness, MIN_THICKNESS, MAX_THICKNESS);
		this.padding = clamp(padding, 0, MAX_PADDING);
		// A fully transparent outline would hide the highlight; keep it visible.
		this.outlinePercent = clamp(outlinePercent, 10, 100);
		this.fillPercent = clamp(fillPercent, 0, 100);
		this.pulse = pulse;
	}

	/** The target grown by the padding. The outline is drawn inside this, so it never covers the target. */
	public Rectangle area(Rectangle target)
	{
		return new Rectangle(target.x - padding, target.y - padding,
			target.width + 2 * padding, target.height + 2 * padding);
	}

	/** Outline alpha, 0 to 255, at this moment. */
	public int outlineAlpha(long nowMillis)
	{
		return alpha(outlinePercent, nowMillis);
	}

	/** Fill alpha, 0 to 255, at this moment. */
	public int fillAlpha(long nowMillis)
	{
		return fillPercent == 0 ? 0 : alpha(fillPercent, nowMillis);
	}

	private int alpha(int percent, long nowMillis)
	{
		double a = percent / 100.0 * 255;
		if (pulse)
		{
			double phase = (Math.floorMod(nowMillis, PULSE_PERIOD_MILLIS)) / (double) PULSE_PERIOD_MILLIS;
			// Smooth 0..1..0 wave.
			double wave = 0.5 - 0.5 * Math.cos(2 * Math.PI * phase);
			a *= PULSE_FLOOR + (1 - PULSE_FLOOR) * wave;
		}
		return clamp((int) Math.round(a), 0, 255);
	}

	private static int clamp(int v, int lo, int hi)
	{
		return Math.max(lo, Math.min(hi, v));
	}
}
