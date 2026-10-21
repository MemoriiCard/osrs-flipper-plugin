package com.osrsflipper.ui;

import com.osrsflipper.tracking.Flip;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.util.Collections;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.QuantityFormatter;

/**
 * Side panel: session and all-time profit, then the most recent flips.
 */
public class FlipperPanel extends PluginPanel
{
	private static final Color PROFIT = new Color(0x4C, 0xBB, 0x17);
	private static final Color LOSS = new Color(0xE0, 0x4B, 0x4B);

	private final JLabel sessionLabel = new JLabel();
	private final JLabel allTimeLabel = new JLabel();
	private final JPanel flipList = new JPanel();
	private final JLabel status = new JLabel();

	public FlipperPanel()
	{
		setLayout(new BorderLayout());
		setBorder(new EmptyBorder(10, 10, 10, 10));

		JPanel header = new JPanel(new GridLayout(0, 1, 0, 4));
		JLabel title = new JLabel("OSRS Flipper");
		title.setFont(FontManager.getRunescapeBoldFont());
		header.add(title);
		header.add(sessionLabel);
		header.add(allTimeLabel);
		header.add(status);
		header.setBorder(new EmptyBorder(0, 0, 10, 0));
		add(header, BorderLayout.NORTH);

		flipList.setLayout(new BoxLayout(flipList, BoxLayout.Y_AXIS));
		add(flipList, BorderLayout.CENTER);

		update(false, 0, 0, Collections.emptyList());
	}

	public void update(boolean loggedIn, long sessionProfit, long allTimeProfit, List<Flip> recentFlips)
	{
		sessionLabel.setText("Session profit: " + gp(sessionProfit));
		sessionLabel.setForeground(sessionProfit < 0 ? LOSS : PROFIT);
		allTimeLabel.setText("All-time profit: " + gp(allTimeProfit));
		allTimeLabel.setForeground(allTimeProfit < 0 ? LOSS : PROFIT);
		status.setText(loggedIn
			? (recentFlips.isEmpty() ? "No flips yet. Buy and sell on the GE to start tracking." : "Recent flips")
			: "Log in to see your flips.");
		status.setForeground(ColorScheme.LIGHT_GRAY_COLOR);

		flipList.removeAll();
		for (Flip flip : recentFlips)
		{
			flipList.add(row(flip));
		}
		flipList.revalidate();
		flipList.repaint();
	}

	private JPanel row(Flip flip)
	{
		JPanel row = new JPanel(new BorderLayout());
		row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		row.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createMatteBorder(0, 0, 4, 0, ColorScheme.DARK_GRAY_COLOR),
			new EmptyBorder(6, 8, 6, 8)));

		JLabel name = new JLabel(flip.itemName + " x" + QuantityFormatter.formatNumber(flip.quantity));
		name.setForeground(Color.WHITE);
		JLabel detail = new JLabel(QuantityFormatter.quantityToStackSize(flip.buyPrice) + " → "
			+ QuantityFormatter.quantityToStackSize(flip.sellPrice));
		detail.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		JLabel profit = new JLabel(gp(flip.profit));
		profit.setForeground(flip.profit < 0 ? LOSS : PROFIT);

		JPanel left = new JPanel(new GridLayout(0, 1));
		left.setOpaque(false);
		left.add(name);
		left.add(detail);
		row.add(left, BorderLayout.CENTER);
		row.add(profit, BorderLayout.EAST);
		return row;
	}

	private static String gp(long amount)
	{
		return QuantityFormatter.formatNumber(amount) + " gp";
	}
}
