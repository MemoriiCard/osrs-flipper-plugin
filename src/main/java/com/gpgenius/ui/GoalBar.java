package com.gpgenius.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.swing.JComponent;

/** A thin rounded bar showing how far along the goal is, in the theme's colours. */
public class GoalBar extends JComponent
{
	private double fraction;
	private Color track = Color.DARK_GRAY;
	private Color fill = Color.GREEN;

	public void set(double fraction, Color track, Color fill)
	{
		this.fraction = Math.max(0, Math.min(1, fraction));
		this.track = track;
		this.fill = fill;
		setToolTipText(Math.round(this.fraction * 100) + "% of the way there");
		getAccessibleContext().setAccessibleDescription(getToolTipText());
		repaint();
	}

	@Override
	public Dimension getPreferredSize()
	{
		return new Dimension(150, 8);
	}

	@Override
	public Dimension getMaximumSize()
	{
		return new Dimension(Integer.MAX_VALUE, 8);
	}

	@Override
	protected void paintComponent(Graphics graphics)
	{
		Graphics2D g = (Graphics2D) graphics.create();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		int h = getHeight();
		g.setColor(track);
		g.fillRoundRect(0, 0, getWidth(), h, h, h);
		int w = (int) Math.round(getWidth() * fraction);
		if (w > 0)
		{
			g.setColor(fill);
			g.fillRoundRect(0, 0, Math.max(w, h), h, h, h);
		}
		g.dispose();
	}

	@Override
	public AccessibleContext getAccessibleContext()
	{
		if (accessibleContext == null)
		{
			accessibleContext = new AccessibleJComponent()
			{
				@Override
				public AccessibleRole getAccessibleRole()
				{
					return AccessibleRole.PROGRESS_BAR;
				}
			};
		}
		return accessibleContext;
	}
}
