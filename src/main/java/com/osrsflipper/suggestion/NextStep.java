package com.osrsflipper.suggestion;

import com.osrsflipper.api.Suggestion;

/**
 * Works out which part of the GE the player should click next to carry out a suggestion.
 * Kept free of RuneLite types so it can be unit tested.
 */
public final class NextStep
{
	public enum Step
	{
		NONE,
		/** "Buy" on an empty slot. */
		BUY_BUTTON,
		/** The suggested item in the chatbox item search. */
		SEARCH_RESULT,
		/** The item in the GE inventory, which opens a sell offer. */
		INVENTORY_ITEM,
		/** The slot holding an offer to abort. */
		SLOT,
		SET_PRICE,
		SET_QUANTITY,
		CONFIRM
	}

	/** What the GE screen shows right now. */
	public static class Screen
	{
		public boolean open;
		/** Slot being viewed or set up, or -1 on the home screen. */
		public int openSlot = -1;
		public boolean setupOpen;
		public boolean setupSell;
		public int setupItemId = -1;
		public long setupPrice;
		public int setupQuantity;
		public int firstEmptySlot = -1;
		public boolean searchOpen;
	}

	private NextStep()
	{
	}

	public static Step of(Suggestion s, Screen screen)
	{
		if (s == null || !screen.open || s.itemId == null || s.is(Suggestion.WAIT))
		{
			return Step.NONE;
		}

		if (s.is(Suggestion.ABORT))
		{
			return screen.openSlot == -1 && s.slot != null ? Step.SLOT : Step.NONE;
		}

		boolean buy = s.is(Suggestion.BUY);
		if (screen.setupOpen)
		{
			boolean typeMatches = screen.setupSell != buy;
			if (!typeMatches)
			{
				return Step.NONE;
			}
			if (screen.setupItemId != s.itemId)
			{
				return buy && screen.searchOpen ? Step.SEARCH_RESULT : Step.NONE;
			}
			if (s.price != null && screen.setupPrice != s.price)
			{
				return Step.SET_PRICE;
			}
			if (s.quantity != null && screen.setupQuantity != s.quantity)
			{
				return Step.SET_QUANTITY;
			}
			return Step.CONFIRM;
		}

		if (screen.openSlot != -1)
		{
			// Viewing an existing offer; the player has to go back first.
			return Step.NONE;
		}
		if (buy)
		{
			return screen.firstEmptySlot == -1 ? Step.NONE : Step.BUY_BUTTON;
		}
		return Step.INVENTORY_ITEM;
	}

	/** The value the quick-set key should type into the open prompt, or -1 if none applies. */
	public static long promptValue(Suggestion s, Screen screen, boolean pricePrompt)
	{
		if (s == null || s.itemId == null || !screen.setupOpen || screen.setupItemId != s.itemId)
		{
			return -1;
		}
		if (!s.is(Suggestion.BUY) && !s.is(Suggestion.SELL))
		{
			return -1;
		}
		if (screen.setupSell == s.is(Suggestion.BUY))
		{
			return -1;
		}
		if (pricePrompt)
		{
			return s.price == null ? -1 : s.price;
		}
		return s.quantity == null ? -1 : s.quantity;
	}
}
