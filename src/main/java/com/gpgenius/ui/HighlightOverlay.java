package com.gpgenius.ui;

import com.gpgenius.GpGeniusConfig;
import com.gpgenius.api.Suggestion;
import com.gpgenius.ge.GrandExchangeWidgets;
import com.gpgenius.market.GuardState;
import com.gpgenius.market.PriceCache;
import com.gpgenius.market.SlotColor;
import com.gpgenius.suggestion.NextStep;
import com.gpgenius.suggestion.SuggestionController;
import java.awt.BasicStroke;
import java.awt.Font;
import java.awt.geom.Area;
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
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * Outlines the one GE slot, button, item or typing box the player should click next for the current
 * suggestion, says what to do there, colours active offers by whether they'll still fill, and flags a
 * mistyped price on Confirm. Colours, size, strength and labels follow the player's settings.
 * It only draws; the player does every click and keypress.
 */
public class HighlightOverlay extends Overlay
{
	/** The clickable part of the set-price and set-quantity buttons. */
	private static final Rectangle SET_BUTTON_AREA = new Rectangle(1, 6, 33, 23);
	private static final Color WARNING_TEXT = new Color(0xFF, 0x6B, 0x6B);
	private static final Color LABEL_BACKGROUND = new Color(0, 0, 0, 215);
	/** How dark the spotlight makes everything else. */
	private static final int SPOTLIGHT_ALPHA = 120;
	private static final float[] DASH = {6f, 4f};

	private final GpGeniusConfig config;
	private final SuggestionController suggestions;
	private final GrandExchangeWidgets ge;
	private final Client client;
	private final PriceCache prices;
	private final GuardState guard;

	@Inject
	HighlightOverlay(GpGeniusConfig config, SuggestionController suggestions, GrandExchangeWidgets ge,
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
		long now = System.currentTimeMillis();
		HighlightPalette.Colors colors = config.highlightPalette()
			.colors(Theme.effective(config.theme(), config.highContrast()));
		HighlightStyle style = new HighlightStyle(config.highlightThickness(), config.highlightPadding(),
			config.highlightOpacity(), config.highlightFill(), config.highlightPulse());

		if (config.slotColors() && ge.openSlot() == -1)
		{
			drawSlotColors(g, colors, style);
		}
		String warning = config.confirmGuard() ? guard.getReason() : null;
		if (warning != null && ge.isSetupOpen())
		{
			drawWarning(g, ge.confirmButton(), warning, colors.bad);
		}
		Suggestion s = suggestions.getCurrent();
		if (!config.highlightOffers() || s == null)
		{
			return null;
		}
		NextStep.Screen screen = ge.screen();

		// The price or quantity box is open: the field to type in is the next thing.
		GrandExchangeWidgets.PromptType prompt = ge.openPrompt();
		if (prompt != GrandExchangeWidgets.PromptType.NONE)
		{
			if (config.highlightPrompt())
			{
				boolean price = prompt == GrandExchangeWidgets.PromptType.PRICE;
				long value = NextStep.promptValue(s, screen, price);
				if (value > 0)
				{
					draw(g, ge.promptInput(), null, colors.next, null, StepCallout.prompt(price, value), style, now);
				}
			}
			return null;
		}

		NextStep.Step step = NextStep.of(s, screen);
		String label = StepCallout.of(step, s);
		switch (step)
		{
			case BUY_BUTTON:
				draw(g, ge.buyButton(screen.firstEmptySlot), null, colors.next, null, label, style, now);
				break;
			case SEARCH_RESULT:
				draw(g, ge.searchResult(s.itemId, s.name), null, colors.next, null, label, style, now);
				break;
			case INVENTORY_ITEM:
				draw(g, ge.inventoryItem(s.itemId), null, colors.next, null, label, style, now);
				break;
			case SLOT:
				// An upkeep fix is drawn in the colour of what follows (buy or sell); a plain abort in red.
				Color slotColor = s.fix() == null || Suggestion.FIX_CLOSE.equals(s.fix()) ? colors.abort
					: Suggestion.FIX_RAISE.equals(s.fix()) ? colors.next : colors.good;
				draw(g, ge.slot(s.slot), null, slotColor, StepCallout.slotBadge(s), label, style, now);
				break;
			case SET_PRICE:
				draw(g, ge.setPriceButton(), SET_BUTTON_AREA, colors.next, null, label, style, now);
				break;
			case SET_QUANTITY:
				draw(g, ge.setQuantityButton(), SET_BUTTON_AREA, colors.next, null, label, style, now);
				break;
			case CONFIRM:
				draw(g, ge.confirmButton(), null, colors.next, null, label, style, now);
				break;
			case NONE:
			default:
				break;
		}
		return null;
	}

