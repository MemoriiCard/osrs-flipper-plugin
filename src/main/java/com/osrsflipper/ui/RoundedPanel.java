package com.osrsflipper.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import javax.swing.JPanel;

/** A card with rounded corners, a hairline border and an optional coloured stripe down the left. */
public class RoundedPanel extends JPanel
{
	static final int RADIUS = 10;
	static final int STRIPE = 3;

	private Color fill = Color.BLACK;
	private Color line;
	private Color stripe;

	public RoundedPanel(LayoutManager layout)
	{
		super(layout);
		setOpaque(false);
	}

	public void setColors(Color fill, Color line)
	{
		this.fill = fill;
		this.line = line;
		repaint();
	}

	/** @param stripe the left accent stripe, or null for none */
	public void setStripe(Color stripe)
	{
		this.stripe = stripe;
		repaint();
	}

	@Override
	protected void paintComponent(Graphics graphics)
	{
		Graphics2D g = (Graphics2D) graphics.create();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		RoundRectangle2D shape = new RoundRectangle2D.Double(0, 0, getWidth() - 1, getHeight() - 1, RADIUS, RADIUS);
		g.setColor(fill);
		g.fill(shape);
		if (stripe != null)
		{
			g.clip(shape);
			g.setColor(stripe);
			g.fillRect(0, 0, STRIPE, getHeight());
			g.setClip(null);
		}
		if (line != null)
		{
			g.setColor(line);
			g.setStroke(new BasicStroke(1f));
			g.draw(shape);
		}
		g.dispose();
		super.paintComponent(graphics);
	}
}
