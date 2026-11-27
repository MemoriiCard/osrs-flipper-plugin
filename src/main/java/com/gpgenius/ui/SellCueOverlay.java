package com.gpgenius.ui;

import com.gpgenius.GpGeniusConfig;
import com.gpgenius.api.Suggestion;
import com.gpgenius.ge.GrandExchangeWidgets;
import com.gpgenius.suggestion.SuggestionController;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.List;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Point;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * The "sell now" cue: a quiet outline with a small "$" on inventory items the server says are worth
 * selling now (price reached the target, buyers paying it right now, or the margin about to drop).
 * Only urgent cues pulse, and only when the player's highlight pulse is on. Hovering the item shows
 * what to do. It only draws; the player sells by hand.
 */
public class SellCueOverlay extends Overlay
{
	private static final Color LABEL_BACKGROUND = new Color(0, 0, 0, 215);
	/** Non-urgent cues stay this faint so they never compete with the next-step highlight. */
	private static final int QUIET_ALPHA = 150;

	private final GpGeniusConfig config;
	private final SuggestionController suggestions;
	private final GrandExchangeWidgets ge;
	private final Client client;

	@Inject
	SellCueOverlay(GpGeniusConfig config, SuggestionController suggestions, GrandExchangeWidgets ge, Client client)
	{
		this.config = config;
		this.suggestions = suggestions;
		this.ge = ge;
		this.client = client;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
	}

	@Override
	public Dimension render(Graphics2D g)
	{
		Suggestion s = suggestions.getCurrent();
		if (!config.sellCues() || !config.suggestionsEnabled() || s == null || s.sellCues == null || s.sellCues.isEmpty())
		{
			return null;
		}
		long now = System.currentTimeMillis();
		HighlightPalette.Colors colors = config.highlightPalette()
			.colors(Theme.effective(config.theme(), config.highContrast()));
		HighlightStyle style = new HighlightStyle(Math.min(config.highlightThickness(), 3), 0,
			config.highlightOpacity(), 0, config.highlightPulse());
		boolean atGe = ge.isOpen();
		Point mouse = client.getMouseCanvasPosition();
		List<Suggestion.SellCue> cues = s.sellCues;
		for (Suggestion.SellCue cue : cues)
		{
			// At the GE the next-step highlight already points at the item it says to sell.
			if (atGe && s.is(Suggestion.SELL) && s.itemId != null && s.itemId == cue.itemId)
			{
				continue;
			}
			Widget item = ge.visibleInventoryItem(cue.itemId);
			if (item == null || item.isHidden() || item.getBounds() == null)
			{
				continue;
			}
			Rectangle b = item.getBounds();
			Color color = Suggestion.SellCue.DROPPING.equals(cue.kind) ? colors.next : colors.good;
			int alpha = cue.urgent ? style.outlineAlpha(now) : Math.min(QUIET_ALPHA, style.outlineAlpha(now));
			int t = Math.max(1, style.thickness);
			g.setStroke(new BasicStroke(t + 2));
			g.setColor(new Color(0, 0, 0, Math.min(140, alpha)));
			g.drawRect(b.x, b.y, b.width - 1, b.height - 1);
			g.setStroke(new BasicStroke(t));
			g.setColor(Theme.withAlpha(color, alpha));
			g.drawRect(b.x, b.y, b.width - 1, b.height - 1);
			drawBadge(g, b, color);
			if (mouse != null && b.contains(mouse.getX(), mouse.getY()))
			{
				drawLabel(g, b, cue.label(), color);
			}
		}
		return null;
	}

	private static void drawBadge(Graphics2D g, Rectangle b, Color color)
	{
		Font old = g.getFont();
		g.setFont(FontManager.getRunescapeSmallFont());
		FontMetrics fm = g.getFontMetrics();
		int size = Math.max(10, fm.getAscent() + 1);
		int x = b.x + b.width - size;
		int y = b.y;
		g.setColor(LABEL_BACKGROUND);
		g.fillRect(x, y, size, size);
		g.setColor(color);
		g.drawString("$", x + (size - fm.stringWidth("$")) / 2, y + size - 2);
		g.setFont(old);
	}

	private void drawLabel(Graphics2D g, Rectangle target, String text, Color color)
	{
		Font old = g.getFont();
		g.setFont(FontManager.getRunescapeSmallFont());
		FontMetrics fm = g.getFontMetrics();
		int w = fm.stringWidth(text) + 10;
		int h = fm.getHeight() + 4;
		int x = Math.max(2, Math.min(target.x + target.width / 2 - w / 2, client.getCanvasWidth() - w - 2));
		int y = target.y - h - 2;
		if (y < 2)
		{
			y = target.y + target.height + 2;
		}
		g.setColor(LABEL_BACKGROUND);
		g.fillRect(x, y, w, h);
		g.setColor(color);
		g.drawRect(x, y, w, h);
		g.drawString(text, x + 5, y + 2 + fm.getAscent());
		g.setFont(old);
	}
}
