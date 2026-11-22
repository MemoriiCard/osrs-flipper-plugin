package com.gpgenius.ui;

import com.gpgenius.api.TradeHistory;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableRowSorter;

/**
 * The pop-out trade dashboard: every completed trade with open and close times, item, prices, GE tax
 * and gross and net profit, plus totals and charts over the same trades.
 *
 * <p>It shows only the player's own trades, as the server sends them from GET /v1/history. Nothing
 * about suggestions is shown here.</p>
 */
public class TradeDashboard extends JFrame
{
	/** Fetches data for the window. Callbacks may arrive on any thread. */
	public interface Source
	{
		void load(int days, Long beforeId, Consumer<TradeHistory> onSuccess, Consumer<String> onError);

		/** Save the trades in the window's period as a CSV file. */
		void export(int days);
	}

	static final int[] DAYS = {1, 7, 30, 90, 365, 3650};
	static final String[] DAY_LABELS = {"Today", "7 days", "30 days", "90 days", "Year", "All time"};
	static final String[] COLUMNS = {
		"Opened", "Closed", "Held", "Account", "Item", "Qty", "Buy", "Sell", "Cost", "Sales", "Tax", "Gross", "Net",
	};
	private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");

	private final Source source;
	private Theme theme = Theme.GRAND_EXCHANGE;
	private final List<Runnable> painters = new ArrayList<>();
	private int days = 30;
	/** Bumped on every reload so replies to older requests are dropped. */
	private int generation;

	private final JPanel root = new JPanel(new BorderLayout(0, 8));
	private final ThemedButton[] periodButtons = new ThemedButton[DAYS.length];
	private final ThemedButton refreshButton = new ThemedButton("Refresh", "Load your latest trades", ThemedButton.Style.GHOST);
	private final ThemedButton exportButton = new ThemedButton("Export CSV", "Save these trades as a spreadsheet file", ThemedButton.Style.SECONDARY);
	private final ThemedButton moreButton = new ThemedButton("Load more", "Show older trades in the table", ThemedButton.Style.GHOST);
	private final JLabel status = new JLabel();
	private final JPanel tiles = new JPanel(new GridLayout(2, 5, 8, 8));
	private final TradeCharts.LineChart runningChart = new TradeCharts.LineChart("Net profit over your trades");
	private final TradeCharts.BarChart splitChart = new TradeCharts.BarChart("Gross, tax and net", "gp");
	private final TradeCharts.BarChart distributionChart = new TradeCharts.BarChart("Trades by net profit", "trades");
	private final TradeCharts.BarChart holdChart = new TradeCharts.BarChart("Net profit by hold time", "gp");
	private final TradeCharts.BarChart bestItemsChart = new TradeCharts.BarChart("Best items (net)", "gp");
	private final TradeCharts.BarChart worstItemsChart = new TradeCharts.BarChart("Items that lost (net)", "gp");
	private final TradesModel model = new TradesModel();
	private final JTable table = new JTable(model);
	private final JScrollPane tableScroll = new JScrollPane(table);
	private Long nextBeforeId;

	public TradeDashboard(Source source)
	{
		super("GP Genius: your trades");
		this.source = source;
		setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
		setMinimumSize(new Dimension(820, 600));
		setSize(new Dimension(1180, 820));

		root.setBorder(new EmptyBorder(12, 12, 12, 12));
		root.add(top(), BorderLayout.NORTH);

		JPanel upper = new JPanel(new BorderLayout(0, 8));
		upper.setOpaque(false);
		tiles.setOpaque(false);
		upper.add(tiles, BorderLayout.NORTH);
		JPanel charts = new JPanel(new GridLayout(2, 3, 8, 8));
		charts.setOpaque(false);
		for (TradeCharts.Chart c : charts())
		{
			charts.add(c);
		}
		upper.add(charts, BorderLayout.CENTER);

		JPanel lower = new JPanel(new BorderLayout(0, 6));
		lower.setOpaque(false);
		lower.add(tableScroll, BorderLayout.CENTER);
		JPanel more = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
		more.setOpaque(false);
		more.add(moreButton);
		lower.add(more, BorderLayout.SOUTH);
		moreButton.addActionListener(e -> loadMore());

		JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, upper, lower);
		split.setResizeWeight(0.55);
		split.setBorder(null);
		split.setOpaque(false);
		root.add(split, BorderLayout.CENTER);
		setContentPane(root);

