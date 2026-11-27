package com.gpgenius.ui;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Side panel layout presets, and which parts ("columns") of the panel each one shows. The next step
 * card itself is always shown. Free of Swing so it can be unit tested.
 */
public enum PanelLayout
{
	/** Everything: the default, and how the panel looked before presets. */
	DETAILED("Detailed", EnumSet.allOf(Part.class)),
	/** The next step, alerts, limits, goal and profit. No chart or flip list. */
	COMPACT("Compact", EnumSet.of(Part.PRICE, Part.EST_PROFIT, Part.DETAILS, Part.GOAL, Part.ALERTS,
		Part.LIMITS, Part.PROFIT)),
	/** Only the next step with its price, and the profit totals. */
	MINIMAL("Minimal", EnumSet.of(Part.PRICE, Part.PROFIT)),
	/** The parts picked under "Panel parts". */
	CUSTOM("Custom", EnumSet.noneOf(Part.class));

	/** One part of the panel that a layout can show or hide. */
	public enum Part
	{
		PRICE("Step: price and quantity"),
		EST_PROFIT("Step: estimated profit"),
		DETAILS("Step: details (paid, break-even, limit)"),
		GOAL("Goal card"),
		ALERTS("Dump alerts"),
		LIMITS("Buy limit timers"),
		CHART("Price chart"),
		PROFIT("Profit totals"),
		STACK("Suggested cash stack"),
		FLIPS("Recent flips"),
		FLIP_QUANTITY("Flips: quantity column"),
		FLIP_PRICES("Flips: buy and sell price column");

		private final String displayName;

		Part(String displayName)
		{
			this.displayName = displayName;
		}

		@Override
		public String toString()
		{
			return displayName;
		}
	}

	private final String displayName;
	private final Set<Part> parts;

	PanelLayout(String displayName, Set<Part> parts)
	{
		this.displayName = displayName;
		this.parts = Collections.unmodifiableSet(parts);
	}

	/**
	 * What to show. The "Compact panel" switch (and its hotkey) still works and means Compact.
	 *
	 * @param custom the player's picks, used by Custom only
	 */
	public static View view(PanelLayout chosen, boolean compactSwitch, Set<Part> custom)
	{
		PanelLayout layout = compactSwitch ? COMPACT : chosen == null ? DETAILED : chosen;
		Set<Part> shown = layout == CUSTOM
			? (custom == null || custom.isEmpty() ? EnumSet.noneOf(Part.class) : EnumSet.copyOf(custom))
			: EnumSet.copyOf(layout.parts.isEmpty() ? EnumSet.noneOf(Part.class) : layout.parts);
		return new View(layout, shown);
	}

	@Override
	public String toString()
	{
		return displayName;
	}

	/** A resolved layout: the preset and the parts it shows. */
	public static final class View
	{
		public static final View DEFAULT = view(DETAILED, false, null);

		public final PanelLayout layout;
		private final Set<Part> shown;

		View(PanelLayout layout, Set<Part> shown)
		{
			this.layout = layout;
			this.shown = Collections.unmodifiableSet(shown);
		}

		public boolean shows(Part part)
		{
			return shown.contains(part);
		}

		/** Anything but Detailed trims the "nothing here yet" cards, as Compact always has. */
		public boolean trimmed()
		{
			return layout != DETAILED;
		}

		/** Space between cards, in pixels: tighter for the smaller layouts. */
		public int gap()
		{
			return layout == MINIMAL ? 4 : layout == COMPACT ? 6 : 8;
		}

		@Override
		public boolean equals(Object o)
		{
			return o instanceof View && ((View) o).layout == layout && ((View) o).shown.equals(shown);
		}

		@Override
		public int hashCode()
		{
			return layout.hashCode() * 31 + shown.hashCode();
		}
	}
}
