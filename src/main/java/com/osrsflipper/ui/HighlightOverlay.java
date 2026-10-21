package com.osrsflipper.ui;

import com.osrsflipper.OsrsFlipperConfig;
import com.osrsflipper.api.Suggestion;
import com.osrsflipper.ge.GrandExchangeWidgets;
import com.osrsflipper.suggestion.NextStep;
import com.osrsflipper.suggestion.SuggestionController;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import javax.inject.Inject;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * Outlines the one GE slot, button or item the player should click next for the current suggestion.
 * It only draws; the player does every click.
 */
public class HighlightOverlay extends Overlay
{
	private static final Color FILL = new Color(0x4C, 0xBB, 0x17, 50);
	private static final Color BORDER = new Color(0x4C, 0xBB, 0x17, 200);
	private static final Color ABORT_FILL = new Color(0xE0, 0x4B, 0x4B, 50);
	private static final Color ABORT_BORDER = new Color(0xE0, 0x4B, 0x4B, 200);
	/** The clickable part of the set-price and set-quantity buttons. */
	private static final Rectangle SET_BUTTON_AREA = new Rectangle(1, 6, 33, 23);

	private final OsrsFlipperConfig config;
	private final SuggestionController suggestions;
	private final GrandExchangeWidgets ge;

	@Inject
	HighlightOverlay(OsrsFlipperConfig config, SuggestionController suggestions, GrandExchangeWidgets ge)
	{
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

	private static void draw(Graphics2D g, Widget widget, Rectangle area, boolean abort)
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
		g.setColor(abort ? ABORT_FILL : FILL);
		g.fill(bounds);
		g.setColor(abort ? ABORT_BORDER : BORDER);
		g.setStroke(new BasicStroke(2));
		g.draw(bounds);
	}
}
