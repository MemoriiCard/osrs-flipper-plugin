package com.gpgenius.ui;

import com.gpgenius.api.Suggestion;
import java.awt.BorderLayout;
import java.awt.Font;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;

/**
 * "Buy limits": each item bought in its current 4-hour window, how much of the limit is used, and a
 * live countdown to the reset. Items at their limit say other items are suggested meanwhile. Swing
 * thread only. The countdown ticks once a second while the card is showing.
 */
public class LimitCard extends RoundedPanel
{
	private final JLabel title = new JLabel("BUY LIMITS");
	private final JPanel rows = new JPanel();
	private final Timer timer = new Timer(1000, e -> tick());
	private final List<Row> shown = new ArrayList<>();
	private Theme theme = Theme.GRAND_EXCHANGE;
	private Font font;
	private Font smallFont;
	private List<Suggestion.BuyLimit> limits = Collections.emptyList();
	private boolean allowed = true;

	private static final class Row
	{
		final Suggestion.BuyLimit limit;
		final long resetsAt;
		final JLabel note;

		Row(Suggestion.BuyLimit limit, JLabel note)
		{
			this.limit = limit;
			this.resetsAt = LimitTimers.resetsAtMillis(limit.resetsAt);
			this.note = note;
		}
	}

	public LimitCard()
	{
		super(new BorderLayout(0, 6));
		setBorder(new EmptyBorder(10, 12, 10, 10));
		setAlignmentX(LEFT_ALIGNMENT);
		rows.setLayout(new BoxLayout(rows, BoxLayout.Y_AXIS));
		rows.setOpaque(false);
		add(title, BorderLayout.NORTH);
		add(rows, BorderLayout.CENTER);
		timer.setRepeats(true);
		setVisible(false);
	}

	/** Fonts from the panel, so the card matches it (RuneLite fonts in game, defaults in tests). */
	public void setFonts(Font font, Font smallFont)
	{
		this.font = font;
		this.smallFont = smallFont;
		title.setFont(smallFont);
		rebuild();
	}

	public void setTheme(Theme theme)
	{
		this.theme = theme;
		setColors(theme.surface, theme.border);
		title.setForeground(theme.muted);
		rebuild();
	}

	/** Whether the layout shows this card at all. */
	public void setAllowed(boolean allowed)
	{
		this.allowed = allowed;
		rebuild();
	}

	public void show(List<Suggestion.BuyLimit> limits)
	{
		this.limits = limits == null ? Collections.<Suggestion.BuyLimit>emptyList() : limits;
		rebuild();
	}

	private void rebuild()
	{
		rows.removeAll();
		shown.clear();
		boolean visible = allowed && !limits.isEmpty();
		setVisible(visible);
		if (!visible)
		{
			timer.stop();
			return;
		}
		long now = System.currentTimeMillis();
		for (Suggestion.BuyLimit l : limits)
		{
			JPanel row = new JPanel(new BorderLayout(6, 0));
			row.setOpaque(false);
			row.setAlignmentX(LEFT_ALIGNMENT);
			row.setBorder(new EmptyBorder(rows.getComponentCount() == 0 ? 0 : 6, 0, 0, 0));
			JLabel name = new JLabel(l.name == null ? "Item " + l.itemId : l.name);
			JLabel used = new JLabel(LimitTimers.usage(l));
			JLabel note = new JLabel();
			if (font != null)
			{
				name.setFont(font);
				used.setFont(smallFont);
				note.setFont(smallFont);
			}
			name.setForeground(theme.text);
			used.setForeground(l.locked ? theme.warning : theme.muted);
			note.setForeground(l.locked ? theme.warning : theme.muted);
			row.add(name, BorderLayout.CENTER);
			row.add(used, BorderLayout.EAST);
			row.add(note, BorderLayout.SOUTH);
			Row r = new Row(l, note);
			note.setText(LimitTimers.note(l, r.resetsAt, now));
			shown.add(r);
			rows.add(row);
		}
		if (!timer.isRunning())
		{
			timer.start();
		}
		revalidate();
		repaint();
	}

	private void tick()
	{
		if (!isShowing())
		{
			return;
		}
		long now = System.currentTimeMillis();
		for (Row r : shown)
		{
			r.note.setText(LimitTimers.note(r.limit, r.resetsAt, now));
		}
	}

	/** Stops the countdown, for shutdown. */
	public void stop()
	{
		timer.stop();
	}
}
