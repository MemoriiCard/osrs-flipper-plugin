package com.gpgenius.ui;

import com.gpgenius.api.Efficiency;
import com.gpgenius.tracking.IdleTracker;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.FontManager;
import net.runelite.client.util.QuantityFormatter;

/**
 * GP per hour in the side panel: overall, this session, the last hours and per GE slot from the
 * server, plus idle slot time and unused cash from what the client sees. Every figure can be
 * switched off and given its window under "Choose"; the website's dashboard uses the same choices.
 */
public class EfficiencyCard extends RoundedPanel
{
	private final JLabel title = new JLabel("GP per hour");
	private final JPanel rates = column();
	private final JPanel slots = column();
	private final JLabel idle = new JLabel();
	private final JLabel status = new JLabel();
	private final JPanel chooser = column();
	private final ThemedButton choose = new ThemedButton("Choose", "Pick which figures to show and over what time.", ThemedButton.Style.GHOST);

	private final JCheckBox overallOn = check("Overall");
	private final JSpinner overallDays = spinner(0, 0, 365);
	private final JCheckBox sessionOn = check("This session");
	private final JCheckBox dayOn = check("Recent");
	private final JSpinner dayHours = spinner(24, 1, 720);
	private final JCheckBox slotsOn = check("By GE slot");
	private final JSpinner slotsHours = spinner(24, 1, 720);
	private final JCheckBox idleOn = check("Idle slots and unused cash");

	private final List<JLabel> mutedLabels = new ArrayList<>();
	private final List<ThemedButton> saveButtons = new ArrayList<>();
	private Theme theme = Theme.GRAND_EXCHANGE;
	private Efficiency data;
	private boolean showIdle = true;

	/**
	 * @param onSave the new choices as the server wants them ({overall: {enabled, days}, ...})
	 * @param onIdleToggle whether to show idle slots and unused cash (a plugin setting)
	 */
	public EfficiencyCard(Consumer<Map<String, Map<String, Object>>> onSave, Consumer<Boolean> onIdleToggle)
	{
		super(new BorderLayout());
		setAlignmentX(LEFT_ALIGNMENT);
		setBorder(new EmptyBorder(8, 12, 8, 10));
		title.setFont(FontManager.getRunescapeBoldFont());
		idle.setFont(FontManager.getRunescapeSmallFont());
		status.setFont(FontManager.getRunescapeSmallFont());
		choose.setFont(FontManager.getRunescapeSmallFont());
		mutedLabels.add(idle);
		mutedLabels.add(status);

		JPanel head = new JPanel(new BorderLayout());
		head.setOpaque(false);
		head.add(title, BorderLayout.WEST);
		head.add(choose, BorderLayout.EAST);

		chooser.setVisible(false);
		stack(chooser, withSpinner(overallOn, overallDays, "days (0 = all time)"), 2);
		stack(chooser, sessionOn, 2);
		stack(chooser, withSpinner(dayOn, dayHours, "hours"), 2);
		stack(chooser, withSpinner(slotsOn, slotsHours, "hours"), 2);
		stack(chooser, idleOn, 2);
		ThemedButton save = new ThemedButton("Save", null, ThemedButton.Style.SECONDARY);
		save.setFont(FontManager.getRunescapeSmallFont());
		save.addActionListener(e ->
		{
			showIdle = idleOn.isSelected();
			onIdleToggle.accept(showIdle);
			onSave.accept(prefs());
			chooser.setVisible(false);
			status.setText("Saving...");
			revalidate();
		});
		JPanel saveRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		saveRow.setOpaque(false);
		saveRow.add(save);
		stack(chooser, saveRow, 4);
		choose.addActionListener(e ->
		{
			chooser.setVisible(!chooser.isVisible());
			revalidate();
			repaint();
		});
		saveButtons.add(save);
		JPanel text = column();
		stack(text, head, 0);
		stack(text, chooser, 4);
		stack(text, rates, 4);
		stack(text, slots, 4);
		stack(text, idle, 4);
		stack(text, status, 2);
		add(text, BorderLayout.CENTER);
		show(null, null, null, System.currentTimeMillis());
	}

	public void setShowIdle(boolean show)
	{
		showIdle = show;
		idleOn.setSelected(show);
	}

