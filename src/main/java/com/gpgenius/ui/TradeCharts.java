package com.gpgenius.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.JComponent;
import net.runelite.client.util.QuantityFormatter;

/**
 * Small charts for the trade dashboard, drawn with Java2D in the current theme. Gains use the profit
 * colour and losses the loss colour; every chart shows the exact value of the point or bar under the
 * mouse as a tooltip, so colour is never the only way to read it.
 */
public final class TradeCharts
{
	private TradeCharts()
	{
	}

	/** One labelled value. */
	public static final class Point
	{
		public final String label;
		public final long value;

		public Point(String label, long value)
		{
			this.label = label;
			this.value = value;
		}
	}

	/** Round tick values covering low..high, always including zero. */
	public static List<Long> ticks(long low, long high, int count)
	{
		low = Math.min(low, 0);
		high = Math.max(high, 0);
		if (low == high)
		{
			high = 1;
		}
		double raw = (double) (high - low) / Math.max(1, count);
		double magnitude = Math.pow(10, Math.floor(Math.log10(raw)));
		double step = magnitude;
		for (double m : new double[]{1, 2, 2.5, 5, 10})
		{
			if (m * magnitude >= raw)
			{
				step = m * magnitude;
				break;
			}
		}
		long s = Math.max(1, Math.round(step));
		List<Long> out = new ArrayList<>();
		for (long t = (long) Math.floor((double) low / s) * s; t < high + s; t += s)
		{
			out.add(t);
		}
		return out;
	}

	/** 1,234,567 to "1.23M", keeping the sign. */
	public static String shortGp(long value)
	{
		return (value < 0 ? "-" : "") + QuantityFormatter.quantityToStackSize(Math.abs(value));
	}

	/** Shared drawing for both chart kinds. */
	abstract static class Chart extends JComponent
	{
		static final int LEFT = 52;
		static final int RIGHT = 8;
		static final int TOP = 24;
		static final int BOTTOM = 22;

		final String title;
		final String unit;
		Theme theme = Theme.GRAND_EXCHANGE;
		List<Point> points = Collections.emptyList();
		int hover = -1;

		Chart(String title, String unit)
		{
			this.title = title;
			this.unit = unit;
			setPreferredSize(new Dimension(320, 190));
			setMinimumSize(new Dimension(200, 150));
			setToolTipText("");
			MouseAdapter mouse = new MouseAdapter()
			{
				@Override
				public void mouseMoved(MouseEvent e)
				{
					int i = indexAt(e.getX());
					if (i != hover)
					{
						hover = i;
						repaint();
					}
				}

				@Override
				public void mouseExited(MouseEvent e)
				{
					hover = -1;
					repaint();
				}
			};
			addMouseListener(mouse);
			addMouseMotionListener(mouse);
		}

		void setTheme(Theme theme)
		{
			this.theme = theme;
			repaint();
		}

		void setPoints(List<Point> points)
		{
			this.points = points == null ? Collections.emptyList() : points;
			hover = -1;
			repaint();
		}

		@Override
		public String getToolTipText(MouseEvent e)
		{
			int i = indexAt(e.getX());
			if (i < 0)
			{
				return null;
			}
			Point p = points.get(i);
			return tooltip(p);
		}

		String tooltip(Point p)
		{
			String sign = "gp".equals(unit) && p.value > 0 ? "+" : "";
			return p.label + ": " + sign + String.format("%,d", p.value) + " " + unit;
		}

		abstract int indexAt(int x);

		int plotWidth()
		{
			return Math.max(1, getWidth() - LEFT - RIGHT);
		}

		int plotHeight()
		{
			return Math.max(1, getHeight() - TOP - BOTTOM);
		}

		@Override
		protected void paintComponent(Graphics g0)
		{
			Graphics2D g = (Graphics2D) g0.create();
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			g.setColor(theme.surface);
			g.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
			g.setColor(theme.border);
			g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
			Font base = getFont() == null ? new Font(Font.SANS_SERIF, Font.PLAIN, 12) : getFont();
			g.setFont(base.deriveFont(Font.BOLD));
			g.setColor(theme.text);
			g.drawString(title, 10, 16);
			g.setFont(base.deriveFont(Font.PLAIN, Math.max(10f, base.getSize2D() - 1)));
			if (points.isEmpty())
			{
				g.setColor(theme.muted);
				g.drawString("Nothing to chart yet.", LEFT, TOP + plotHeight() / 2);
				g.dispose();
				return;
			}
			long lo = Long.MAX_VALUE;
			long hi = Long.MIN_VALUE;
			for (Point p : points)
			{
				lo = Math.min(lo, p.value);
				hi = Math.max(hi, p.value);
			}
			List<Long> ticks = ticks(lo, hi, 4);
			long tlo = ticks.get(0);
			long thi = ticks.get(ticks.size() - 1);
			FontMetrics fm = g.getFontMetrics();
			for (long t : ticks)
			{
				int y = y(t, tlo, thi);
				g.setColor(t == 0 ? theme.muted : theme.border);
				g.drawLine(LEFT, y, getWidth() - RIGHT, y);
				g.setColor(theme.muted);
				String label = "gp".equals(unit) ? shortGp(t) : String.format("%,d", t);
				g.drawString(label, LEFT - 6 - fm.stringWidth(label), y + fm.getAscent() / 2 - 1);
			}
			draw(g, tlo, thi, fm);
			g.dispose();
		}

