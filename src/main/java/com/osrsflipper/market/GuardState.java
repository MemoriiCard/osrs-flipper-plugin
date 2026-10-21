package com.osrsflipper.market;

import javax.inject.Singleton;
import lombok.Getter;
import lombok.Setter;

/** Why the offer being set up looks wrong, or null. Written on each game tick, read when drawing and building menus. */
@Singleton
public class GuardState
{
	@Getter
	@Setter
	private volatile String reason;
}
