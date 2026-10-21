package com.osrsflipper.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JComponent;

/** A small rounded tag, such as BUY or PREMIUM: tinted fill, coloured text. */
public class Pill extends JComponent
{
	private String text = "";
	private Color color = Color.WHITE;

	public void set(String text, Color color)
	{
		this.text = text == null ? "" : text;
		this.color = color;
		setVisible(!this.text.isEmpty());
		revalidate();
		repaint();
	}

	@Override
	public Dimension getPreferredSize()
	{
		FontMetrics fm = getFontMetrics(getFont());
		return new Dimension(fm.stringWidth(text) + 14, fm.getHeight() + 4);
	}

	@Override
	public Dimension getMaximumSize()
	{
		return getPreferredSize();
	}

	@Override
	protected void paintComponent(Graphics graphics)
	{
		Graphics2D g = (Graphics2D) graphics.create();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g.setColor(Theme.withAlpha(color, 40));
		g.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
		g.setColor(Theme.withAlpha(color, 140));
		g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, getHeight(), getHeight());
		g.setFont(getFont());
		g.setColor(color);
		FontMetrics fm = g.getFontMetrics();
		g.drawString(text, (getWidth() - fm.stringWidth(text)) / 2, (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
		g.dispose();
	}
}
