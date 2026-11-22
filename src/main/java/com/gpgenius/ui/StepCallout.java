package com.gpgenius.ui;

import com.gpgenius.api.Suggestion;
import com.gpgenius.suggestion.NextStep;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * The short label drawn next to the highlight, saying exactly what to click or type. Text only; the
 * player does every click and keypress.
 */
public final class StepCallout
{
	private StepCallout()
	{
	}

	/** The label for a highlighted step, or null for none. */
	public static String of(NextStep.Step step, Suggestion s)
	{
		if (step == null || s == null)
		{
			return null;
		}
		String item = s.name == null ? "the item" : s.name;
		switch (step)
		{
			case BUY_BUTTON:
				return "Click Buy";
			case SEARCH_RESULT:
				return "Click " + item;
			case INVENTORY_ITEM:
				return "Click " + item + " to sell";
			case SLOT:
				return "Abort this offer: " + item;
			case SET_PRICE:
				return s.price == null ? "Set the price" : "Set price to " + coins(s.price);
			case SET_QUANTITY:
				return s.quantity == null ? "Set the quantity" : "Set quantity to " + coins(s.quantity);
			case CONFIRM:
				return "Click Confirm";
			case NONE:
			default:
				return null;
		}
	}

	/** The label over the open chatbox price or quantity box. */
	public static String prompt(boolean price, long value)
	{
		if (value <= 0)
		{
			return null;
		}
		return "Type " + coins(value) + (price ? " gp" : "") + ", then press Enter";
	}

	static String coins(long value)
	{
		return NumberFormat.getIntegerInstance(Locale.US).format(value);
	}
}