		table.setAutoCreateRowSorter(false);
		TableRowSorter<TradesModel> sorter = new TableRowSorter<>(model);
		for (int i = 0; i < COLUMNS.length; i++)
		{
			sorter.setComparator(i, comparator(i));
		}
		table.setRowSorter(sorter);
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.setFillsViewportHeight(true);
		table.setRowHeight(22);
		table.setShowVerticalLines(false);
		table.setDefaultRenderer(Object.class, new Cells());
		int[] widths = {130, 130, 70, 90, 170, 60, 80, 80, 100, 100, 80, 90, 90};
		for (int i = 0; i < widths.length; i++)
		{
			table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
		}
		table.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);

		painters.add(() ->
		{
			root.setBackground(theme.background);
			status.setForeground(theme.muted);
			table.setBackground(theme.surface);
			table.setForeground(theme.text);
			table.setGridColor(theme.border);
			table.setSelectionBackground(theme.raised);
			table.setSelectionForeground(theme.text);
			JTableHeader header = table.getTableHeader();
			header.setBackground(theme.raised);
			header.setForeground(theme.text);
			tableScroll.getViewport().setBackground(theme.surface);
			tableScroll.setBorder(BorderFactory.createLineBorder(theme.border));
			for (TradeCharts.Chart c : charts())
			{
				c.setTheme(theme);
			}
			refreshButton.setTheme(theme);
			exportButton.setTheme(theme);
			moreButton.setTheme(theme);
			for (ThemedButton b : periodButtons)
			{
				b.setTheme(theme);
			}
		});
		showHistory(null);
		applyTheme();
	}

	private List<TradeCharts.Chart> charts()
	{
		List<TradeCharts.Chart> out = new ArrayList<>();
		Collections.addAll(out, runningChart, splitChart, distributionChart, holdChart, bestItemsChart, worstItemsChart);
		return out;
	}

	private JComponent top()
	{
		JPanel bar = new JPanel(new BorderLayout());
		bar.setOpaque(false);
		JPanel periods = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
		periods.setOpaque(false);
		for (int i = 0; i < DAYS.length; i++)
		{
			final int d = DAYS[i];
			ThemedButton b = new ThemedButton(DAY_LABELS[i], null, ThemedButton.Style.SEGMENT);
			b.addActionListener(e ->
			{
				days = d;
				reload();
			});
			periodButtons[i] = b;
			periods.add(b);
		}
		JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
		actions.setOpaque(false);
		refreshButton.addActionListener(e -> reload());
		exportButton.addActionListener(e -> source.export(days));
		actions.add(refreshButton);
		actions.add(exportButton);
		bar.add(periods, BorderLayout.WEST);
		bar.add(actions, BorderLayout.EAST);
		JPanel column = new JPanel(new BorderLayout(0, 6));
		column.setOpaque(false);
		column.add(bar, BorderLayout.NORTH);
		status.setBorder(new EmptyBorder(0, 4, 0, 0));
		column.add(status, BorderLayout.SOUTH);
		return column;
	}

	public void setTheme(Theme theme)
	{
		this.theme = theme;
		applyTheme();
	}

	private void applyTheme()
	{
		for (Runnable p : painters)
		{
			p.run();
		}
		repaint();
	}

	/** Shows the window and loads the latest trades. Swing thread. */
	public void open()
	{
		if (!isVisible())
		{
			setLocationRelativeTo(null);
		}
		setVisible(true);
		toFront();
		reload();
	}

	/** Called by the plugin when an export finishes or fails, to show it in the window too. */
	public void showStatus(String message)
	{
		status.setText(message == null ? "" : message);
	}

	private void reload()
	{
		final int gen = ++generation;
		for (int i = 0; i < DAYS.length; i++)
		{
			periodButtons[i].setSelected(DAYS[i] == days);
		}
		status.setText("Loading your trades...");
		moreButton.setVisible(false);
		source.load(days, null,
			h -> SwingUtilities.invokeLater(() -> received(gen, h, false)),
			message -> SwingUtilities.invokeLater(() -> failed(gen, message)));
	}

	private void loadMore()
	{
		if (nextBeforeId == null)
		{
			return;
		}
		final int gen = generation;
		moreButton.setEnabled(false);
		source.load(days, nextBeforeId,
			h -> SwingUtilities.invokeLater(() -> received(gen, h, true)),
			message -> SwingUtilities.invokeLater(() -> failed(gen, message)));
	}

	private void received(int gen, TradeHistory h, boolean append)
	{
		if (gen != generation)
		{
			return;
		}
		moreButton.setEnabled(true);
		if (append)
		{
			model.add(h == null ? null : h.trades);
			nextBeforeId = h == null ? null : h.nextBeforeId;
			moreButton.setVisible(nextBeforeId != null);
			status.setText(statusLine(model.getRowCount(), h));
			return;
		}
		showHistory(h);
	}

	private void failed(int gen, String message)
	{
		if (gen != generation)
		{
			return;
		}
		moreButton.setEnabled(true);
		status.setText(message == null ? "Couldn't load your trades." : message);
	}

	private void showHistory(TradeHistory h)
	{
		TradeHistory.Summary s = h == null || h.summary == null ? new TradeHistory.Summary() : h.summary;
		tiles.removeAll();
		tile("Trades", String.format("%,d", s.trades), null);
		tile("Gross profit", signed(s.grossProfit), s.grossProfit);
		tile("GE tax paid", String.format("%,d gp", s.tax), null);
		tile("Net profit", signed(s.netProfit), s.netProfit);
		tile("Coins spent", String.format("%,d gp", s.cost), null);
		tile("Sales after tax", String.format("%,d gp", s.revenue - s.tax), null);
		tile("Won / lost", s.wins + " / " + s.losses, null);
		tile("Average hold", TradeHistory.hold(Math.round(s.avgHoldMinutes)), null);
		tile("Best trade", signed(s.best), s.best);
		tile("Worst trade", signed(s.worst), s.worst);
		tiles.revalidate();

		ZoneId zone = ZoneId.systemDefault();
		runningChart.setPoints(h == null ? null : running(h, zone));
		splitChart.setPoints(split(s));
		distributionChart.setPoints(h == null ? null : buckets(h.distribution, true));
		holdChart.setPoints(h == null ? null : buckets(h.holdTimes, false));
		bestItemsChart.setPoints(h == null ? null : items(h.topItems));
		worstItemsChart.setPoints(h == null ? null : items(h.worstItems));
		model.set(h == null ? null : h.trades);
		nextBeforeId = h == null ? null : h.nextBeforeId;
		moreButton.setVisible(nextBeforeId != null);
		status.setText(h == null ? "" : statusLine(model.getRowCount(), h));
	}

	private void tile(String label, String value, Long sign)
	{
		RoundedPanel card = new RoundedPanel(new BorderLayout());
		card.setColors(theme.surface, theme.border);
		JPanel text = new JPanel(new GridLayout(2, 1));
		text.setOpaque(false);
		JLabel l = new JLabel(label.toUpperCase(java.util.Locale.ROOT));
		l.setForeground(theme.muted);
		l.setFont(l.getFont().deriveFont(Font.PLAIN, 11f));
		JLabel v = new JLabel(value);
		v.setFont(v.getFont().deriveFont(Font.BOLD, 15f));
		v.setForeground(sign == null || sign == 0 ? theme.text : sign > 0 ? theme.profit : theme.loss);
		text.add(l);
		text.add(v);
		card.add(text, BorderLayout.CENTER);
		card.setBorder(new EmptyBorder(8, 10, 8, 10));
		tiles.add(card);
	}

	static String statusLine(int shown, TradeHistory h)
	{
		int total = h == null || h.summary == null ? 0 : h.summary.trades;
		if (total == 0)
		{
			return "No trades in this period yet. A trade shows here once you've bought and then sold an item.";
		}
		String times = "Times are in your computer's time zone.";
		if (shown >= total)
		{
			return String.format("%,d trades. %s", total, times);
		}
		return String.format("Showing the latest %,d of %,d trades in the table; totals and charts cover all of them. %s",
			shown, total, times);
	}

	static String signed(long v)
	{
		return (v > 0 ? "+" : "") + String.format("%,d gp", v);
	}

	static List<TradeCharts.Point> running(TradeHistory h, ZoneId zone)
	{
		List<TradeCharts.Point> out = new ArrayList<>();
		if (h.running == null)
		{
			return out;
		}
		DateTimeFormatter f = DateTimeFormatter.ofPattern("dd MMM HH:mm").withZone(zone);
		for (TradeHistory.Point p : h.running)
		{
			Instant at = TradeHistory.parse(p.at);
			out.add(new TradeCharts.Point(at == null ? "" : f.format(at), p.netProfit));
		}
		return out;
	}

	static List<TradeCharts.Point> split(TradeHistory.Summary s)
	{
		List<TradeCharts.Point> out = new ArrayList<>();
		if (s.trades == 0)
		{
			return out;
		}
		out.add(new TradeCharts.Point("Gross", s.grossProfit));
		out.add(new TradeCharts.Point("Tax", -s.tax));
		out.add(new TradeCharts.Point("Net", s.netProfit));
		return out;
	}

	/** Bucket counts (or net profit per bucket). Empty when no bucket has any trades. */
	static List<TradeCharts.Point> buckets(List<TradeHistory.Bucket> buckets, boolean counts)
	{
		List<TradeCharts.Point> out = new ArrayList<>();
		if (buckets == null)
		{
			return out;
		}
		boolean any = false;
		for (TradeHistory.Bucket b : buckets)
		{
			any |= b.trades > 0;
			out.add(new TradeCharts.Point(b.label, counts ? b.trades : b.netProfit));
		}
		return any ? out : new ArrayList<>();
	}

	static List<TradeCharts.Point> items(List<TradeHistory.ItemTotal> items)
	{
		List<TradeCharts.Point> out = new ArrayList<>();
		if (items != null)
		{
			for (TradeHistory.ItemTotal i : items)
			{
				out.add(new TradeCharts.Point(i.item, i.netProfit));
			}
		}
		return out;
	}

	/** The value a table cell holds: numbers and times, so sorting is by value, not by text. */
	static Object cell(TradeHistory.Trade t, int column)
	{
		switch (column)
		{
			case 0:
				return t.opened();
			case 1:
				return t.closed();
			case 2:
				return t.holdMinutes;
			case 3:
				return t.account == null ? "" : t.account;
			case 4:
				return t.item == null ? "Unknown item" : t.item + (t.imported ? " (imported)" : "");
			case 5:
				return t.quantity;
			case 6:
				return t.buyPrice;
			case 7:
				return t.sellPrice;
			case 8:
				return t.cost;
			case 9:
				return t.revenue;
			case 10:
				return t.tax;
			case 11:
				return t.grossProfit;
			case 12:
				return t.netProfit;
			default:
				return "";
		}
	}

	@SuppressWarnings("unchecked")
	private static Comparator<Object> comparator(int column)
	{
		return (a, b) ->
		{
			if (a == null || b == null)
			{
				return a == null ? (b == null ? 0 : -1) : 1;
			}
			if (a instanceof Comparable && a.getClass() == b.getClass())
			{
				return ((Comparable<Object>) a).compareTo(b);
			}
			return String.valueOf(a).compareToIgnoreCase(String.valueOf(b));
		};
	}

	/** The trades table's rows. */
	static class TradesModel extends AbstractTableModel
	{
		private final List<TradeHistory.Trade> rows = new ArrayList<>();

		void set(List<TradeHistory.Trade> trades)
		{
			rows.clear();
			if (trades != null)
			{
				rows.addAll(trades);
			}
			fireTableDataChanged();
		}

		void add(List<TradeHistory.Trade> trades)
		{
			if (trades != null && !trades.isEmpty())
			{
				int from = rows.size();
				rows.addAll(trades);
				fireTableRowsInserted(from, rows.size() - 1);
			}
		}

		@Override
		public int getRowCount()
		{
			return rows.size();
		}

		@Override
		public int getColumnCount()
		{
			return COLUMNS.length;
		}

		@Override
		public String getColumnName(int column)
		{
			return COLUMNS[column];
		}

		@Override
		public Object getValueAt(int row, int column)
		{
			return cell(rows.get(row), column);
		}
	}

	/** Formats times, hold lengths and coins, and colours profit and loss. */
	private class Cells extends DefaultTableCellRenderer
	{
		@Override
		public Component getTableCellRendererComponent(JTable t, Object value, boolean selected, boolean focus,
			int row, int column)
		{
			int model = t.convertColumnIndexToModel(column);
			String text;
			if (value instanceof Instant)
			{
				text = WHEN.withZone(ZoneId.systemDefault()).format((Instant) value);
			}
			else if (model == 2 && value instanceof Long)
			{
				text = TradeHistory.hold((Long) value);
			}
			else if (value instanceof Long)
			{
				long v = (Long) value;
				text = model >= 11 ? (v > 0 ? "+" : "") + String.format("%,d", v) : String.format("%,d", v);
			}
			else
			{
				text = value == null ? "" : value.toString();
			}
			super.getTableCellRendererComponent(t, text, selected, focus, row, column);
			setHorizontalAlignment(value instanceof Long ? SwingConstants.RIGHT : SwingConstants.LEFT);
			Color fg = theme.text;
			if (model >= 11 && value instanceof Long)
			{
				long v = (Long) value;
				fg = v > 0 ? theme.profit : v < 0 ? theme.loss : theme.text;
			}
			setForeground(fg);
			setBackground(selected ? theme.raised : theme.surface);
			setBorder(new EmptyBorder(0, 6, 0, 6));
			return this;
		}
	}
}