		int y(long v, long lo, long hi)
		{
			return TOP + (int) Math.round((double) (hi - v) / (double) (hi - lo) * plotHeight());
		}

		abstract void draw(Graphics2D g, long lo, long hi, FontMetrics fm);

		Color colour(long v)
		{
			return v < 0 ? theme.loss : theme.profit;
		}
	}

	/** Bars around a zero baseline. */
	public static class BarChart extends Chart
	{
		public BarChart(String title, String unit)
		{
			super(title, unit);
		}

		@Override
		int indexAt(int x)
		{
			if (points.isEmpty() || x < LEFT || x > getWidth() - RIGHT)
			{
				return -1;
			}
			int i = (int) ((x - LEFT) / ((double) plotWidth() / points.size()));
			return Math.min(points.size() - 1, Math.max(0, i));
		}

		@Override
		void draw(Graphics2D g, long lo, long hi, FontMetrics fm)
		{
			double slot = (double) plotWidth() / points.size();
			int width = Math.max(2, Math.min(56, (int) (slot * 0.7)));
			int zero = y(0, lo, hi);
			int every = Math.max(1, (int) Math.ceil(points.size() / Math.max(1.0, plotWidth() / 70.0)));
			for (int i = 0; i < points.size(); i++)
			{
				Point p = points.get(i);
				int x = LEFT + (int) (i * slot + (slot - width) / 2);
				int y = y(p.value, lo, hi);
				Color c = "gp".equals(unit) ? colour(p.value) : theme.accent;
				if (i == hover)
				{
					g.setColor(theme.raised);
					g.fillRect(LEFT + (int) (i * slot), TOP, (int) Math.ceil(slot), plotHeight());
				}
				g.setColor(c);
				g.fillRoundRect(x, Math.min(y, zero), width, Math.max(1, Math.abs(zero - y)), 4, 4);
				if (i % every == 0)
				{
					g.setColor(theme.muted);
					String label = fit(p.label, fm, (int) (slot * every) - 4);
					g.drawString(label, x + width / 2 - fm.stringWidth(label) / 2, getHeight() - 7);
				}
			}
		}
	}

	/** A running total as a line with a soft area down to zero. */
	public static class LineChart extends Chart
	{
		public LineChart(String title)
		{
			super(title, "gp");
		}

		@Override
		String tooltip(Point p)
		{
			return p.label + ": " + (p.value > 0 ? "+" : "") + String.format("%,d", p.value) + " gp so far";
		}

		@Override
		int indexAt(int x)
		{
			if (points.isEmpty() || x < LEFT - 4 || x > getWidth() - RIGHT + 4)
			{
				return -1;
			}
			if (points.size() == 1)
			{
				return 0;
			}
			double step = (double) plotWidth() / (points.size() - 1);
			int i = (int) Math.round((x - LEFT) / step);
			return Math.min(points.size() - 1, Math.max(0, i));
		}

		private int x(int i)
		{
			if (points.size() == 1)
			{
				return LEFT + plotWidth() / 2;
			}
			return LEFT + (int) Math.round(i * (double) plotWidth() / (points.size() - 1));
		}

		@Override
		void draw(Graphics2D g, long lo, long hi, FontMetrics fm)
		{
			int zero = y(0, lo, hi);
			Path2D line = new Path2D.Double();
			Path2D area = new Path2D.Double();
			area.moveTo(x(0), zero);
			for (int i = 0; i < points.size(); i++)
			{
				int x = x(i);
				int y = y(points.get(i).value, lo, hi);
				if (i == 0)
				{
					line.moveTo(x, y);
				}
				else
				{
					line.lineTo(x, y);
				}
				area.lineTo(x, y);
			}
			area.lineTo(x(points.size() - 1), zero);
			area.closePath();
			Color c = colour(points.get(points.size() - 1).value);
			g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 40));
			g.fill(area);
			g.setColor(c);
			g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			g.draw(line);
			if (hover >= 0)
			{
				int x = x(hover);
				int y = y(points.get(hover).value, lo, hi);
				g.setColor(theme.muted);
				g.setStroke(new BasicStroke(1f));
				g.drawLine(x, TOP, x, TOP + plotHeight());
				g.setColor(c);
				g.fillOval(x - 4, y - 4, 8, 8);
			}
			g.setColor(theme.muted);
			String first = points.get(0).label;
			String last = points.get(points.size() - 1).label;
			g.drawString(first, LEFT, getHeight() - 7);
			if (points.size() > 1)
			{
				g.drawString(last, getWidth() - RIGHT - fm.stringWidth(last), getHeight() - 7);
			}
		}
	}

	/** The label cut to fit the width, with "..." when it was too long. */
	static String fit(String label, FontMetrics fm, int width)
	{
		if (label == null)
		{
			return "";
		}
		if (fm.stringWidth(label) <= width)
		{
			return label;
		}
		String s = label;
		while (s.length() > 1 && fm.stringWidth(s + "...") > width)
		{
			s = s.substring(0, s.length() - 1);
		}
		return s.length() <= 1 ? "" : s + "...";
	}
}