	/** New figures from the server (null keeps the last ones), the live idle tracker, and an error. */
	public void show(Efficiency fresh, IdleTracker tracker, String error, long now)
	{
		if (fresh != null)
		{
			data = fresh;
			fillChooser(fresh);
		}
		rates.removeAll();
		slots.removeAll();
		if (data != null)
		{
			for (Efficiency.Rate r : data.rates())
			{
				rates.add(line(r.label, perHour(r.perHour)));
			}
			if (data.slots != null)
			{
				JLabel heading = new JLabel(data.slotsLabel == null ? "By slot" : data.slotsLabel);
				heading.setFont(FontManager.getRunescapeSmallFont());
				heading.setForeground(theme.muted);
				stack(slots, heading, 0);
				JPanel grid = new JPanel(new GridLayout(0, 2, 6, 0));
				grid.setOpaque(false);
				int shown = tracker != null && tracker.running() ? tracker.slotsAvailable() : data.slots.size();
				for (Efficiency.SlotRate s : data.slots)
				{
					if (s.slot >= shown)
					{
						continue;
					}
					String idleText = showIdle && tracker != null && tracker.running()
						? " · idle " + IdleTracker.duration(tracker.idleMillis(s.slot, now)) : "";
					grid.add(small("Slot " + (s.slot + 1), theme.muted));
					grid.add(small(perHour(s.perHour) + idleText, theme.text));
				}
				stack(slots, grid, 2);
			}
			if (data.rates().isEmpty() && data.slots == null)
			{
				rates.add(small("Every figure is switched off. Use Choose to turn some on.", theme.muted));
			}
		}
		if (showIdle && tracker != null && tracker.running() && tracker.elapsed(now) > 60_000)
		{
			idle.setText(html(String.format("Idle slots: %.0f%% of slot time since you logged in. Unused cash: %s (%.0f%% of your coins).",
				tracker.idleShare(now) * 100, QuantityFormatter.quantityToStackSize(tracker.unusedCash()) + " gp",
				tracker.unusedShare() * 100)));
			idle.setVisible(true);
		}
		else
		{
			idle.setVisible(false);
		}
		status.setText(error == null ? (data == null ? "Sign in to see your GP per hour." : "") : html(error));
		status.setForeground(error == null ? theme.muted : theme.loss);
		revalidate();
		repaint();
	}

	public void setTheme(Theme theme)
	{
		this.theme = theme;
		setColors(theme.surface, theme.border);
		title.setForeground(theme.text);
		for (JLabel l : mutedLabels)
		{
			l.setForeground(theme.muted);
		}
		for (JCheckBox c : new JCheckBox[]{overallOn, sessionOn, dayOn, slotsOn, idleOn})
		{
			c.setForeground(theme.text);
		}
		choose.setTheme(theme);
		for (ThemedButton b : saveButtons)
		{
			b.setTheme(theme);
		}
		show(null, null, null, System.currentTimeMillis());
	}

	/** The chooser's state as the server's preferences. */
	Map<String, Map<String, Object>> prefs()
	{
		Map<String, Map<String, Object>> out = new LinkedHashMap<>();
		out.put("overall", pref(overallOn, "days", overallDays));
		out.put("session", pref(sessionOn, null, null));
		out.put("day", pref(dayOn, "hours", dayHours));
		out.put("slots", pref(slotsOn, "hours", slotsHours));
		return out;
	}

	private void fillChooser(Efficiency e)
	{
		overallOn.setSelected(e.enabled("overall"));
		overallDays.setValue(e.window("overall", "days", 0));
		sessionOn.setSelected(e.enabled("session"));
		dayOn.setSelected(e.enabled("day"));
		dayHours.setValue(e.window("day", "hours", 24));
		slotsOn.setSelected(e.enabled("slots"));
		slotsHours.setValue(e.window("slots", "hours", 24));
		idleOn.setSelected(showIdle);
	}

	private static Map<String, Object> pref(JCheckBox on, String field, JSpinner value)
	{
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("enabled", on.isSelected());
		if (field != null)
		{
			m.put(field, ((Number) value.getValue()).intValue());
		}
		return m;
	}

	private static String perHour(Long value)
	{
		if (value == null)
		{
			return "–";
		}
		return (value > 0 ? "+" : "") + QuantityFormatter.quantityToStackSize(value) + " gp/h";
	}

	private JComponent line(String label, String value)
	{
		JPanel row = new JPanel(new BorderLayout());
		row.setOpaque(false);
		row.setAlignmentX(LEFT_ALIGNMENT);
		row.add(small(label, theme.muted), BorderLayout.WEST);
		JLabel v = small(value, theme.text);
		v.setFont(FontManager.getRunescapeFont());
		row.add(v, BorderLayout.EAST);
		return row;
	}

	private JLabel small(String text, java.awt.Color color)
	{
		JLabel l = new JLabel(text);
		l.setFont(FontManager.getRunescapeSmallFont());
		l.setForeground(color);
		return l;
	}

	private static JCheckBox check(String text)
	{
		JCheckBox c = new JCheckBox(text, true);
		c.setOpaque(false);
		c.setFont(FontManager.getRunescapeSmallFont());
		return c;
	}

	private static JSpinner spinner(int value, int min, int max)
	{
		JSpinner s = new JSpinner(new SpinnerNumberModel(value, min, max, 1));
		s.setFont(FontManager.getRunescapeSmallFont());
		return s;
	}

	private JComponent withSpinner(JCheckBox box, JSpinner spinner, String unit)
	{
		JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
		row.setOpaque(false);
		row.add(box);
		row.add(spinner);
		JLabel u = new JLabel(unit);
		u.setFont(FontManager.getRunescapeSmallFont());
		mutedLabels.add(u);
		row.add(u);
		return row;
	}

	private static JPanel column()
	{
		JPanel p = new JPanel();
		p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
		p.setOpaque(false);
		p.setAlignmentX(LEFT_ALIGNMENT);
		return p;
	}

	private static void stack(JPanel column, JComponent child, int gap)
	{
		if (column.getComponentCount() > 0 && gap > 0)
		{
			column.add(Box.createVerticalStrut(gap));
		}
		child.setAlignmentX(LEFT_ALIGNMENT);
		column.add(child);
	}

	private static String html(String text)
	{
		return FlipperPanel.html(text);
	}
}
