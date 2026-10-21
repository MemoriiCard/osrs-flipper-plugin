package com.osrsflipper.ui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JButton;
import javax.swing.border.EmptyBorder;

/** A flat, rounded button drawn in the current theme instead of the look-and-feel's. */
public class ThemedButton extends JButton
{
	public enum Style
	{
		/** Accent fill: the one main action on a card. */
		PRIMARY,
		/** Raised fill: other actions. */
		SECONDARY,
		/** Text only, until hovered. */
		GHOST,
		/** One option of a segmented control; "selected" is drawn with the accent. */
		SEGMENT,
	}

	private final Style style;
	private Theme theme = Theme.GRAND_EXCHANGE;
	private boolean hover;
	private boolean selected;

	public ThemedButton(String text, String tooltip, Style style)
	{
		super(text);
		this.style = style;
		setFocusable(false);
		setContentAreaFilled(false);
		setBorderPainted(false);
		setFocusPainted(false);
		setOpaque(false);
		setRolloverEnabled(true);
		setBorder(new EmptyBorder(4, 10, 4, 10));
		setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		if (tooltip != null)
		{
			setToolTipText(tooltip);
		}
		addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseEntered(MouseEvent e)
			{
				hover = true;
				repaint();
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				hover = false;
				repaint();
			}
		});
	}

	public void setTheme(Theme theme)
	{
		this.theme = theme;
		repaint();
	}

	/** For segments: whether this is the chosen option. */
	@Override
	public void setSelected(boolean selected)
	{
		this.selected = selected;
		repaint();
	}

	@Override
	public Dimension getPreferredSize()
	{
		FontMetrics fm = getFontMetrics(getFont());
		return new Dimension(fm.stringWidth(getText()) + 20, fm.getHeight() + 8);
	}

	@Override
	protected void paintComponent(Graphics graphics)
	{
		Graphics2D g = (Graphics2D) graphics.create();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		Color fill = null;
		Color text = theme.text;
		boolean on = isEnabled();
		switch (style)
		{
			case PRIMARY:
				fill = hover && on ? brighter(theme.accent) : theme.accent;
				text = theme.onAccent;
				break;
			case SECONDARY:
				fill = hover && on ? theme.border : theme.raised;
				break;
			case SEGMENT:
				fill = selected ? Theme.withAlpha(theme.accent, 48) : hover ? theme.raised : null;
				text = selected ? theme.accent : theme.muted;
				break;
			case GHOST:
			default:
				fill = hover && on ? theme.raised : null;
				text = theme.muted;
				break;
		}
		if (!on && style != Style.SEGMENT)
		{
			text = Theme.withAlpha(text, 110);
			if (fill != null)
			{
				fill = Theme.withAlpha(fill, 110);
			}
		}
		if (fill != null)
		{
			g.setColor(fill);
			g.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
		}
		g.setFont(getFont());
		g.setColor(text);
		FontMetrics fm = g.getFontMetrics();
		int x = (getWidth() - fm.stringWidth(getText())) / 2;
		int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
		g.drawString(getText(), x, y);
		g.dispose();
	}

	private static Color brighter(Color c)
	{
		return new Color(Math.min(255, c.getRed() + 24), Math.min(255, c.getGreen() + 24), Math.min(255, c.getBlue() + 24));
	}
}
