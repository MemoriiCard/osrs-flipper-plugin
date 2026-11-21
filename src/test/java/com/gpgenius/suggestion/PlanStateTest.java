package com.gpgenius.suggestion;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.gpgenius.api.Suggestion;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;

public class PlanStateTest
{
	private static final long HOUR = 3_600_000;

	private static Suggestion step(String action, Integer itemId)
	{
		Suggestion s = new Suggestion();
		s.action = action;
		s.itemId = itemId;
		return s;
	}

	private static Suggestion.Plan plan(Suggestion... steps)
	{
		Suggestion.Plan p = new Suggestion.Plan();
		p.steps = Arrays.asList(steps);
		return p;
	}

	@Test
	public void loginStartsAPlanThatLastsWhileItHasSteps()
	{
		PlanState state = new PlanState();
		assertFalse(state.wanted());
		state.start();
		assertTrue(state.wanted());

		state.onResult(plan(step("sell", 554), step("buy", 4151), step("buy", 11802)));
		assertEquals(3, state.steps().size());
		assertEquals(Arrays.asList(4151, 11802), state.plannedItemIds());

		// Down to the last step: back to one suggestion at a time.
		state.onResult(plan(step("buy", 11802)));
		assertFalse(state.wanted());
		assertNull(state.getPlan());
		assertTrue(state.plannedItemIds().isEmpty());
	}

	@Test
	public void comingBackAfterAWindowStartsAPlan()
	{
		PlanState state = new PlanState();
		assertFalse(state.atGe(true, 10 * HOUR, HOUR));
		assertFalse(state.atGe(true, 10 * HOUR + 1000, HOUR));
		assertFalse(state.atGe(false, 11 * HOUR, HOUR));
		assertTrue(state.atGe(true, 12 * HOUR, HOUR));
		assertTrue(state.wanted());
	}

	@Test
	public void hidingThePlanStopsAskingForIt()
	{
		PlanState state = new PlanState();
		state.start();
		state.onResult(plan(step("buy", 1), step("buy", 2)));
		state.dismiss();
		assertFalse(state.wanted());
		assertEquals(Collections.emptyList(), state.steps());
		state.onResult(plan(step("buy", 1), step("buy", 2)));
		assertNull(state.getPlan());
	}
}
