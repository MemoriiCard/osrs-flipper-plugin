package com.osrsflipper.ge;

import com.osrsflipper.suggestion.NextStep;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.GrandExchangeOffer;
import net.runelite.api.GrandExchangeOfferState;
import net.runelite.api.ItemComposition;
import net.runelite.api.VarClientInt;
import net.runelite.api.VarClientStr;
import net.runelite.api.VarPlayer;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.ComponentID;
import net.runelite.api.widgets.Widget;

/**
 * Reads the Grand Exchange interface: which screen is open, the offer being set up, and the widgets
 * to highlight. Widget child indices follow the GE interface layout as of late 2026.
 *
 * <p>All methods must be called on the client thread.</p>
 */
@Singleton
public class GrandExchangeWidgets
{
	/** GE offer-setup price varp; 64-bit since the 2026-09-30 max-cash update. */
	private static final int SETUP_PRICE_VARP = 5753;
	/** The GE-side inventory shown while the exchange is open. */
	private static final int GE_INVENTORY_GROUP = 467;

	private static final int SLOT_CHILD_OFFSET = 7;
	private static final int OFFER_CONTAINER_CHILD = 26;
	private static final int SET_QUANTITY_BUTTON = 51;
	private static final int SET_PRICE_BUTTON = 54;
	private static final int CONFIRM_BUTTON = 58;

	/** Chatbox numeric prompt modes: 32-bit (quantity) and 64-bit (price). */
	private static final int INPUT_MODE_INT = 7;
	private static final int INPUT_MODE_LONG = 30;

	private final Client client;

	@Inject
	public GrandExchangeWidgets(Client client)
	{
		this.client = client;
	}

	public boolean isOpen()
	{
		return client.getWidget(InterfaceID.GE_OFFERS, SLOT_CHILD_OFFSET) != null;
	}

	/** Zero-based slot being viewed or set up, or -1 on the home screen. */
	public int openSlot()
	{
		return client.getVarbitValue(VarbitID.GE_SELECTEDSLOT) - 1;
	}

	public boolean isSetupOpen()
	{
		Widget confirm = offerChild(CONFIRM_BUTTON);
		return isOpen() && openSlot() != -1 && confirm != null && !confirm.isHidden();
	}

	public boolean isSetupSell()
	{
		return client.getVarbitValue(VarbitID.GE_NEWOFFER_TYPE) == 1;
	}

	public int setupItemId()
	{
		return client.getVarpValue(VarPlayer.CURRENT_GE_ITEM);
	}

	public long setupPrice()
	{
		return client.getVarpLongValue(SETUP_PRICE_VARP);
	}

	public int setupQuantity()
	{
		return client.getVarbitValue(VarbitID.GE_NEWOFFER_QUANTITY);
	}

	public Widget slot(int slot)
	{
		return client.getWidget(InterfaceID.GE_OFFERS, SLOT_CHILD_OFFSET + slot);
	}

	/** The "Buy" button shown on an empty slot. */
	public Widget buyButton(int slot)
	{
		Widget w = slot(slot);
		return w == null ? null : w.getChild(0);
	}

	public Widget setPriceButton()
	{
		return offerChild(SET_PRICE_BUTTON);
	}

	public Widget setQuantityButton()
	{
		return offerChild(SET_QUANTITY_BUTTON);
	}

	public Widget confirmButton()
	{
		return offerChild(CONFIRM_BUTTON);
	}

	/** The item in the GE-side inventory, noted or not. */
	public Widget inventoryItem(int unnotedItemId)
	{
		Widget inventory = client.getWidget(GE_INVENTORY_GROUP, 0);
		if (inventory == null || inventory.getDynamicChildren() == null)
		{
			return null;
		}
		for (Widget item : inventory.getDynamicChildren())
		{
			if (item == null || item.isHidden() || item.getItemId() <= 0)
			{
				continue;
			}
			if (item.getItemId() == unnotedItemId)
			{
				return item;
			}
			ItemComposition def = client.getItemDefinition(item.getItemId());
			if (def.getNote() != -1 && def.getLinkedNoteId() == unnotedItemId)
			{
				return item;
			}
		}
		return null;
	}

	/** The suggested item in the chatbox item search, if listed. */
	public Widget searchResult(int itemId, String name)
	{
		Widget results = client.getWidget(ComponentID.CHATBOX_GE_SEARCH_RESULTS);
		if (results == null || results.isHidden() || results.getDynamicChildren() == null)
		{
			return null;
		}
		String label = "<col=ff9040>" + name + "</col>";
		for (Widget w : results.getDynamicChildren())
		{
			if (w != null && label.equals(w.getName()))
			{
				return w;
			}
		}
		// The "previous search" row shows the item icon without a name.
		Widget previous = results.getChild(3);
		return previous != null && previous.getItemId() == itemId ? previous : null;
	}

	public int firstEmptySlot()
	{
		GrandExchangeOffer[] offers = client.getGrandExchangeOffers();
		for (int i = 0; i < offers.length; i++)
		{
			if (offers[i] == null || offers[i].getState() == GrandExchangeOfferState.EMPTY)
			{
				Widget slot = slot(i);
				if (slot != null && !slot.isHidden())
				{
					return i;
				}
			}
		}
		return -1;
	}

	/** A snapshot of the GE screen for {@link NextStep}. */
	public NextStep.Screen screen()
	{
		NextStep.Screen screen = new NextStep.Screen();
		screen.open = isOpen();
		if (!screen.open)
		{
			return screen;
		}
		screen.openSlot = openSlot();
		screen.setupOpen = isSetupOpen();
		if (screen.setupOpen)
		{
			screen.setupSell = isSetupSell();
			screen.setupItemId = setupItemId();
			screen.setupPrice = setupPrice();
			screen.setupQuantity = setupQuantity();
			Widget results = client.getWidget(ComponentID.CHATBOX_GE_SEARCH_RESULTS);
			screen.searchOpen = results != null && !results.isHidden();
		}
		screen.firstEmptySlot = firstEmptySlot();
		return screen;
	}

	/** Whether the chatbox is asking for an offer's price or quantity. */
	public PromptType openPrompt()
	{
		int mode = client.getVarcIntValue(VarClientInt.INPUT_TYPE);
		Widget title = client.getWidget(ComponentID.CHATBOX_TITLE);
		if ((mode != INPUT_MODE_INT && mode != INPUT_MODE_LONG) || title == null || !isSetupOpen())
		{
			return PromptType.NONE;
		}
		String text = title.getText();
		if ("Set a price for each item:".equals(text))
		{
			return PromptType.PRICE;
		}
		if ("How many do you wish to buy?".equals(text) || "How many do you wish to sell?".equals(text))
		{
			return PromptType.QUANTITY;
		}
		return PromptType.NONE;
	}

	/**
	 * Puts a value into the open price or quantity prompt. The player still presses Enter to accept it.
	 */
	public void fillPrompt(long value)
	{
		Widget input = client.getWidget(ComponentID.CHATBOX_FULL_INPUT);
		if (input == null)
		{
			return;
		}
		input.setText(value + "*");
		client.setVarcStrValue(VarClientStr.INPUT_TEXT, String.valueOf(value));
	}

	private Widget offerChild(int index)
	{
		Widget container = client.getWidget(InterfaceID.GE_OFFERS, OFFER_CONTAINER_CHILD);
		return container == null ? null : container.getChild(index);
	}

	public enum PromptType
	{
		NONE,
		PRICE,
		QUANTITY
	}
}
