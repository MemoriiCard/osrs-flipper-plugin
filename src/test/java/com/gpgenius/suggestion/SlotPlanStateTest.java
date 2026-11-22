package com.gpgenius.suggestion;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.gpgenius.api.SlotPlan;
import org.junit.Test;

public class SlotPlanStateTest
{
	@Test
	public void asksAtMostOnceEveryGapAndNeverTwiceAtOnce()
	{
		SlotPlanState state = new SlotPlanState();
		assertTrue(state.due(1_000));
		state.sent(1_000);
		assertFalse(state.due(1_000 + SlotPlanState.MIN_GAP_MILLIS * 5));
		SlotPlan plan = new SlotPlan();
		state.received(plan);
		assertSame(plan, state.getPlan());
		assertFalse(state.due(1_000 + SlotPlanState.MIN_GAP_MILLIS - 1));
		assertTrue(state.due(1_000 + SlotPlanState.MIN_GAP_MILLIS));
	}

	@Test
	public void aFailureKeepsTheLastPlanAndResetClearsIt()
	{
		SlotPlanState state = new SlotPlanState();
		SlotPlan plan = new SlotPlan();
		state.sent(1);
		state.received(plan);
		state.sent(20_000);
		state.failed();
		assertSame(plan, state.getPlan());
		state.reset();
		assertNull(state.getPlan());
		assertTrue(state.due(5));
	}
}
