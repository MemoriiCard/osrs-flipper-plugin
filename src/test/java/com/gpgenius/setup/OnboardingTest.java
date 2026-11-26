package com.gpgenius.setup;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

public class OnboardingTest
{
	private static Map<Integer, Long> items(long coins, long platinum)
	{
		Map<Integer, Long> m = new HashMap<>();
		m.put(Onboarding.COINS, coins);
		m.put(Onboarding.PLATINUM_TOKEN, platinum);
		m.put(4151, 1L);
		return m;
	}

	@Test
	public void countsCoinsAndPlatinumTokensInTheInventoryAndBank()
	{
		Onboarding.Detected d = Onboarding.detect(items(2_500_000, 10), items(40_000_000, 0), true, 2);
		assertEquals(2_510_000, d.inventoryCoins);
		assertEquals(40_000_000, d.bankCoins);
		assertEquals(42_510_000, d.total());
		assertEquals(8, d.slotsAvailable());
		assertTrue(Onboarding.cashHint(d).contains("40m in your bank"));
		assertTrue(Onboarding.slotsHint(d).contains("2 already hold offers"));
	}

	@Test
	public void bankNotOpenedYetSaysSo()
	{
		Onboarding.Detected d = Onboarding.detect(items(5_000, 0), null, false, 0);
		assertFalse(d.bankSeen());
		assertEquals(5_000, d.total());
		assertEquals(3, d.slotsAvailable());
		assertTrue(Onboarding.cashHint(d).contains("Open your bank"));
	}

	@Test
	public void defaultsUseEveryCoinCarriedAndEverySlot()
	{
		Onboarding.Detected d = Onboarding.detect(items(1_000_000, 0), null, false, 0);
		Onboarding.Choices c = Onboarding.defaults(d, "HIGH", 120);
		assertEquals(0, c.cashCap);
		assertEquals("HIGH", c.risk);
		assertEquals(120, c.checkInMinutes);
		assertEquals(3, c.slotsToUse);
		Onboarding.Choices odd = Onboarding.defaults(d, "WILD", 7);
		assertEquals("MEDIUM", odd.risk);
		assertEquals(30, odd.checkInMinutes);
	}

	@Test
	public void choicesBecomeSettings()
	{
		Onboarding.Choices c = new Onboarding.Choices();
		c.cashCap = 25_000_000;
		c.risk = "LOW";
		c.checkInMinutes = 480;
		c.slotsAvailable = 8;
		c.slotsToUse = 6;
		Map<String, Object> s = Onboarding.settings(c);
		assertEquals(25_000_000, s.get("coinCap"));
		assertEquals("LOW", s.get("riskLevel"));
		assertEquals("HOURS_8", s.get("checkIn"));
		assertEquals(2, s.get("reservedSlots"));
		assertEquals(true, s.get("setupDone"));
		assertEquals("Up to 25m", Onboarding.summary(c).get(0));
		assertEquals("6 of 8 GE slots (2 kept for you)", Onboarding.summary(c).get(3));
	}

	@Test
	public void cashCapStaysInRangeAndAtLeastOneSlotIsUsed()
	{
		Onboarding.Choices c = new Onboarding.Choices();
		c.cashCap = 9_000_000_000L;
		c.slotsAvailable = 3;
		c.slotsToUse = 0;
		Map<String, Object> s = Onboarding.settings(c);
		assertEquals(Integer.MAX_VALUE, s.get("coinCap"));
		assertEquals(2, s.get("reservedSlots"));
	}

	@Test
	public void parsesTypedAmounts()
	{
		assertEquals(25_000_000, Onboarding.parseGp("25m"));
		assertEquals(1_500_000_000, Onboarding.parseGp("1.5B"));
		assertEquals(500_000, Onboarding.parseGp("500k"));
		assertEquals(2_000_000, Onboarding.parseGp("2,000,000 gp"));
		assertEquals(-1, Onboarding.parseGp("lots"));
		assertEquals(-1, Onboarding.parseGp(""));
		assertEquals(-1, Onboarding.parseGp("-5m"));
	}

	@Test
	public void stepsRunInOrder()
	{
		assertEquals(Onboarding.Step.RISK, Onboarding.Step.CASH.next());
		assertEquals(Onboarding.Step.DONE, Onboarding.Step.SLOTS.next());
		assertEquals(Onboarding.Step.DONE, Onboarding.Step.DONE.next());
		assertEquals(Onboarding.Step.CASH, Onboarding.Step.CASH.back());
		assertEquals(4, Onboarding.Step.SLOTS.number());
		assertEquals(4, Onboarding.Step.DONE.number());
	}
}
