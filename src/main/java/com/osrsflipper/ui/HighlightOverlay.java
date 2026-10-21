package com.osrsflipper.ui;

import com.osrsflipper.OsrsFlipperConfig;
import com.osrsflipper.api.Suggestion;
import com.osrsflipper.ge.GrandExchangeWidgets;
import com.osrsflipper.market.GuardState;
import com.osrsflipper.market.PriceCache;
import com.osrsflipper.market.SlotColor;
import com.osrsflipper.suggestion.NextStep;
import com.osrsflipper.suggestion.SuggestionController;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.FontMetrics;
import java.awt.Rectangle;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GrandExchangeOffer;
import net.runelite.api.GrandExchangeOfferState;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * Outlines the one GE slot, button or item the player should click next for the current suggestion,
 * colours active offers by whether they'll still fill, and flags a mistyped price on Confirm.
 * It only draws; the player does every click.
 */
public class HighlightOverlay extends Overlay
{
	/** The clickable part of the set-price and set-quantity buttons. */
	private static final Rectangle SET_BUTTON_AREA = new Rectangle(1, 6, 33, 23);
	private static final Color COMPETITIVE = new Color(0x4C, 0xBB, 0x17, 170);
	private static final Color OUTBID = new Color(0xE0, 0x4B, 0x4B, 200);
	private static final Color WARNING_TEXT = new Color(0xFF, 0x6B, 0x6B);

	private final OsrsFlipperConfig config;
	private final SuggestionController suggestions;
	private final GrandExchangeWidgets ge;
	private final Client client;
	private final PriceCache prices;
	private final GuardState guard;

	@Inject
	HighlightOverlay(OsrsFlipperConfig config, SuggestionController suggestions, GrandExchangeWidgets ge,
		Client client, PriceCache prices, GuardState guard)
	{
		this.client = client;
		this.prices = prices;
		this.guard = guard;
		this.config = config;
		this.suggestions = suggestions;
		this.ge = ge;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
		setPriority(PRIORITY_HIGH);
	}

	@Override
	public Dimension render(Graphics2D g)
	{
		if (!ge.isOpen())
		{
			return null;
		}
		if (config.slotColors() && ge.openSlot() == -1)
		{
			drawSlotColors(g);
		}
		String warning = config.confirmGuard() ? guard.getReason() : null;
		if (warning != null && ge.isSetupOpen())
		{
			drawWarning(g, ge.confirmButton(), warning);
		}
		Suggestion s = suggestions.getCurrent();
		if (!config.highlightOffers() || s == null)
		{
			return null;
		}
		NextStep.Screen screen = ge.screen();
		NextStep.Step step = NextStep.of(s, screen);
		switch (step)
		{
			case BUY_BUTTON:
				draw(g, ge.buyButton(screen.firstEmptySlot), null, false);
				break;
			case SEARCH_RESULT:
				draw(g, ge.searchResult(s.itemId, s.name), null, false);
				break;
			case INVENTORY_ITEM:
				draw(g, ge.inventoryItem(s.itemId), null, false);
				break;
			case SLOT:
				draw(g, ge.slot(s.slot), null, true);
				break;
			case SET_PRICE:
				draw(g, ge.setPriceButton(), SET_BUTTON_AREA, false);
				break;
			case SET_QUANTITY:
				draw(g, ge.setQuantityButton(), SET_BUTTON_AREA, false);
				break;
			case CONFIRM:
				draw(g, ge.confirmButton(), null, false);
				break;
			case NONE:
			default:
				break;
		}
		return null;
	}

	private void drawSlotColors(Graphics2D g)
	{
		GrandExchangeOffer[] offers = client.getGrandExchangeOffers();
		for (int i = 0; i < offers.length; i++)
		{
			GrandExchangeOffer offer = offers[i];
			if (offer == null)
			{
				continue;
			}
			GrandExchangeOfferState state = offer.getState();
			boolean buying = state == GrandExchangeOfferState.BUYING;
			if (!buying && state != GrandExchangeOfferState.SELLING)
			{
				continue;
			}
			SlotColor color = SlotColor.of(buying, offer.getPrice(), prices.get(offer.getItemId()));
			Widget slot = ge.slot(i);
			if (color == SlotColor.NONE || slot == null || slot.isHidden() || slot.getBounds() == null)
			{
				continue;
			}
			g.setColor(color == SlotColor.COMPETITIVE ? COMPETITIVE : OUTBID);
			g.setStroke(new BasicStroke(2));
			Rectangle b = slot.getBounds();
			g.drawRect(b.x + 1, b.y + 1, b.width - 2, b.height - 2);
		}
	}

	private static void drawWarning(Graphics2D g, Widget confirm, String text)
	{
		if (confirm == null || confirm.isHidden() || confirm.getBounds() == null)
		{
			return;
		}
		Rectangle b = confirm.getBounds();
		g.setColor(OUTBID);
		g.setStroke(new BasicStroke(2));
		g.draw(b);
		FontMetrics fm = g.getFontMetrics();
		String line = text + " Right-click to confirm anyway.";
		int x = b.x + b.width / 2 - fm.stringWidth(line) / 2;
		int y = b.y - 6;
		g.setColor(Color.BLACK);
		g.drawString(line, x + 1, y + 1);
		g.setColor(WARNING_TEXT);
		g.drawString(line, x, y);
	}

	private void draw(Graphics2D g, Widget widget, Rectangle area, boolean abort)
	{
		if (widget == null || widget.isHidden())
		{
			return;
		}
		Rectangle bounds = widget.getBounds();
		if (bounds == null)
		{
			return;
		}
		if (area != null)
		{
			bounds = new Rectangle(bounds.x + area.x, bounds.y + area.y, area.width, area.height);
		}
		// The suggestion highlight follows the theme; aborts stay red in every theme.
		Color color = abort ? config.theme().loss : config.theme().accent;
		g.setColor(Theme.withAlpha(color, 50));
		g.fill(bounds);
		g.setColor(Theme.withAlpha(color, 210));
		g.setStroke(new BasicStroke(2));
		g.draw(bounds);
	}
}
