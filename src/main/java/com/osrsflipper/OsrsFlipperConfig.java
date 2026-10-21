package com.osrsflipper;

import java.awt.event.KeyEvent;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Keybind;
import net.runelite.client.config.Range;

@ConfigGroup(OsrsFlipperConfig.GROUP)
public interface OsrsFlipperConfig extends Config
{
	String GROUP = "osrsflipper";

	enum RiskLevel
	{
		LOW,
		MEDIUM,
		HIGH
	}

	enum ViewMode
	{
		SIMPLE,
		ADVANCED
	}

	@ConfigSection(
		name = "Suggestions",
		description = "What kind of flips to suggest",
		position = 0
	)
	String suggestionSection = "suggestions";

	@ConfigSection(
		name = "Offer setup",
		description = "Highlights and hotkeys on the Grand Exchange",
		position = 1
	)
	String offerSection = "offers";

	@ConfigSection(
		name = "Tracking",
		description = "Flip tracking and the side panel",
		position = 2
	)
	String trackingSection = "tracking";

	@ConfigSection(
		name = "Connection",
		description = "Where the plugin gets suggestions from",
		position = 3,
		closedByDefault = true
	)
	String connectionSection = "connection";

	@ConfigItem(
		keyName = "viewMode",
		name = "Panel view",
		description = "Simple shows the next step and your profit. Advanced adds the reasoning, ratings, 'why this item' and every recent flip.",
		position = -1
	)
	default ViewMode viewMode()
	{
		return ViewMode.SIMPLE;
	}

	@ConfigItem(
		keyName = "suggestionsEnabled",
		name = "Get suggestions",
		description = "Send your GE offers, coins and flip settings to the OSRS Flipper server to get flip suggestions.",
		section = suggestionSection,
		position = 0
	)
	default boolean suggestionsEnabled()
	{
		return true;
	}

	@ConfigItem(
		keyName = "riskLevel",
		name = "Risk level",
		description = "Low sticks to steady, high-volume items. High allows jumpier items with bigger margins.",
		section = suggestionSection,
		position = 1
	)
	default RiskLevel riskLevel()
	{
		return RiskLevel.MEDIUM;
	}

	@Range(min = 5, max = 10080)
	@ConfigItem(
		keyName = "timeframeMinutes",
		name = "Check offers every (minutes)",
		description = "How often you come back to adjust offers. Short times suggest fast flips; long times suggest patient offers with wider margins.",
		section = suggestionSection,
		position = 2
	)
	default int timeframeMinutes()
	{
		return 30;
	}

	@Range(min = 0, max = 7)
	@ConfigItem(
		keyName = "reservedSlots",
		name = "Reserved GE slots",
		description = "Slots to keep free for your own trades.",
		section = suggestionSection,
		position = 3
	)
	default int reservedSlots()
	{
		return 0;
	}

	@ConfigItem(
		keyName = "sellOnly",
		name = "Sell-only mode",
		description = "Only suggest selling what you already bought.",
		section = suggestionSection,
		position = 4
	)
	default boolean sellOnly()
	{
		return false;
	}

	@ConfigItem(
		keyName = "f2pOnly",
		name = "Free-to-play items only",
		description = "Only suggest items free-to-play players can trade, even on members worlds.",
		section = suggestionSection,
		position = 5
	)
	default boolean f2pOnly()
	{
		return false;
	}

	@ConfigItem(
		keyName = "minProfit",
		name = "Minimum profit per flip",
		description = "Skip suggestions expected to make less than this many coins.",
		section = suggestionSection,
		position = 6
	)
	default int minProfit()
	{
		return 0;
	}

	@ConfigItem(
		keyName = "blockedItems",
		name = "Blocked item IDs",
		description = "Comma-separated item IDs never to suggest.",
		section = suggestionSection,
		position = 7
	)
	default String blockedItems()
	{
		return "";
	}

	@ConfigItem(
		keyName = "allowSharedItems",
		name = "Same items on my alts",
		description = "Let your other game accounts be suggested the same item at the same time. Off spreads them across different items so they don't outbid each other.",
		section = suggestionSection,
		position = 8
	)
	default boolean allowSharedItems()
	{
		return false;
	}

	@ConfigItem(
		keyName = "apiUrl",
		name = "Server URL",
		description = "OSRS Flipper server address. Must start with https://. Leave as is unless told otherwise.",
		section = connectionSection,
		position = 0
	)
	default String apiUrl()
	{
		return "http://localhost:8000";
	}

	@ConfigItem(
		keyName = "highlightOffers",
		name = "Highlight next step",
		description = "Highlight the GE slot, button or item to click next for the current suggestion.",
		section = offerSection,
		position = 0
	)
	default boolean highlightOffers()
	{
		return true;
	}

	@ConfigItem(
		keyName = "quickSetKeybind",
		name = "Set price/quantity key",
		description = "While the GE price or quantity box is open, press this to fill in the suggested value.",
		section = offerSection,
		position = 1
	)
	default Keybind quickSetKeybind()
	{
		return new Keybind(KeyEvent.VK_E, 0);
	}

	@ConfigItem(
		keyName = "skipKeybind",
		name = "Skip suggestion key",
		description = "Skip the current suggestion and get another.",
		section = offerSection,
		position = 2
	)
	default Keybind skipKeybind()
	{
		return Keybind.NOT_SET;
	}

	@ConfigItem(
		keyName = "trackTrades",
		name = "Track GE trades",
		description = "Record your Grand Exchange trades on this computer to work out flip profit.",
		section = trackingSection,
		position = 0
	)
	default boolean trackTrades()
	{
		return true;
	}

	@ConfigItem(
		keyName = "syncTrades",
		name = "Sync flips to website",
		description = "Upload your tracked GE trades to your OSRS Flipper account so you can see history and charts on the website. Needs sign-in.",
		section = trackingSection,
		position = 1
	)
	default boolean syncTrades()
	{
		return true;
	}

	@ConfigItem(
		keyName = "recentFlipCount",
		name = "Recent flips shown",
		description = "How many recent flips the side panel lists in the advanced view.",
		section = trackingSection,
		position = 2
	)
	default int recentFlipCount()
	{
		return 25;
	}
}
