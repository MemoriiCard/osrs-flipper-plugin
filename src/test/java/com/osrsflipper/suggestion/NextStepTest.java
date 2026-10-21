package com.osrsflipper.suggestion;

import static org.junit.Assert.assertEquals;

import com.osrsflipper.api.Suggestion;
import org.junit.Test;

public class NextStepTest
{
	private static Suggestion buy()
	{
		Suggestion s = new Suggestion();
		s.action = Suggestion.BUY;
		s.itemId = 4151;
		s.name = "Abyssal whip";
		s.price = 1_500_000L;
		s.quantity = 2;
		return s;
	}

	private static NextStep.Screen home()
	{
		NextStep.Screen screen = new NextStep.Screen();
		screen.open = true;
		screen.firstEmptySlot = 3;
		return screen;
	}

	private static NextStep.Screen setup(boolean sell, int itemId, long price, int quantity)
	{
		NextStep.Screen screen = home();
		screen.openSlot = 3;
		screen.setupOpen = true;
		screen.setupSell = sell;
		screen.setupItemId = itemId;
		screen.setupPrice = price;
		screen.setupQuantity = quantity;
		return screen;
	}

	@Test
	public void nothingWhenGeClosedOrWaiting()
	{
		assertEquals(NextStep.Step.NONE, NextStep.of(buy(), new NextStep.Screen()));
		Suggestion wait = new Suggestion();
		wait.action = Suggestion.WAIT;
		assertEquals(NextStep.Step.NONE, NextStep.of(wait, home()));
	}

	@Test
	public void buyWalksThroughSetup()
	{
		assertEquals(NextStep.Step.BUY_BUTTON, NextStep.of(buy(), home()));

		NextStep.Screen searching = setup(false, -1, 0, 0);
		searching.searchOpen = true;
		assertEquals(NextStep.Step.SEARCH_RESULT, NextStep.of(buy(), searching));

		assertEquals(NextStep.Step.SET_PRICE, NextStep.of(buy(), setup(false, 4151, 1_400_000, 1)));
		assertEquals(NextStep.Step.SET_QUANTITY, NextStep.of(buy(), setup(false, 4151, 1_500_000, 1)));
		assertEquals(NextStep.Step.CONFIRM, NextStep.of(buy(), setup(false, 4151, 1_500_000, 2)));
	}

	@Test
	public void noBuyButtonWithoutFreeSlot()
	{
		NextStep.Screen full = home();
		full.firstEmptySlot = -1;
		assertEquals(NextStep.Step.NONE, NextStep.of(buy(), full));
	}

	@Test
	public void sellStartsFromInventoryAndIgnoresWrongOfferType()
	{
		Suggestion sell = buy();
		sell.action = Suggestion.SELL;
		assertEquals(NextStep.Step.INVENTORY_ITEM, NextStep.of(sell, home()));
		assertEquals(NextStep.Step.NONE, NextStep.of(sell, setup(false, 4151, 1, 1)));
		assertEquals(NextStep.Step.SET_PRICE, NextStep.of(sell, setup(true, 4151, 1, 2)));
	}

	@Test
	public void abortHighlightsSlotOnHomeScreen()
	{
		Suggestion abort = buy();
		abort.action = Suggestion.ABORT;
		abort.slot = 5;
		assertEquals(NextStep.Step.SLOT, NextStep.of(abort, home()));
		NextStep.Screen viewing = home();
		viewing.openSlot = 5;
		assertEquals(NextStep.Step.NONE, NextStep.of(abort, viewing));
	}

	@Test
	public void promptValueOnlyForMatchingOffer()
	{
		NextStep.Screen screen = setup(false, 4151, 0, 0);
		assertEquals(1_500_000L, NextStep.promptValue(buy(), screen, true));
		assertEquals(2L, NextStep.promptValue(buy(), screen, false));
		assertEquals(-1L, NextStep.promptValue(buy(), setup(false, 11802, 0, 0), true));
		assertEquals(-1L, NextStep.promptValue(buy(), setup(true, 4151, 0, 0), true));
	}
}
