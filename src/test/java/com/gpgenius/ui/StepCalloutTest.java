package com.gpgenius.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import com.gpgenius.api.Suggestion;
import com.gpgenius.suggestion.NextStep;
import org.junit.Test;

public class StepCalloutTest
{
	private static Suggestion buy()
	{
		Suggestion s = new Suggestion();
		s.action = Suggestion.BUY;
		s.itemId = 4151;
		s.name = "Abyssal whip";
		s.price = 1_250_000L;
		s.quantity = 30;
		return s;
	}

	@Test
	public void saysExactlyWhatToClickOrType()
	{
		Suggestion s = buy();
		assertEquals("Click Buy", StepCallout.of(NextStep.Step.BUY_BUTTON, s));
		assertEquals("Click Abyssal whip", StepCallout.of(NextStep.Step.SEARCH_RESULT, s));
		assertEquals("Set price to 1,250,000", StepCallout.of(NextStep.Step.SET_PRICE, s));
		assertEquals("Set quantity to 30", StepCallout.of(NextStep.Step.SET_QUANTITY, s));
		assertEquals("Click Confirm", StepCallout.of(NextStep.Step.CONFIRM, s));
		assertEquals("Abort this offer: Abyssal whip", StepCallout.of(NextStep.Step.SLOT, s));
		assertEquals("Click Abyssal whip to sell", StepCallout.of(NextStep.Step.INVENTORY_ITEM, s));
	}

	@Test
	public void nothingForNoStep()
	{
		assertNull(StepCallout.of(NextStep.Step.NONE, buy()));
		assertNull(StepCallout.of(NextStep.Step.CONFIRM, null));
	}

	@Test
	public void promptLabel()
	{
		assertEquals("Type 1,250,000 gp, then press Enter", StepCallout.prompt(true, 1_250_000));
		assertEquals("Type 30, then press Enter", StepCallout.prompt(false, 30));
		assertNull(StepCallout.prompt(true, -1));
	}
}
