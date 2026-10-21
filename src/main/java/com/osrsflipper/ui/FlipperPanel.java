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
import java.util.Collections;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.QuantityFormatter;

/**
 * Side panel: sign-in and plan, the current suggestion, profit, then recent flips.
 *
 * <p>The simple view shows the next step and profit. The advanced view adds the reasoning, ratings,
 * "why this item?" and the full flip list. All methods must be called on the Swing thread.</p>
 */
public class FlipperPanel extends PluginPanel
{
	private static final Color PROFIT = new Color(0x4C, 0xBB, 0x17);
	private static final Color LOSS = new Color(0xE0, 0x4B, 0x4B);
	private static final Color NOTICE = new Color(0xFF, 0xB8, 0x4D);
	private static final int SIMPLE_FLIPS = 5;

	private final JLabel accountTitle = new JLabel();
	private final JLabel accountDetail = new JLabel();
	private final JLabel accountError = new JLabel();
	private final JButton signInButton = button("Sign in", "Link this plugin to your OSRS Flipper account");
	private final JButton reopenButton = button("Open page", "Open the sign-in page again");
	private final JButton cancelButton = button("Cancel", null);
	private final JButton websiteButton = button("Website", "Your history, charts and plan");
	private final JButton signOutButton = button("Sign out", null);
	private final JPanel announcements = new JPanel(new GridLayout(0, 1, 0, 4));

