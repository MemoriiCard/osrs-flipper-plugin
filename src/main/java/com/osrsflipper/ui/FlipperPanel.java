package com.osrsflipper.ui;

import com.osrsflipper.api.DeviceStart;
import com.osrsflipper.api.DumpAlert;
import com.osrsflipper.api.Me;
import com.osrsflipper.api.PriceHistory;
import com.osrsflipper.api.Suggestion;
import com.osrsflipper.auth.AuthState;
import com.osrsflipper.tracking.Flip;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.QuantityFormatter;

/**
 * Side panel: account, the next step, dump alerts, a price chart, profit, then recent flips.
 *
 * <p>The simple view shows the next step and profit. The advanced view adds the reasoning, ratings,
 * "why this item?" and the full flip list. Everything is drawn in the chosen {@link Theme}, all of
 * which are dark. All methods must be called on the Swing thread.</p>
 */
public class FlipperPanel extends PluginPanel
{
	private static final int SIMPLE_FLIPS = 5;
	private static final String[] PERIODS = {"day", "week", "month"};

	private final PanelActions actions;
	private Theme theme = Theme.GRAND_EXCHANGE;
	/** Re-colours every long-lived component when the theme changes. */
	private final List<Runnable> painters = new ArrayList<>();

	private final JLabel title = new JLabel("OSRS Flipper");
	private final Pill planPill = new Pill();

	private final RoundedPanel accountCard = card();
	private final JLabel accountTitle = new JLabel();
	private final JLabel accountDetail = new JLabel();
	private final JLabel accountError = new JLabel();
	private final ThemedButton signInButton = button("Sign in", "Link this plugin to your OSRS Flipper account", ThemedButton.Style.PRIMARY);
	private final ThemedButton reopenButton = button("Open page", "Open the sign-in page again", ThemedButton.Style.SECONDARY);
	private final ThemedButton cancelButton = button("Cancel", null, ThemedButton.Style.GHOST);
	private final ThemedButton websiteButton = button("Website", "Your history, charts and plan", ThemedButton.Style.SECONDARY);
	private final ThemedButton signOutButton = button("Sign out", null, ThemedButton.Style.GHOST);
	private final JPanel announcements = column();

	private final RoundedPanel suggestionCard = card();
	private final Pill actionPill = new Pill();
	private final JLabel suggestionItem = new JLabel();
	private final JLabel suggestionPrice = new JLabel();
	private final JLabel suggestionProfit = new JLabel();
	private final JLabel suggestionReason = new JLabel();
	private final JLabel suggestionError = new JLabel();
	private final JLabel explanation = new JLabel();
	private final ThemedButton skipButton = button("Skip", "Get a different suggestion", ThemedButton.Style.SECONDARY);
	private final ThemedButton blockButton = button("Block", "Never suggest this item again. Undo it in settings under Blocked items.", ThemedButton.Style.GHOST);
	private final ThemedButton goodButton = button("Good", "Rate this suggestion as good", ThemedButton.Style.GHOST);
	private final ThemedButton badButton = button("Bad", "Rate this suggestion as bad", ThemedButton.Style.GHOST);
	private final ThemedButton whyButton = button("Why?", "Explain why this item was suggested", ThemedButton.Style.GHOST);
	private final JPanel feedbackRow = row();
	private String suggestionAction;

	private final JPanel alertList = column();

	private final RoundedPanel chartCard = card();
	private final JLabel chartTitle = new JLabel();
	private final JLabel chartLegend = new JLabel();
	private final JLabel chartError = new JLabel();
	private final PriceChart chart = new PriceChart();
	private final ThemedButton[] periodButtons = {
		button("Day", "Last 24 hours, 5-minute averages", ThemedButton.Style.SEGMENT),
		button("Week", "Last 7 days, hourly averages", ThemedButton.Style.SEGMENT),
		button("Month", "Last 30 days, hourly averages", ThemedButton.Style.SEGMENT),
	};

