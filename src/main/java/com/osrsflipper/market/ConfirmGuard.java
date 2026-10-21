package com.osrsflipper.market;

import com.osrsflipper.api.Suggestion;

/**
 * Catches a mistyped GE offer before it's confirmed: a buy far above the market, a sell far below it,
 * or a price far from the one suggested for this item. When it trips, the plugin moves Confirm off
 * left-click (it stays on the right-click menu), so a slip of the finger can't place the offer.
 */
public final class ConfirmGuard
{
	private ConfirmGuard()
	{
	}

	/**
	 * @param percent how far off a price may be before the guard trips, such as 10
	 * @return why the offer looks wrong, or null if it looks fine
	 */
	public static String check(boolean isSell, int itemId, long price, MarketPrice market, Suggestion suggestion, int percent)
	{
		if (price <= 0 || percent <= 0)
		{
			return null;
		}
		double limit = percent / 100.0;
		if (market != null)
		{
			if (!isSell && market.high != null && market.high > 0 && price > market.high * (1 + limit))
			{
				return String.format("This buy price is %.0f%% above what the item sells for.", (price / (double) market.high - 1) * 100);
			}
			if (isSell && market.low != null && market.low > 0 && price < market.low * (1 - limit))
			{
				return String.format("This sell price is %.0f%% below what buyers pay.", (1 - price / (double) market.low) * 100);
			}
		}
		if (suggestion != null && suggestion.itemId != null && suggestion.itemId == itemId && suggestion.price != null
			&& suggestion.price > 0 && suggestion.is(isSell ? Suggestion.SELL : Suggestion.BUY))
		{
			double off = Math.abs(price / (double) suggestion.price - 1);
			if (off > limit)
			{
				return String.format("This price is %.0f%% away from the suggested %,d.", off * 100, suggestion.price);
			}
		}
		return null;
	}
}
