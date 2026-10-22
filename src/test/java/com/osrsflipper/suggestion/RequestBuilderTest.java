package com.osrsflipper.suggestion;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.osrsflipper.OsrsFlipperConfig;
import com.osrsflipper.api.SuggestionRequest;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

public class RequestBuilderTest
{
	@Test
	public void parsesBlockedIdsLeniently()
	{
		assertEquals(Arrays.asList(4151, 11802), RequestBuilder.parseItemIds("4151, 11802,abc 4151 -3"));
		assertTrue(RequestBuilder.parseItemIds(null).isEmpty());
	}

	@Test
	public void blockAppendsOnce()
	{
		assertEquals("4151", RequestBuilder.withItemId("", 4151));
		assertEquals("4151,11802", RequestBuilder.withItemId("4151, junk", 11802));
		assertEquals("4151,11802", RequestBuilder.withItemId("4151,11802", 4151));
	}

	@Test
	public void checkInPresetsCoverFiveMinutesToADay()
	{
		assertEquals(5, OsrsFlipperConfig.CheckIn.values()[0].minutes);
		OsrsFlipperConfig.CheckIn[] all = OsrsFlipperConfig.CheckIn.values();
		assertEquals(1440, all[all.length - 1].minutes);
	}

	@Test
	public void onlyTrackedBuysInInventoryAreSellable()
	{
		Map<Integer, Long> inventory = new HashMap<>();
		inventory.put(995, 10_000_000L);
		inventory.put(4151, 3L);
		inventory.put(385, 20L);
		Map<Integer, Integer> holdings = new HashMap<>();
		holdings.put(4151, 2);
		holdings.put(11802, 1);

		Map<Integer, Integer> held = RequestBuilder.sellable(inventory, holdings);
		assertEquals(Collections.singletonMap(4151, 2), held);
	}

	@Test
	public void buildsRequestFromSettingsAndAccount()
	{
		RequestBuilder.Settings settings = new RequestBuilder.Settings();
		settings.risk = "high";
		settings.timeframeMinutes = 480;
		settings.blockedItems = "2";
		settings.minProfit = -5;
		settings.reservedSlots = 2;

		SuggestionRequest.Offer offer = new SuggestionRequest.Offer();
		offer.slot = 1;
		Map<Integer, Long> inventory = Collections.singletonMap(995, 123L);

		SuggestionRequest request = RequestBuilder.build(settings, Collections.singletonList(offer), inventory,
			Collections.emptyMap(), false, Collections.singletonList(7));
		assertEquals("high", request.profile.risk);
		assertEquals(480, request.profile.timeframeMinutes);
		assertEquals(Collections.singletonList(2), request.profile.blockedItemIds);
		assertEquals(0, request.profile.minProfitPerFlip);
		assertEquals(123L, request.account.coins);
		assertEquals(1, request.account.offers.size());
		assertEquals(2, request.account.reservedSlots);
		assertEquals(Collections.singletonList(7), request.account.skipItemIds);
		assertTrue(!request.account.members);
	}
}