	private final RoundedPanel sessionTile = card();
	private final RoundedPanel allTimeTile = card();
	private final JLabel sessionValue = new JLabel();
	private final JLabel allTimeValue = new JLabel();
	private final JLabel flipsHeading = new JLabel();
	private final JPanel flipList = column();

	private boolean advanced;
	private String shownSuggestionId;
	private long sessionProfit;
	private long allTimeProfit;
	private boolean loggedIn;
	private List<Flip> flips = Collections.emptyList();
	private List<DumpAlert> alerts = Collections.emptyList();

	public FlipperPanel(PanelActions actions)
	{
		this.actions = actions;
		setLayout(new BorderLayout());
		setBorder(new EmptyBorder(10, 10, 10, 10));

		JPanel content = column();
		content.add(header());
		content.add(Box.createVerticalStrut(8));
		content.add(accountCard(actions));
		content.add(Box.createVerticalStrut(8));
		content.add(announcements);
		content.add(suggestionCard(actions));
		content.add(Box.createVerticalStrut(8));
		content.add(alertList);
		content.add(chartCard(actions));
		content.add(Box.createVerticalStrut(8));
		content.add(stats());
		content.add(Box.createVerticalStrut(10));
		content.add(flipsHeading);
		content.add(Box.createVerticalStrut(4));
		content.add(flipList);
		add(content, BorderLayout.NORTH);

		painters.add(() ->
		{
			setBackground(theme.background);
			getWrappedPanel().setBackground(theme.background);
			getScrollPane().getViewport().setBackground(theme.background);
			title.setForeground(theme.accent);
			flipsHeading.setForeground(theme.muted);
		});

		showAccount(AuthState.SIGNED_OUT, null, null, null);
		update(false, 0, 0, Collections.emptyList());
		showSuggestion(false, null, null);
		showChart(null, "day", null);
		setTheme(theme);
	}

	/** Re-colours everything. Safe to call at any time on the Swing thread. */
	public void setTheme(Theme theme)
	{
		this.theme = theme;
		for (Runnable painter : painters)
		{
			painter.run();
		}
		chart.setTheme(theme);
		// Rows built from data pick up the theme when rebuilt.
		showAlerts(alerts);
		update(loggedIn, sessionProfit, allTimeProfit, flips);
		recolorSuggestion();
		revalidate();
		repaint();
	}

	public void setAdvanced(boolean advanced)
	{
		this.advanced = advanced;
	}

