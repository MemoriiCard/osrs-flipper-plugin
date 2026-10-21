package com.osrsflipper;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(OsrsFlipperConfig.GROUP)
public interface OsrsFlipperConfig extends Config
{
	String GROUP = "osrsflipper";

	@ConfigItem(
		keyName = "trackTrades",
		name = "Track GE trades",
		description = "Record your Grand Exchange trades on this computer to work out flip profit.",
		position = 0
	)
	default boolean trackTrades()
	{
		return true;
	}

	@ConfigItem(
		keyName = "recentFlipCount",
		name = "Recent flips shown",
		description = "How many recent flips the side panel lists.",
		position = 1
	)
	default int recentFlipCount()
	{
		return 25;
	}
}
