package com.osrsflipper.tracking;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Grand Exchange sales tax: 2% of the sale price per item, rounded down, capped at 5m per item.
 * Items selling for under 50 gp round down to no tax, and a few items are never taxed.
 */
public final class GeTax
{
	static final double RATE = 0.02;
	static final long MAX_TAX_PER_ITEM = 5_000_000L;

	/**
	 * Old school bonds and the basic skilling tools from the tax's launch update. Keep in step with the
	 * backend's app/engine/tax.py.
	 */
	static final Set<Integer> EXEMPT_ITEM_IDS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
		13190, // Old school bond
		1755, // Chisel
		5343, // Seed dibber
		5325, // Gardening trowel
		1785, // Glassblowing pipe
		2347, // Hammer
		1733, // Needle
		233, // Pestle and mortar
		5341, // Rake
		8794, // Saw
		5329, // Secateurs
		1735, // Shears
		952, // Spade
		5331 // Watering can (empty)
	)));

	private GeTax()
	{
	}

	public static long perItem(long pricePerItem)
	{
		return Math.min((long) Math.floor(pricePerItem * RATE), MAX_TAX_PER_ITEM);
	}

	public static long perItem(int itemId, long pricePerItem)
	{
		return EXEMPT_ITEM_IDS.contains(itemId) ? 0 : perItem(pricePerItem);
	}

	public static long total(int itemId, long pricePerItem, int quantity)
	{
		return perItem(itemId, pricePerItem) * quantity;
	}
}