	private JComponent header()
	{
		JPanel header = new JPanel(new BorderLayout());
		header.setOpaque(false);
		header.setAlignmentX(LEFT_ALIGNMENT);
		title.setFont(FontManager.getRunescapeBoldFont().deriveFont(18f));
		planPill.setFont(FontManager.getRunescapeSmallFont());
		planPill.setVisible(false);
		header.add(title, BorderLayout.WEST);
		JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 2));
		right.setOpaque(false);
		right.add(planPill);
		header.add(right, BorderLayout.EAST);
		return header;
	}

	private JComponent accountCard(PanelActions actions)
	{
		JPanel text = column();
		accountTitle.setFont(FontManager.getRunescapeBoldFont());
		stack(text, accountTitle, 2);
		stack(text, accountDetail, 2);
		stack(text, accountError, 2);
		accountCard.add(text, BorderLayout.CENTER);

		signInButton.addActionListener(e -> actions.signIn());
		reopenButton.addActionListener(e -> actions.reopenSignIn());
		cancelButton.addActionListener(e -> actions.cancelSignIn());
		websiteButton.addActionListener(e -> actions.openWebsite());
		signOutButton.addActionListener(e -> actions.signOut());
		JPanel buttons = row();
		buttons.add(signInButton);
		buttons.add(reopenButton);
		buttons.add(cancelButton);
		buttons.add(websiteButton);
		buttons.add(signOutButton);
		accountCard.add(buttons, BorderLayout.SOUTH);

		painters.add(() ->
		{
			accountCard.setColors(theme.surface, theme.border);
			accountTitle.setForeground(theme.text);
			accountDetail.setForeground(theme.muted);
			accountError.setForeground(theme.loss);
		});
		return accountCard;
	}

	private JComponent suggestionCard(PanelActions actions)
	{
		actionPill.setFont(FontManager.getRunescapeSmallFont());
		suggestionItem.setFont(FontManager.getRunescapeBoldFont());
		suggestionPrice.setFont(FontManager.getRunescapeFont());
		suggestionProfit.setFont(FontManager.getRunescapeFont());

		JPanel top = new JPanel(new BorderLayout(6, 0));
		top.setOpaque(false);
		JPanel pillWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		pillWrap.setOpaque(false);
		pillWrap.add(actionPill);
		top.add(pillWrap, BorderLayout.WEST);

		JPanel text = column();
		stack(text, top, 2);
		stack(text, suggestionItem, 2);
		stack(text, suggestionPrice, 2);
		stack(text, suggestionProfit, 2);
		stack(text, suggestionReason, 2);
		stack(text, suggestionError, 2);
		stack(text, explanation, 2);
		suggestionCard.add(text, BorderLayout.CENTER);

		skipButton.addActionListener(e -> actions.skip());
		blockButton.addActionListener(e -> actions.block());
		goodButton.addActionListener(e ->
		{
			actions.rate(1);
			goodButton.setEnabled(false);
			badButton.setEnabled(false);
		});
		badButton.addActionListener(e ->
		{
			actions.rate(-1);
			goodButton.setEnabled(false);
			badButton.setEnabled(false);
		});
		whyButton.addActionListener(e ->
		{
			whyButton.setEnabled(false);
			showExplanation("Asking...");
			actions.explain();
		});
		feedbackRow.add(skipButton);
		feedbackRow.add(blockButton);
		feedbackRow.add(goodButton);
		feedbackRow.add(badButton);
		feedbackRow.add(whyButton);
		suggestionCard.add(feedbackRow, BorderLayout.SOUTH);

		painters.add(this::recolorSuggestion);
		return suggestionCard;
	}

	private void recolorSuggestion()
	{
		Color color = suggestionAction == null ? theme.muted : theme.action(suggestionAction);
		suggestionCard.setColors(theme.surface, theme.border);
		suggestionCard.setStripe(suggestionAction == null ? null : color);
		actionPill.set(label(suggestionAction), color);
		suggestionItem.setForeground(theme.text);
		suggestionPrice.setForeground(theme.text);
		suggestionProfit.setForeground(theme.profit);
		suggestionReason.setForeground(theme.muted);
		suggestionError.setForeground(theme.loss);
		explanation.setForeground(theme.muted);
	}

	private JComponent chartCard(PanelActions actions)
	{
		chartTitle.setFont(FontManager.getRunescapeSmallFont());
		chartLegend.setFont(FontManager.getRunescapeSmallFont());
		JPanel top = new JPanel(new BorderLayout());
		top.setOpaque(false);
		top.add(chartTitle, BorderLayout.CENTER);
		top.add(chartLegend, BorderLayout.EAST);
		chartCard.add(top, BorderLayout.NORTH);
		chartCard.add(chart, BorderLayout.CENTER);

		JPanel periods = new JPanel(new GridLayout(1, PERIODS.length, 4, 0));
		periods.setOpaque(false);
		for (int i = 0; i < PERIODS.length; i++)
		{
			final String period = PERIODS[i];
			periodButtons[i].addActionListener(e -> actions.chartPeriod(period));
			periods.add(periodButtons[i]);
		}
		JPanel bottom = column();
		stack(bottom, periods, 2);
		stack(bottom, chartError, 2);
		chartCard.add(bottom, BorderLayout.SOUTH);

		painters.add(() ->
		{
			chartCard.setColors(theme.surface, theme.border);
			chartTitle.setForeground(theme.text);
			chartError.setForeground(theme.loss);
			chartLegend.setText("<html><font color='" + hex(theme.chartHigh) + "'>High</font> &nbsp;<font color='"
				+ hex(theme.chartLow) + "'>Low</font></html>");
		});
		return chartCard;
	}

	private JComponent stats()
	{
		JPanel tiles = new JPanel(new GridLayout(1, 2, 8, 0));
		tiles.setOpaque(false);
		tiles.setAlignmentX(LEFT_ALIGNMENT);
		tiles.add(tile(sessionTile, "Session", sessionValue));
		tiles.add(tile(allTimeTile, "All time", allTimeValue));
		return tiles;
	}

	private RoundedPanel tile(RoundedPanel tile, String name, JLabel value)
	{
		JLabel label = new JLabel(name);
		label.setFont(FontManager.getRunescapeSmallFont());
		value.setFont(FontManager.getRunescapeBoldFont());
		JPanel text = column();
		stack(text, label, 2);
		stack(text, value, 2);
		tile.add(text, BorderLayout.CENTER);
		painters.add(() ->
		{
			tile.setColors(theme.surface, theme.border);
			label.setForeground(theme.muted);
		});
		return tile;
	}

	/** Shows the chart, or hides the card when there is no item to chart. */
	public void showChart(PriceHistory history, String period, String error)
	{
		chartCard.setVisible(history != null || error != null);
		chartTitle.setText(history == null || history.name == null ? "Price history" : html(history.name, 110));
		chart.setHistory(history);
		chartError.setText(error == null ? "" : html(error));
		chartError.setVisible(error != null);
		for (int i = 0; i < PERIODS.length; i++)
		{
			periodButtons[i].setSelected(PERIODS[i].equals(period));
		}
		revalidate();
		repaint();
	}

	/**
	 * @param linking the code being approved, while signing in
	 * @param me      plan and announcements, once the server has answered
	 * @param error   why the last sign-in step failed, or null
	 */
	public void showAccount(AuthState state, DeviceStart linking, Me me, String error)
	{
		signInButton.setVisible(state == AuthState.SIGNED_OUT);
		reopenButton.setVisible(state == AuthState.LINKING);
		cancelButton.setVisible(state == AuthState.LINKING);
		websiteButton.setVisible(state == AuthState.SIGNED_IN);
		signOutButton.setVisible(state == AuthState.SIGNED_IN);

		String plan = null;
		switch (state)
		{
			case LINKING:
				accountTitle.setText("Code: " + (linking == null ? "" : linking.userCode));
				accountDetail.setText(html("Approve this code on the page that opened in your browser."));
				break;
			case SIGNED_IN:
				if (me == null)
				{
					accountTitle.setText("Signed in");
					accountDetail.setText("");
				}
				else if (me.accountPremium())
				{
					plan = "PREMIUM";
					accountTitle.setText("Premium");
					accountDetail.setText(html(me.seats + " game account" + (me.seats == 1 ? "" : "s")
						+ (me.premiumUntil != null && me.premiumUntil.length() >= 10 ? ", until " + me.premiumUntil.substring(0, 10) : "") + "."));
				}
				else if (me.premium)
				{
					plan = "FREE HERE";
					accountTitle.setText("Free on this account");
					accountDetail.setText(html("Your premium seats are on other game accounts. Move one on the website."));
				}
				else
				{
					plan = "FREE";
					accountTitle.setText("Free plan");
					accountDetail.setText(html("Suggestions from popular items. Premium unlocks every item and dump alerts."));
				}
				break;
			case SIGNED_OUT:
			default:
				accountTitle.setText("Get started");
				accountDetail.setText(steps(
					"Press Sign in. A page opens in your browser.",
					"Sign in there with email or Discord, then approve the code shown here.",
					"Open the Grand Exchange. Your first suggestion appears below."));
				break;
		}
		planPill.set(plan, "PREMIUM".equals(plan) ? theme.accent : theme.muted);
		accountError.setText(error == null ? "" : html(error));
		accountError.setVisible(error != null);

		announcements.removeAll();
		if (state == AuthState.SIGNED_IN && me != null && me.announcements != null)
		{
			for (Me.Announcement a : me.announcements)
			{
				RoundedPanel note = card();
				boolean warning = "warning".equals(a.level);
				note.setColors(theme.surface, theme.border);
				note.setStripe(warning ? theme.warning : theme.accent);
				JLabel label = new JLabel(html(a.message));
				label.setForeground(warning ? theme.warning : theme.text);
				note.add(label, BorderLayout.CENTER);
				stack(announcements, note, 6);
			}
		}
		announcements.setVisible(announcements.getComponentCount() > 0);
		announcements.setBorder(new EmptyBorder(0, 0, announcements.isVisible() ? 8 : 0, 0));
		announcements.revalidate();
	}

	/**
	 * @param enabled whether suggestions are turned on in settings
	 * @param error   why the latest refresh failed, or null
	 */
	public void showSuggestion(boolean enabled, Suggestion suggestion, String error)
	{
		String id = suggestion == null ? null : suggestion.suggestionId + "|" + suggestion.headline();
		boolean changed = !Objects.equals(id, shownSuggestionId);
		shownSuggestionId = id;

		String action = null;
		String item;
		String price = "";
		String profit = "";
		String reason = "";
		if (!enabled)
		{
			item = "Suggestions off";
			reason = "Turn them on in the plugin settings.";
		}
		else if (suggestion == null)
		{
			item = error == null ? "Finding your next flip..." : "No suggestion";
		}
		else
		{
			action = suggestion.itemId == null ? Suggestion.WAIT : suggestion.action;
			item = suggestion.is(Suggestion.WAIT) || suggestion.name == null ? "Nothing to do right now" : suggestion.name;
			if ((suggestion.is(Suggestion.BUY) || suggestion.is(Suggestion.SELL)) && suggestion.price != null)
			{
				price = String.format("%,d x %s gp", suggestion.quantity == null ? 0 : suggestion.quantity,
					QuantityFormatter.formatNumber(suggestion.price));
			}
			if (suggestion.expectedProfit != null && suggestion.expectedProfit > 0)
			{
				profit = "+" + gp(suggestion.expectedProfit) + " est. profit";
			}
			if (suggestion.reason != null && (advanced || suggestion.is(Suggestion.WAIT) || suggestion.is(Suggestion.ABORT)))
			{
				reason = suggestion.reason;
			}
		}
		suggestionAction = action;
		actionPill.set(label(action), action == null ? theme.muted : theme.action(action));
		suggestionItem.setText(html(item));
		suggestionPrice.setText(price);
		suggestionPrice.setVisible(!price.isEmpty());
		suggestionProfit.setText(profit);
		suggestionProfit.setVisible(!profit.isEmpty());
		suggestionReason.setText(html(reason));
		suggestionReason.setVisible(!reason.isEmpty());
		suggestionError.setText(error == null ? "" : html(error));
		suggestionError.setVisible(error != null);
		recolorSuggestion();

		boolean hasItem = enabled && suggestion != null && suggestion.itemId != null;
		boolean rateable = hasItem && suggestion.suggestionId != null;
		skipButton.setVisible(hasItem);
		blockButton.setVisible(hasItem && suggestion.is(Suggestion.BUY));
		goodButton.setVisible(advanced && rateable);
		badButton.setVisible(advanced && rateable);
		whyButton.setVisible(advanced && hasItem && suggestion.is(Suggestion.BUY));
		feedbackRow.setVisible(hasItem);
		if (changed)
		{
			goodButton.setEnabled(true);
			badButton.setEnabled(true);
			whyButton.setEnabled(true);
			showExplanation(null);
		}
		revalidate();
	}

	/** Dump alerts for premium accounts; an empty list hides the section. */
	public void showAlerts(List<DumpAlert> alerts)
	{
		this.alerts = alerts;
		alertList.removeAll();
		for (DumpAlert alert : alerts)
		{
			RoundedPanel card = card();
			card.setColors(theme.surface, theme.border);
			card.setStripe(theme.warning);
			Pill pill = new Pill();
			pill.setFont(FontManager.getRunescapeSmallFont());
			pill.set(String.format("DUMP -%.0f%%", alert.dropPercent), theme.warning);
			JPanel pillWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
			pillWrap.setOpaque(false);
			pillWrap.add(pill);
			JLabel name = new JLabel(html(alert.name));
			name.setFont(FontManager.getRunescapeBoldFont());
			name.setForeground(theme.text);
			JLabel detail = new JLabel(html(String.format("Buy %,d at %,d", alert.quantity, alert.buyPrice)));
			detail.setForeground(theme.muted);
			JLabel profit = new JLabel("+" + gp(alert.expectedProfit) + " est. profit");
			profit.setForeground(theme.profit);
			JPanel text = column();
			stack(text, pillWrap, 2);
			stack(text, name, 2);
			stack(text, detail, 2);
			stack(text, profit, 2);
			card.add(text, BorderLayout.CENTER);

			ThemedButton use = button("Use", "Make this the current suggestion", ThemedButton.Style.PRIMARY);
			use.setTheme(theme);
			use.addActionListener(e -> actions.useAlert(alert));
			ThemedButton dismiss = button("Dismiss", null, ThemedButton.Style.GHOST);
			dismiss.setTheme(theme);
			dismiss.addActionListener(e -> actions.dismissAlert(alert.id));
			JPanel buttons = row();
			buttons.add(use);
			buttons.add(dismiss);
			card.add(buttons, BorderLayout.SOUTH);
			stack(alertList, card, 6);
		}
		alertList.setVisible(!alerts.isEmpty());
		alertList.setBorder(new EmptyBorder(0, 0, alerts.isEmpty() ? 0 : 8, 0));
		alertList.revalidate();
		alertList.repaint();
	}

	public void showExplanation(String text)
	{
		explanation.setText(text == null ? "" : html(text));
		explanation.setVisible(text != null);
		if (text == null)
		{
			whyButton.setEnabled(true);
		}
	}

	public void update(boolean loggedIn, long sessionProfit, long allTimeProfit, List<Flip> recentFlips)
	{
		this.loggedIn = loggedIn;
		this.sessionProfit = sessionProfit;
		this.allTimeProfit = allTimeProfit;
		this.flips = recentFlips;

		sessionValue.setText(signedGp(sessionProfit));
		sessionValue.setForeground(sessionProfit < 0 ? theme.loss : theme.profit);
		allTimeValue.setText(signedGp(allTimeProfit));
		allTimeValue.setForeground(allTimeProfit < 0 ? theme.loss : theme.profit);
		flipsHeading.setText(loggedIn
			? (recentFlips.isEmpty() ? html("No flips yet. Buy and sell on the GE to start tracking.") : "RECENT FLIPS")
			: "Log in to see your flips.");
		flipsHeading.setFont(FontManager.getRunescapeSmallFont());

		flipList.removeAll();
		int shown = 0;
		for (Flip flip : recentFlips)
		{
			if (!advanced && shown >= SIMPLE_FLIPS)
			{
				break;
			}
			stack(flipList, flipRow(flip), 4);
			shown++;
		}
		flipList.revalidate();
		flipList.repaint();
	}

	private JComponent flipRow(Flip flip)
	{
		RoundedPanel row = new RoundedPanel(new BorderLayout(6, 0));
		row.setColors(theme.surface, null);
		row.setStripe(flip.profit < 0 ? theme.loss : theme.profit);
		row.setBorder(new EmptyBorder(6, 10, 6, 8));
		row.setAlignmentX(LEFT_ALIGNMENT);

		JLabel name = new JLabel(html(flip.itemName + " x" + QuantityFormatter.formatNumber(flip.quantity), 105));
		name.setForeground(theme.text);
		JLabel profit = new JLabel(signedGp(flip.profit));
		profit.setFont(FontManager.getRunescapeBoldFont());
		profit.setForeground(flip.profit < 0 ? theme.loss : theme.profit);

		JPanel left = column();
		stack(left, name, 2);
		if (advanced)
		{
			JLabel detail = new JLabel(QuantityFormatter.quantityToStackSize(flip.buyPrice) + " → "
				+ QuantityFormatter.quantityToStackSize(flip.sellPrice));
			detail.setFont(FontManager.getRunescapeSmallFont());
			detail.setForeground(theme.muted);
			stack(left, detail, 2);
		}
		row.add(left, BorderLayout.CENTER);
		row.add(profit, BorderLayout.EAST);
		return row;
	}

	private RoundedPanel card()
	{
		RoundedPanel card = new RoundedPanel(new BorderLayout(6, 8));
		card.setBorder(new EmptyBorder(10, 12, 10, 10));
		card.setAlignmentX(LEFT_ALIGNMENT);
		return card;
	}

	private static JPanel column()
	{
		JPanel p = new JPanel();
		p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
		p.setOpaque(false);
		p.setAlignmentX(LEFT_ALIGNMENT);
		return p;
	}

	/** Adds to a column, left-aligned, with space above everything but the first child. */
	private static void stack(JPanel column, JComponent child, int gap)
	{
		if (column.getComponentCount() > 0 && gap > 0)
		{
			column.add(Box.createVerticalStrut(gap));
		}
		child.setAlignmentX(LEFT_ALIGNMENT);
		column.add(child);
	}

	private static JPanel row()
	{
		JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
		p.setOpaque(false);
		return p;
	}

	private ThemedButton button(String text, String tooltip, ThemedButton.Style style)
	{
		ThemedButton b = new ThemedButton(text, tooltip, style);
		b.setFont(FontManager.getRunescapeSmallFont());
		painters.add(() -> b.setTheme(theme));
		return b;
	}

	private static String label(String action)
	{
		return action == null ? "" : action.toUpperCase(java.util.Locale.ROOT);
	}

	private static String hex(Color c)
	{
		return String.format("#%02X%02X%02X", c.getRed(), c.getGreen(), c.getBlue());
	}

	/** Text width inside a card, so long lines wrap instead of running off the panel. */
	private static final int CARD_TEXT_WIDTH = 150;

	/** Wraps text for a card. Server text is escaped so it can't inject Swing HTML. */
	/** A short numbered list, escaped like {@link #html(String)}. */
	static String steps(String... lines)
	{
		StringBuilder out = new StringBuilder("<html><body style='width:" + CARD_TEXT_WIDTH + "px'>");
		for (int i = 0; i < lines.length; i++)
		{
			out.append(i == 0 ? "" : "<br>").append(i + 1).append(". ")
				.append(lines[i].replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;"));
		}
		return out.append("</body></html>").toString();
	}

	static String html(String text)
	{
		return html(text, CARD_TEXT_WIDTH);
	}

	static String html(String text, int width)
	{
		return "<html><body style='width:" + width + "px'>"
			+ text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;") + "</body></html>";
	}

	private static String gp(long amount)
	{
		return QuantityFormatter.formatNumber(amount) + " gp";
	}

	private static String signedGp(long amount)
	{
		return (amount > 0 ? "+" : "") + QuantityFormatter.quantityToStackSize(amount) + " gp";
	}
}
