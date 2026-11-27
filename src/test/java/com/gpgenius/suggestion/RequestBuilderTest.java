package com.gpgenius.suggestion;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.gpgenius.GpGeniusConfig;
import com.gpgenius.api.SuggestionRequest;
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
		assertEquals(5, GpGeniusConfig.CheckIn.values()[0].minutes);
		GpGeniusConfig.CheckIn[] all = GpGeniusConfig.CheckIn.values();
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
		assertEquals(Long.valueOf(0), request.profile.minProfitPerFlip);
		assertEquals(123L, request.account.coins);
		assertEquals(1, request.account.offers.size());
		assertEquals(2, request.account.reservedSlots);
		assertEquals(Collections.singletonList(7), request.account.skipItemIds);
		assertTrue(!request.account.members);
	}

	@Test
	public void limitsAreSentOnlyWhenSet()
	{
		Map<Integer, Long> inventory = Collections.singletonMap(995, 1L);
		SuggestionRequest none = RequestBuilder.build(new RequestBuilder.Settings(), Collections.emptyList(), inventory,
			Collections.emptyMap(), true, Collections.emptyList());
		assertEquals(null, none.profile.coinCap);
		assertEquals(null, none.profile.maxItemPercent);
		assertEquals(null, none.profile.minItemPrice);
		assertEquals(null, none.profile.maxItemPrice);
		assertTrue(none.profile.favouriteItemIds.isEmpty());
		assertTrue(none.profile.categories.isEmpty());

		RequestBuilder.Settings settings = new RequestBuilder.Settings();
		settings.coinCap = 50_000_000;
		settings.maxItemPercent = 250;
		settings.minItemPrice = 1_000;
		settings.maxItemPrice = 5_000_000;
		settings.favouriteItems = "4151, 4151 x 11802";
		settings.categories = Arrays.asList("runes", "potions");
		settings.membersOnly = true;
		SuggestionRequest request = RequestBuilder.build(settings, Collections.emptyList(), inventory,
			Collections.emptyMap(), true, Collections.emptyList());
		assertEquals(Long.valueOf(50_000_000), request.profile.coinCap);
		assertEquals(Integer.valueOf(100), request.profile.maxItemPercent);
		assertEquals(Long.valueOf(1_000), request.profile.minItemPrice);
		assertEquals(Long.valueOf(5_000_000), request.profile.maxItemPrice);
		assertEquals(Arrays.asList(4151, 11802), request.profile.favouriteItemIds);
		assertEquals(Arrays.asList("runes", "potions"), request.profile.categories);
		assertTrue(request.profile.membersOnly);

		// Free-to-play wins if both are somehow on.
		settings.f2pOnly = true;
		request = RequestBuilder.build(settings, Collections.emptyList(), inventory, Collections.emptyMap(), true,
			Collections.emptyList());
		assertTrue(!request.profile.membersOnly);
	}

	@Test
	public void categoryKeysMatchTheServer()
	{
		assertEquals("runes", GpGeniusConfig.Category.RUNES.key());
		assertEquals("skilling", GpGeniusConfig.Category.SKILLING.key());
		assertEquals("other", GpGeniusConfig.Category.OTHER.key());
	}

	@Test
	public void presetRequestsSendTheIdAndLeaveTheMinimumToTheServer()
	{
		RequestBuilder.Settings settings = new RequestBuilder.Settings();
		settings.preset = "overnight";
		SuggestionRequest request = RequestBuilder.build(settings, Collections.emptyList(),
			new HashMap<>(), new HashMap<>(), true, Collections.emptyList());
		assertEquals("overnight", request.preset);
		assertNull(request.profileId);
		assertNull(request.profile.minProfitPerFlip);
		String json = new com.google.gson.Gson().toJson(request);
		assertTrue(json.contains("\"preset\":\"overnight\""));
		assertTrue(!json.contains("min_profit_per_flip"));

		settings.minProfit = 5_000;
		settings.profileId = "abc";
		request = RequestBuilder.build(settings, Collections.emptyList(),
			new HashMap<>(), new HashMap<>(), true, Collections.emptyList());
		assertEquals(Long.valueOf(5_000), request.profile.minProfitPerFlip);
		assertEquals("abc", request.profileId);
	}

	@Test
	public void slotRolesAreLeftOutUnlessSwitchedOn()
	{
		RequestBuilder.Settings settings = new RequestBuilder.Settings();
		SuggestionRequest off = RequestBuilder.build(settings, Collections.emptyList(), new HashMap<>(),
			new HashMap<>(), true, Collections.emptyList());
		assertNull(off.slotRoles);

		settings.slotRoles = true;
		settings.longSlots = 9;
		settings.longTimeframeMinutes = 30;
		settings.longRisk = "medium";
		SuggestionRequest on = RequestBuilder.build(settings, Collections.emptyList(), new HashMap<>(),
			new HashMap<>(), true, Collections.emptyList());
		// Clamped to what the server takes.
		assertEquals(7, on.slotRoles.longSlots);
		assertEquals(60, on.slotRoles.timeframeMinutes);
		assertEquals("medium", on.slotRoles.risk);
	}
}
