package com.osrsflipper.ui;

import com.osrsflipper.api.PriceHistory;
import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import javax.swing.JPanel;
import javax.swing.ToolTipManager;

/**
 * A small line chart of an item's average high and low prices. Hovering shows the values at that time.
 * Only uses plain Swing so the scaling can be unit tested.
 */
public class PriceChart extends JPanel
{
	private static final int TOP = 18;
	private static final int BOTTOM = 14;
	private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT);
	private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("d MMM HH:mm", Locale.ROOT);

	private Theme theme = Theme.GRAND_EXCHANGE;
	private List<PriceHistory.Point> points = Collections.emptyList();
	private boolean hourly;
	private int hoverX = -1;

	public PriceChart()
	{
		setOpaque(false);
		setPreferredSize(new Dimension(200, 110));
		ToolTipManager.sharedInstance().registerComponent(this);
		MouseAdapter hover = new MouseAdapter()
		{
			@Override
			public void mouseMoved(MouseEvent e)
			{
				hoverX = e.getX();
				repaint();
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				hoverX = -1;
				repaint();
			}
		};
		addMouseListener(hover);
		addMouseMotionListener(hover);
	}

	public void setTheme(Theme theme)
	{
		this.theme = theme;
		repaint();
	}

	public void setHistory(PriceHistory history)
	{
		points = history == null || history.points == null ? Collections.emptyList() : history.points;
		hourly = history != null && !"day".equals(history.period);
		repaint();
	}

	@Override
	public String getToolTipText(MouseEvent e)
	{
		int i = nearest(points, e.getX(), getWidth());
		if (i < 0)
		{
			return null;
		}
		PriceHistory.Point p = points.get(i);
		return String.format(Locale.ROOT, "%s  High %s  Low %s", label(p.t), price(p.high), price(p.low));
	}

	@Override
	protected void paintComponent(Graphics graphics)
	{
		super.paintComponent(graphics);
		Graphics2D g = (Graphics2D) graphics.create();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setFont(getFont().deriveFont(Font.PLAIN, 10f));
		FontMetrics fm = g.getFontMetrics();
		int w = getWidth();
		int h = getHeight();
		long[] range = range(points);
		if (range == null)
		{
			g.setColor(theme.muted);
			g.drawString("No trades recorded for this period yet.", 0, h / 2);
			g.dispose();
			return;
		}

		int plotH = h - TOP - BOTTOM;
		g.setColor(theme.border);
		g.drawLine(0, TOP, w, TOP);
		g.drawLine(0, TOP + plotH, w, TOP + plotH);
		g.setColor(theme.muted);
		g.drawString(price(range[1]), 0, TOP - 5);
		g.drawString(price(range[0]), 0, h - 2);
		String span = label(points.get(0).t) + " to now";
		g.drawString(span, w - fm.stringWidth(span), h - 2);

		g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		drawLine(g, true, range, w, plotH);
		drawLine(g, false, range, w, plotH);

		int i = hoverX < 0 ? -1 : nearest(points, hoverX, w);
		if (i >= 0)
		{
			int x = x(i, points.size(), w);
			g.setStroke(new BasicStroke(1f));
			g.setColor(theme.muted);
			g.drawLine(x, TOP, x, TOP + plotH);
		}
		g.dispose();
	}

	private void drawLine(Graphics2D g, boolean high, long[] range, int w, int plotH)
	{
		Path2D path = new Path2D.Double();
		boolean drawing = false;
		for (int i = 0; i < points.size(); i++)
		{
			Long v = high ? points.get(i).high : points.get(i).low;
			if (v == null)
			{
				// A bucket with no trades on this side breaks the line rather than inventing a value.
				drawing = false;
				continue;
			}
			double y = TOP + y(v, range, plotH);
			if (drawing)
			{
				path.lineTo(x(i, points.size(), w), y);
			}
			else
			{
				path.moveTo(x(i, points.size(), w), y);
				drawing = true;
			}
		}
		g.setColor(high ? theme.chartHigh : theme.chartLow);
		g.draw(path);
	}

	private String label(long t)
	{
		return (hourly ? DAY : TIME).format(Instant.ofEpochSecond(t).atZone(ZoneId.systemDefault()));
	}

	private static String price(Long v)
	{
		return v == null ? "-" : String.format(Locale.ROOT, "%,d", v);
	}

	/** Lowest and highest price across both lines, or null if there is nothing to draw. */
	static long[] range(List<PriceHistory.Point> points)
	{
		long min = Long.MAX_VALUE;
		long max = Long.MIN_VALUE;
		for (PriceHistory.Point p : points)
		{
			for (Long v : new Long[]{p.high, p.low})
			{
				if (v != null)
				{
					min = Math.min(min, v);
					max = Math.max(max, v);
				}
			}
		}
		if (min == Long.MAX_VALUE)
		{
			return null;
		}
		return new long[]{min, max};
	}

	static int x(int index, int count, int width)
	{
		return count <= 1 ? width / 2 : (int) Math.round((double) index * (width - 1) / (count - 1));
	}

	/** Pixels down from the top of the plot; a flat line sits in the middle. */
	static double y(long value, long[] range, int height)
	{
		if (range[1] == range[0])
		{
			return height / 2.0;
		}
		return (double) (range[1] - value) * height / (range[1] - range[0]);
	}

	static int nearest(List<PriceHistory.Point> points, int px, int width)
	{
		if (points.isEmpty() || width <= 0)
		{
			return -1;
		}
		if (points.size() == 1)
		{
			return 0;
		}
		int i = (int) Math.round((double) px * (points.size() - 1) / (width - 1));
		return Math.max(0, Math.min(points.size() - 1, i));
	}
}