	private final JLabel suggestionHeadline = new JLabel();
	private final JLabel suggestionDetail = new JLabel();
	private final JLabel suggestionError = new JLabel();
	private final JLabel explanation = new JLabel();
	private final JButton skipButton = button("Skip", "Get a different suggestion");
	private final JButton goodButton = button("Good", "Rate this suggestion as good");
	private final JButton badButton = button("Bad", "Rate this suggestion as bad");
	private final JButton whyButton = button("Why?", "Explain why this item was suggested");
	private final JPanel feedbackRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));

	private final JPanel chartCard = card();
	private final JLabel chartTitle = new JLabel();
	private final JLabel chartError = new JLabel();
	private final PriceChart chart = new PriceChart();
	private final JButton[] periodButtons = {
		button("Day", "Last 24 hours, 5-minute averages"),
		button("Week", "Last 7 days, hourly averages"),
		button("Month", "Last 30 days, hourly averages"),
	};
	private static final String[] PERIODS = {"day", "week", "month"};

	private final JPanel alertList = new JPanel(new GridLayout(0, 1, 0, 4));
	private final PanelActions actions;

	private final JLabel sessionLabel = new JLabel();
	private final JLabel allTimeLabel = new JLabel();
	private final JLabel status = new JLabel();
	private final JPanel flipList = new JPanel();

	private boolean advanced;
	private String shownSuggestionId;

	public FlipperPanel(PanelActions actions)
	{
		this.actions = actions;
		setLayout(new BorderLayout());
		setBorder(new EmptyBorder(10, 10, 10, 10));

		JPanel header = new JPanel();
		header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
		JLabel title = new JLabel("OSRS Flipper");
		title.setFont(FontManager.getRunescapeBoldFont());
		header.add(left(title));
		header.add(gap());
		header.add(accountCard(actions));
		header.add(gap());
		header.add(announcements);
		header.add(suggestionCard(actions));
		header.add(gap());
		header.add(chartCard(actions));
		header.add(gap());
		alertList.setOpaque(false);
		alertList.setAlignmentX(LEFT_ALIGNMENT);
		header.add(alertList);
		header.add(left(sessionLabel));
		header.add(left(allTimeLabel));
		header.add(left(status));
		header.setBorder(new EmptyBorder(0, 0, 10, 0));
		add(header, BorderLayout.NORTH);

		announcements.setOpaque(false);
		flipList.setLayout(new BoxLayout(flipList, BoxLayout.Y_AXIS));
		add(flipList, BorderLayout.CENTER);

		showAccount(AuthState.SIGNED_OUT, null, null, null);
		update(false, 0, 0, Collections.emptyList());
		showSuggestion(false, null, null);
		showChart(null, "day", null);
	}

	private JPanel chartCard(PanelActions actions)
	{
		chartTitle.setFont(FontManager.getRunescapeSmallFont());
		chartTitle.setForeground(Color.WHITE);
		chartError.setForeground(LOSS);
		JPanel top = new JPanel(new BorderLayout());
		top.setOpaque(false);
		top.add(chartTitle, BorderLayout.CENTER);
		JLabel legend = new JLabel("<html><font color='#FF9F40'>High</font> <font color='#4DA3FF'>Low</font></html>");
		legend.setFont(FontManager.getRunescapeSmallFont());
		top.add(legend, BorderLayout.EAST);
		chartCard.add(top, BorderLayout.NORTH);
		chartCard.add(chart, BorderLayout.CENTER);

		JPanel bottom = new JPanel(new BorderLayout());
		bottom.setOpaque(false);
		JPanel periods = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
		periods.setOpaque(false);
		for (int i = 0; i < PERIODS.length; i++)
		{
			final String period = PERIODS[i];
			periodButtons[i].addActionListener(e -> actions.chartPeriod(period));
			periods.add(periodButtons[i]);
		}
		bottom.add(periods, BorderLayout.NORTH);
		bottom.add(chartError, BorderLayout.SOUTH);
		chartCard.add(bottom, BorderLayout.SOUTH);
		return chartCard;
	}

	/** Shows the chart, or hides the card when there is no item to chart. */
	public void showChart(PriceHistory history, String period, String error)
	{
		chartCard.setVisible(history != null || error != null);
		chartTitle.setText(history == null || history.name == null ? "Price history" : html(history.name));
		chart.setHistory(history);
		chartError.setText(error == null ? "" : html(error));
		for (int i = 0; i < PERIODS.length; i++)
		{
			periodButtons[i].setEnabled(!PERIODS[i].equals(period));
		}
		revalidate();
		repaint();
	}

	private JPanel accountCard(PanelActions actions)
	{
		JPanel card = card();
		JPanel text = new JPanel(new GridLayout(0, 1, 0, 2));
		text.setOpaque(false);
		accountTitle.setFont(FontManager.getRunescapeBoldFont());
		accountTitle.setForeground(Color.WHITE);
		accountDetail.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		accountError.setForeground(LOSS);
		text.add(accountTitle);
		text.add(accountDetail);
		text.add(accountError);
		card.add(text, BorderLayout.CENTER);

		JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
		buttons.setOpaque(false);
		signInButton.addActionListener(e -> actions.signIn());
		reopenButton.addActionListener(e -> actions.reopenSignIn());
		cancelButton.addActionListener(e -> actions.cancelSignIn());
		websiteButton.addActionListener(e -> actions.openWebsite());
		signOutButton.addActionListener(e -> actions.signOut());
		buttons.add(signInButton);
		buttons.add(reopenButton);
		buttons.add(cancelButton);
		buttons.add(websiteButton);
		buttons.add(signOutButton);
		card.add(buttons, BorderLayout.SOUTH);
		return card;
	}

	private JPanel suggestionCard(PanelActions actions)
	{
		JPanel card = card();
		JPanel text = new JPanel(new GridLayout(0, 1, 0, 2));
		text.setOpaque(false);
		suggestionHeadline.setFont(FontManager.getRunescapeBoldFont());
		suggestionHeadline.setForeground(Color.WHITE);
		suggestionDetail.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		suggestionError.setForeground(LOSS);
		explanation.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		text.add(suggestionHeadline);
		text.add(suggestionDetail);
		text.add(suggestionError);
		text.add(explanation);
		card.add(text, BorderLayout.CENTER);

		skipButton.addActionListener(e -> actions.skip());
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
		feedbackRow.setOpaque(false);
		feedbackRow.add(skipButton);
		feedbackRow.add(goodButton);
		feedbackRow.add(badButton);
		feedbackRow.add(whyButton);
		card.add(feedbackRow, BorderLayout.SOUTH);
		return card;
	}

	public void setAdvanced(boolean advanced)
	{
		this.advanced = advanced;
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
					accountTitle.setText("Premium");
					accountDetail.setText(html("Premium on " + me.seats + " game account" + (me.seats == 1 ? "" : "s")
						+ (me.premiumUntil != null && me.premiumUntil.length() >= 10 ? ", until " + me.premiumUntil.substring(0, 10) : "") + "."));
				}
				else if (me.premium)
				{
					accountTitle.setText("Free on this account");
					accountDetail.setText(html("Your premium seats are on other game accounts. Move one on the website."));
				}
				else
				{
					accountTitle.setText("Free plan");
					accountDetail.setText(html("Suggestions from popular items. Premium unlocks every item and dump alerts."));
				}
				break;
			case SIGNED_OUT:
			default:
				accountTitle.setText("Not signed in");
				accountDetail.setText(html("Sign in to get suggestions and see your history on the website."));
				break;
		}
		accountError.setText(error == null ? "" : html(error));
		accountError.setVisible(error != null);

		announcements.removeAll();
		if (state == AuthState.SIGNED_IN && me != null && me.announcements != null)
		{
			for (Me.Announcement a : me.announcements)
			{
				JLabel label = new JLabel(html(a.message));
				label.setForeground("warning".equals(a.level) ? NOTICE : ColorScheme.LIGHT_GRAY_COLOR);
				label.setBorder(new EmptyBorder(0, 0, 6, 0));
				announcements.add(label);
			}
		}
		announcements.revalidate();
	}

	/**
	 * @param enabled whether suggestions are turned on in settings
	 * @param error   why the latest refresh failed, or null
	 */
	public void showSuggestion(boolean enabled, Suggestion suggestion, String error)
	{
		String id = suggestion == null ? null : suggestion.suggestionId + "|" + suggestion.headline();
		boolean changed = !java.util.Objects.equals(id, shownSuggestionId);
		shownSuggestionId = id;
		if (!enabled)
		{
			suggestionHeadline.setText("Suggestions off");
			suggestionDetail.setText("Turn them on in the plugin settings.");
		}
		else if (suggestion == null)
		{
			suggestionHeadline.setText(error == null ? "Getting a suggestion..." : "No suggestion");
			suggestionDetail.setText("");
		}
		else
		{
			suggestionHeadline.setText(suggestion.headline());
			String detail = "";
			if (suggestion.expectedProfit != null && suggestion.expectedProfit > 0)
			{
				detail = "Est. profit " + gp(suggestion.expectedProfit) + ".";
			}
			if (advanced && suggestion.reason != null)
			{
				detail += (detail.isEmpty() ? "" : " ") + suggestion.reason;
			}
			else if (suggestion.is(Suggestion.WAIT) || suggestion.is(Suggestion.ABORT))
			{
				detail = suggestion.reason == null ? "" : suggestion.reason;
			}
			suggestionDetail.setText(html(detail));
		}
		suggestionError.setText(error == null ? "" : html(error));
		suggestionError.setVisible(error != null);

		boolean hasItem = enabled && suggestion != null && suggestion.itemId != null;
		boolean rateable = hasItem && suggestion.suggestionId != null;
		skipButton.setVisible(hasItem);
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
	}

	/** Dump alerts for premium accounts; an empty list hides the section. */
	public void showAlerts(List<DumpAlert> alerts)
	{
		alertList.removeAll();
		for (DumpAlert alert : alerts)
		{
			JPanel card = card();
			card.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createMatteBorder(0, 3, 0, 0, NOTICE), new EmptyBorder(6, 8, 6, 8)));
			JLabel title = new JLabel(html(String.format("Dump: %s, %.0f%% down", alert.name, alert.dropPercent)));
			title.setForeground(NOTICE);
			JLabel detail = new JLabel(html(String.format("Buy %,d at %,d, est. profit %s",
				alert.quantity, alert.buyPrice, gp(alert.expectedProfit))));
			detail.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
			JPanel text = new JPanel(new GridLayout(0, 1, 0, 2));
			text.setOpaque(false);
			text.add(title);
			text.add(detail);
			card.add(text, BorderLayout.CENTER);

			JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
			buttons.setOpaque(false);
			JButton use = button("Use", "Make this the current suggestion");
			use.addActionListener(e -> actions.useAlert(alert));
			JButton dismiss = button("Dismiss", null);
			dismiss.addActionListener(e -> actions.dismissAlert(alert.id));
			buttons.add(use);
			buttons.add(dismiss);
			card.add(buttons, BorderLayout.SOUTH);
			alertList.add(card);
		}
		alertList.setVisible(!alerts.isEmpty());
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
		sessionLabel.setText("Session profit: " + gp(sessionProfit));
		sessionLabel.setForeground(sessionProfit < 0 ? LOSS : PROFIT);
		allTimeLabel.setText("All-time profit: " + gp(allTimeProfit));
		allTimeLabel.setForeground(allTimeProfit < 0 ? LOSS : PROFIT);
		status.setText(loggedIn
			? (recentFlips.isEmpty() ? html("No flips yet. Buy and sell on the GE to start tracking.") : "Recent flips")
			: "Log in to see your flips.");
		status.setForeground(ColorScheme.LIGHT_GRAY_COLOR);

		flipList.removeAll();
		int shown = 0;
		for (Flip flip : recentFlips)
		{
			if (!advanced && shown >= SIMPLE_FLIPS)
			{
				break;
			}
			flipList.add(row(flip));
			shown++;
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
		if (advanced)
		{
			left.add(detail);
		}
		row.add(left, BorderLayout.CENTER);
		row.add(profit, BorderLayout.EAST);
		return row;
	}

	private static JPanel card()
	{
		JPanel card = new JPanel(new BorderLayout(6, 6));
		card.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		card.setBorder(new EmptyBorder(8, 8, 8, 8));
		card.setAlignmentX(LEFT_ALIGNMENT);
		return card;
	}

	private static JPanel left(JLabel label)
	{
		JPanel p = new JPanel(new BorderLayout());
		p.setOpaque(false);
		p.add(label, BorderLayout.WEST);
		p.setAlignmentX(LEFT_ALIGNMENT);
		return p;
	}

	private static JPanel gap()
	{
		JPanel p = new JPanel();
		p.setOpaque(false);
		p.setBorder(new EmptyBorder(4, 0, 0, 0));
		p.setAlignmentX(LEFT_ALIGNMENT);
		return p;
	}

	private static JButton button(String text, String tooltip)
	{
		JButton b = new JButton(text);
		b.setFocusable(false);
		if (tooltip != null)
		{
			b.setToolTipText(tooltip);
		}
		return b;
	}

	/** Wraps text for the narrow panel. Server text is escaped so it can't inject Swing HTML. */
	static String html(String text)
	{
		return "<html>" + text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;") + "</html>";
	}

	private static String gp(long amount)
	{
		return QuantityFormatter.formatNumber(amount) + " gp";
	}
}