	private void drawSlotColors(Graphics2D g, HighlightPalette.Colors colors, HighlightStyle style)
	{
		GrandExchangeOffer[] offers = client.getGrandExchangeOffers();
		int width = Math.max(2, Math.min(style.thickness, 4));
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
			Rectangle b = slot.getBounds();
			boolean good = color == SlotColor.COMPETITIVE;
			g.setColor(Theme.withAlpha(good ? colors.good : colors.bad, 210));
			// Solid while it'll fill, dashed with a "!" once outbid, so it reads without colour.
			g.setStroke(good
				? new BasicStroke(width)
				: new BasicStroke(width, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, DASH, 0f));
			g.drawRect(b.x + width / 2, b.y + width / 2, b.width - width, b.height - width);
			if (!good)
			{
				drawBadge(g, b, "!", colors.bad);
			}
		}
	}

	private static void drawWarning(Graphics2D g, Widget confirm, String text, Color color)
	{
		if (confirm == null || confirm.isHidden() || confirm.getBounds() == null)
		{
			return;
		}
		Rectangle b = confirm.getBounds();
		g.setColor(color);
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

	private void draw(Graphics2D g, Widget widget, Rectangle part, Color color, String badge, String label,
		HighlightStyle style, long now)
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
		if (part != null)
		{
			bounds = new Rectangle(bounds.x + part.x, bounds.y + part.y, part.width, part.height);
		}
		Rectangle area = style.area(bounds);

		if (config.highlightSpotlight())
		{
			Area dim = new Area(new Rectangle(0, 0, client.getCanvasWidth(), client.getCanvasHeight()));
			dim.subtract(new Area(area));
			g.setColor(new Color(0, 0, 0, SPOTLIGHT_ALPHA));
			g.fill(dim);
		}

		int fill = style.fillAlpha(now);
		if (fill > 0)
		{
			g.setColor(Theme.withAlpha(color, fill));
			g.fill(area);
		}
		// A thin dark edge first keeps a light outline visible on light backgrounds.
		int t = style.thickness;
		g.setColor(new Color(0, 0, 0, Math.min(160, style.outlineAlpha(now))));
		g.setStroke(new BasicStroke(t + 2));
		g.drawRect(area.x + t / 2, area.y + t / 2, area.width - t, area.height - t);
		g.setColor(Theme.withAlpha(color, style.outlineAlpha(now)));
		g.setStroke(new BasicStroke(t));
		g.drawRect(area.x + t / 2, area.y + t / 2, area.width - t, area.height - t);
		if (badge != null)
		{
			drawBadge(g, area, badge, color);
		}
		if (label != null && config.highlightLabel())
		{
			drawLabel(g, area, label, color);
		}
	}

	/** A small marker in the top-right corner, so meaning doesn't rest on colour alone. */
	private static void drawBadge(Graphics2D g, Rectangle b, String mark, Color color)
	{
		Font old = g.getFont();
		g.setFont(FontManager.getRunescapeBoldFont());
		FontMetrics fm = g.getFontMetrics();
		int size = Math.max(fm.getHeight(), 14);
		int x = b.x + b.width - size - 2;
		int y = b.y + 2;
		g.setColor(LABEL_BACKGROUND);
		g.fillRect(x, y, size, size);
		g.setColor(color);
		g.drawRect(x, y, size, size);
		g.drawString(mark, x + (size - fm.stringWidth(mark)) / 2 + 1, y + (size + fm.getAscent()) / 2 - 2);
		g.setFont(old);
	}

	/** The "what to do" label, above the target, or below it near the top of the screen. */
	private void drawLabel(Graphics2D g, Rectangle target, String text, Color color)
	{
		Font old = g.getFont();
		Font base = FontManager.getRunescapeBoldFont();
		g.setFont(base.deriveFont(base.getSize2D() * config.textSize().scale));
		FontMetrics fm = g.getFontMetrics();
		int padX = 6;
		int padY = 3;
		int w = fm.stringWidth(text) + 2 * padX;
		int h = fm.getHeight() + 2 * padY;
		int x = target.x + target.width / 2 - w / 2;
		x = Math.max(2, Math.min(x, client.getCanvasWidth() - w - 2));
		int y = target.y - h - 4;
		if (y < 2)
		{
			y = target.y + target.height + 4;
		}
		g.setColor(LABEL_BACKGROUND);
		g.fillRect(x, y, w, h);
		g.setColor(color);
		g.setStroke(new BasicStroke(1));
		g.drawRect(x, y, w, h);
		g.drawString(text, x + padX, y + padY + fm.getAscent());
		g.setFont(old);
	}
}
