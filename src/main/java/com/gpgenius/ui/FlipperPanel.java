package com.gpgenius.ui;

import com.gpgenius.api.Announcements;
import com.gpgenius.api.DeviceStart;
import com.gpgenius.api.DumpAlert;
import com.gpgenius.api.Me;
import com.gpgenius.api.PriceHistory;
import com.gpgenius.api.Progress;
import com.gpgenius.api.Profiles;
import com.gpgenius.api.SlotPlan;
import com.gpgenius.api.Suggestion;
import com.gpgenius.auth.AuthState;
import com.gpgenius.auth.DataConsent;
import com.gpgenius.setup.Preset;
import com.gpgenius.tracking.Flip;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
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
 * <p>The simple view shows the next step and profit. The advanced view adds the step's detail, ratings
 * and the full flip list. Everything is drawn in the chosen {@link Theme}, all of
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

	private final JLabel title = new JLabel("GP Genius");
	private final Pill planPill = new Pill();

	private final RoundedPanel accountCard = card();
	private final JLabel accountTitle = new JLabel();
	private final JLabel accountDetail = new JLabel();
	private final JLabel accountError = new JLabel();
	private final ThemedButton signInButton = button("Sign in", "Link this plugin to your GP Genius account", ThemedButton.Style.PRIMARY);
	private final ThemedButton reopenButton = button("Open page", "Open the sign-in page again", ThemedButton.Style.SECONDARY);
	private final ThemedButton cancelButton = button("Cancel", null, ThemedButton.Style.GHOST);
	private final ThemedButton websiteButton = button("Website", "Your history, charts and plan", ThemedButton.Style.SECONDARY);
	private final ThemedButton signOutButton = button("Sign out", null, ThemedButton.Style.GHOST);
	private final ThemedButton agreeButton = button("Agree", "Agree to the data notice and carry on", ThemedButton.Style.PRIMARY);
	private final JPanel announcements = column();
	/** Announcements closed this session, hidden even before the server hears about it. */
	private final Set<String> closedAnnouncements = new HashSet<>();

	private final RoundedPanel suggestionCard = card();
	private final Pill actionPill = new Pill();
	private final JLabel suggestionItem = new JLabel();
	private final JLabel suggestionPrice = new JLabel();
	private final JLabel suggestionProfit = new JLabel();
	private final JLabel suggestionReason = new JLabel();
	private final JLabel suggestionError = new JLabel();
	private final JLabel marketWarning = new JLabel();
	private final ThemedButton skipButton = button("Skip", "Get a different suggestion", ThemedButton.Style.SECONDARY);
	private final ThemedButton whyButton = button("Why?", "Pro: why this item, from the AI market analyst", ThemedButton.Style.SECONDARY);
	private final JLabel explanation = new JLabel();
	private String explainedId;
	private final ThemedButton blockButton = button("Block", "Never suggest this item again. Undo it in settings under Blocked items.", ThemedButton.Style.GHOST);
	private final ThemedButton goodButton = button("Good", "Rate this suggestion as good", ThemedButton.Style.GHOST);
	private final ThemedButton badButton = button("Bad", "Rate this suggestion as bad", ThemedButton.Style.GHOST);
	/** Skip and Block on one row, the advanced view's ratings on a second, so nothing is cut off. */
	private final JPanel feedbackRow = column();
	private final JPanel actionRow = row();
	private final JPanel rateRow = row();
	private String suggestionAction;
	/** The pill's word and colour: the action, or an offer-upkeep fix such as RAISE. */
	private String pillText = "";
	private String pillColor;

	private final ThemedButton favouriteButton = button("Favourite", "Prefer this item when it's a good flip. Undo it in settings under Favourite items.", ThemedButton.Style.GHOST);

	private final RoundedPanel setupCard = card();
	private final JLabel setupTitle = new JLabel("Pick how you play");
	private final JLabel setupIntro = new JLabel();
	private final JPanel presetGrid = new JPanel(new GridLayout(0, 1, 0, 4));
	private final JLabel setupPresetBlurb = new JLabel();
	private final ThemedButton skipSetupButton = button("Skip", "Keep the current settings", ThemedButton.Style.GHOST);
	private final ThemedButton openSetupButton = button("Presets", "Start again from a preset", ThemedButton.Style.GHOST);
	private List<Preset> presets = Preset.BUILT_IN;

	private final JPanel profileBar = new JPanel(new BorderLayout(4, 0));
	private final JComboBox<Profiles.Profile> profileBox = new JComboBox<>();
	private final ThemedButton newProfileButton = button("New", "Save the current settings as a new profile", ThemedButton.Style.GHOST);
	private final ThemedButton renameProfileButton = button("Rename", "Rename this profile", ThemedButton.Style.GHOST);
	private final ThemedButton deleteProfileButton = button("Delete", "Delete this profile", ThemedButton.Style.GHOST);
	private final JLabel profileError = new JLabel();
	/** True while the profile menu is being refilled, so that isn't taken as the player switching. */
	private boolean fillingProfiles;
	private boolean compact;

	private final RoundedPanel tourCard = card();
	private final JLabel tourTitle = new JLabel("How it works");
	private final JLabel tourText = new JLabel();
	private final ThemedButton tourDoneButton = button("Got it", null, ThemedButton.Style.SECONDARY);

	private final RoundedPanel planCard = card();
	private final JLabel planTitle = new JLabel("Welcome back");
	private final JLabel planSummary = new JLabel();
	private final JPanel planSteps = column();
	private final JLabel planNote = new JLabel();
	private final ThemedButton hidePlanButton = button("Hide", "Go back to one step at a time", ThemedButton.Style.GHOST);
	private Suggestion.Plan plan;
	private final RoundedPanel slotsCard = card();
	private final JLabel slotsTitle = new JLabel("GE slots");
	private final JLabel slotsSummary = new JLabel();
	private final JPanel slotsTodo = column();
	private final JPanel slotsList = column();
	private final JLabel slotsNote = new JLabel();
	private SlotPlan slotPlan;

	private final RoundedPanel overnightCard = card();
	private final JLabel overnightTitle = new JLabel("Logging off?");
	private final JLabel overnightText = new JLabel();
	private final JPanel overnightHours = row();
	private final ThemedButton[] overnightHourButtons = new ThemedButton[com.gpgenius.suggestion.OvernightState.CHOICES.length];
	private final ThemedButton overnightStart = button("Plan overnight", "Slower, safer offers for every free slot, sized to your coins and how long you'll be away. You still place each one.", ThemedButton.Style.SECONDARY);
	private final ThemedButton overnightStop = button("Stop", "Back to normal suggestions", ThemedButton.Style.GHOST);
	private int overnightChoice = com.gpgenius.suggestion.OvernightState.DEFAULT_HOURS;
	private boolean overnightOn;
	private boolean overnightAvailable;

	private final ThemedButton dashboardButton = button("Trade dashboard", "Every trade you've made: times, prices, tax, gross and net profit, with charts", ThemedButton.Style.SECONDARY);
	private final ThemedButton exportButton = button("Export CSV", "Save your trades as a spreadsheet file, tax and gross and net profit included", ThemedButton.Style.GHOST);
	private final JLabel exportMessage = new JLabel();

	private final JPanel importRow = column();
	private final ThemedButton importButton = button("Import history", "Copy past flips from the Flipping Utilities plugin on this computer. Imported flips count in your profit, never in how accurate suggestions are.", ThemedButton.Style.GHOST);
	private final JLabel importMessage = new JLabel();

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

	private final RoundedPanel goalCard = card();
	private final JLabel goalTitle = new JLabel();
	private final Pill goalPill = new Pill();
	private final GoalBar goalBar = new GoalBar();
	private final JLabel goalProgress = new JLabel();
	private final JLabel goalAdvice = new JLabel();
	private final JLabel goalError = new JLabel();
	private final ThemedButton setGoalButton = button("Set a goal", "Pick a target and a date. Suggestions and check-in reminders can adapt to keep you on pace.", ThemedButton.Style.SECONDARY);
	private final ThemedButton clearGoalButton = button("Clear", "Stop tracking this goal", ThemedButton.Style.GHOST);

	private final RoundedPanel profitCard = card();
	private final JLabel profitTitle = new JLabel("Profit");
	private final JComboBox<ScopeChoice> scopeBox = new JComboBox<>();
	private final JPanel profitRows = new JPanel(new GridLayout(0, 2, 6, 3));
	private final JLabel stackLine = new JLabel();
	private final ThemedButton resetSessionButton = button("Reset session", "Start a new session for the characters shown. Every other total stays as it is.", ThemedButton.Style.GHOST);
	/** From the server while signed in; null shows only this computer's session and all-time totals. */
	private Progress progress;
	private String progressError;
	private String scopeKey = Progress.CURRENT;
	private boolean fillingScopes;

	/** One entry in the profit card's character picker. */
	private static final class ScopeChoice
	{
		final String key;
		final String label;

		ScopeChoice(String key, String label)
		{
			this.key = key;
			this.label = label;
		}

		@Override
		public String toString()
		{
			return label;
		}
	}

	private final JLabel flipsHeading = new JLabel();
	private final JPanel flipList = column();

	private boolean advanced;
	private String shownSuggestionId;
	private long sessionProfit;
	private long allTimeProfit;
	private boolean loggedIn;
	private List<Flip> flips = Collections.emptyList();
	private List<DumpAlert> alerts = Collections.emptyList();
	/** Large-text mode; follows rows added later too. */
	private final TextScale textScale = new TextScale();

	public FlipperPanel(PanelActions actions)
	{
		this.actions = actions;
		setLayout(new BorderLayout());
		setBorder(new EmptyBorder(10, 10, 10, 10));

		JPanel content = column();
		content.add(header());
		content.add(Box.createVerticalStrut(8));
		content.add(profileBar(actions));
		content.add(setupCard(actions));
		content.add(tourCard());
		content.add(accountCard(actions));
		content.add(Box.createVerticalStrut(8));
		content.add(announcements);
		content.add(goalCard(actions));
		content.add(Box.createVerticalStrut(8));
		content.add(planCard(actions));
		content.add(overnightCard(actions));
		content.add(slotsCard());
		content.add(suggestionCard(actions));
		content.add(Box.createVerticalStrut(8));
		content.add(alertList);
		content.add(chartCard(actions));
		content.add(Box.createVerticalStrut(8));
		content.add(profitCard(actions));
		content.add(Box.createVerticalStrut(10));
		content.add(flipsHeading);
		content.add(Box.createVerticalStrut(4));
		content.add(flipList);
		content.add(importRow(actions));
		add(content, BorderLayout.NORTH);

		painters.add(() ->
		{
			setBackground(theme.background);
			getWrappedPanel().setBackground(theme.background);
			getScrollPane().getViewport().setBackground(theme.background);
			title.setForeground(theme.accent);
			flipsHeading.setForeground(theme.muted);
		});

		showAccount(AuthState.SIGNED_OUT, null, null, null, false);
		update(false, 0, 0, Collections.emptyList());
		showSuggestion(false, null, null);
		showChart(null, "day", null);
		showSetup(false);
		showPlan(null);
		showSlots(null);
		showOvernight(false, false, overnightChoice);
		showExportMessage(null);
		showImportMessage(null);
		setTheme(theme);
		textScale.install(this);
	}

	/** Panel text size, 1 for normal. Safe to call at any time on the Swing thread. */
	public void setTextScale(float scale)
	{
		textScale.setScale(this, scale);
		revalidate();
		repaint();
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
		renderGoal();
		showPlan(plan);
		showSlots(slotPlan);
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
		JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 2));
		right.setOpaque(false);
		openSetupButton.addActionListener(e -> showSetup(true));
		right.add(openSetupButton);
		right.add(planPill);
		header.add(right, BorderLayout.EAST);
		return header;
	}

	private JComponent setupCard(PanelActions actions)
	{
		JPanel text = column();
		setupTitle.setFont(FontManager.getRunescapeBoldFont());
		setupIntro.setText(html("One click and your first suggestion arrives in seconds. Change anything later."));
		stack(text, setupTitle, 2);
		stack(text, setupIntro, 2);
		presetGrid.setOpaque(false);
		stack(text, presetGrid, 8);
		setupPresetBlurb.setFont(FontManager.getRunescapeSmallFont());
		stack(text, setupPresetBlurb, 6);
		skipSetupButton.addActionListener(e ->
		{
			actions.skipSetup();
			showSetup(false);
		});
		JPanel buttons = row();
		buttons.add(skipSetupButton);
		stack(text, buttons, 6);
		setupCard.add(text, BorderLayout.CENTER);
		fillPresets(actions);

		painters.add(() ->
		{
			setupCard.setColors(theme.surface, theme.border);
			setupCard.setStripe(theme.accent);
			setupTitle.setForeground(theme.text);
			setupIntro.setForeground(theme.muted);
			setupPresetBlurb.setForeground(theme.muted);
		});
		return setupCard;
	}

	/** One button per preset; hovering shows what it's for, clicking starts with it. */
	private void fillPresets(PanelActions actions)
	{
		presetGrid.removeAll();
		for (Preset preset : presets)
		{
			ThemedButton b = button(preset.label, preset.blurb, ThemedButton.Style.SECONDARY);
			b.setTheme(theme);
			b.addActionListener(e ->
			{
				actions.choosePreset(preset.id);
				showSetup(false);
				tourCard.setVisible(true);
				revalidate();
				repaint();
			});
			b.addMouseListener(new java.awt.event.MouseAdapter()
			{
				@Override
				public void mouseEntered(java.awt.event.MouseEvent e)
				{
					setupPresetBlurb.setText(html(preset.blurb));
				}
			});
			presetGrid.add(b);
		}
		setupPresetBlurb.setText(html("Hover a preset to see what it's for."));
		presetGrid.revalidate();
	}

	private JComponent profileBar(PanelActions actions)
	{
		profileBar.setOpaque(false);
		profileBar.setAlignmentX(LEFT_ALIGNMENT);
		profileBox.setFont(FontManager.getRunescapeSmallFont());
		profileBox.setToolTipText("Your saved profiles. Switching changes your settings in one click.");
		profileBox.setRenderer(new javax.swing.DefaultListCellRenderer()
		{
			@Override
			public java.awt.Component getListCellRendererComponent(javax.swing.JList<?> list, Object value, int index,
				boolean selected, boolean focused)
			{
				String name = value instanceof Profiles.Profile ? ((Profiles.Profile) value).name : "";
				return super.getListCellRendererComponent(list, name, index, selected, focused);
			}
		});
		profileBox.addActionListener(e ->
		{
			Object picked = profileBox.getSelectedItem();
			if (!fillingProfiles && picked instanceof Profiles.Profile)
			{
				actions.switchProfile(((Profiles.Profile) picked).id);
			}
		});
		newProfileButton.addActionListener(e ->
		{
			String name = askName("Name the new profile", "");
			if (name != null)
			{
				actions.newProfile(name);
			}
		});
		renameProfileButton.addActionListener(e ->
		{
			Object picked = profileBox.getSelectedItem();
			if (picked instanceof Profiles.Profile)
			{
				Profiles.Profile p = (Profiles.Profile) picked;
				String name = askName("Rename profile", p.name);
				if (name != null)
				{
					actions.renameProfile(p.id, name);
				}
			}
		});
		deleteProfileButton.addActionListener(e ->
		{
			Object picked = profileBox.getSelectedItem();
			if (picked instanceof Profiles.Profile
				&& javax.swing.JOptionPane.showConfirmDialog(this, "Delete \"" + ((Profiles.Profile) picked).name + "\"?",
				"GP Genius", javax.swing.JOptionPane.OK_CANCEL_OPTION) == javax.swing.JOptionPane.OK_OPTION)
			{
				actions.deleteProfile(((Profiles.Profile) picked).id);
			}
		});
		JPanel buttons = new JPanel(new GridLayout(1, 3, 2, 0));
		buttons.setOpaque(false);
		buttons.add(newProfileButton);
		buttons.add(renameProfileButton);
		buttons.add(deleteProfileButton);
		profileError.setFont(FontManager.getRunescapeSmallFont());
		JPanel stackPanel = column();
		stack(stackPanel, profileBox, 2);
		stack(stackPanel, buttons, 2);
		stack(stackPanel, profileError, 6);
		profileBar.add(stackPanel, BorderLayout.CENTER);
		painters.add(() -> profileError.setForeground(theme.loss));
		profileBar.setVisible(false);
		return profileBar;
	}

	private String askName(String title, String current)
	{
		Object answer = javax.swing.JOptionPane.showInputDialog(this, "Up to 40 characters.", title,
			javax.swing.JOptionPane.PLAIN_MESSAGE, null, null, current);
		if (!(answer instanceof String))
		{
			return null;
		}
		String name = ((String) answer).trim();
		return name.isEmpty() ? null : name.substring(0, Math.min(40, name.length()));
	}

	/**
	 * Saved profiles and the presets from the server. The bar is hidden until there is a profile.
	 *
	 * @param error the last profile request's error, or null
	 */
	public void showProfiles(List<Preset> presets, List<Profiles.Profile> profiles, String activeId, String error)
	{
		if (!presets.equals(this.presets))
		{
			this.presets = presets;
			fillPresets(actions);
		}
		fillingProfiles = true;
		try
		{
			profileBox.removeAllItems();
			Profiles.Profile active = null;
			for (Profiles.Profile p : profiles)
			{
				profileBox.addItem(p);
				if (p.id.equals(activeId))
				{
					active = p;
				}
			}
			profileBox.setSelectedItem(active);
		}
		finally
		{
			fillingProfiles = false;
		}
		renameProfileButton.setEnabled(profileBox.getSelectedItem() != null);
		deleteProfileButton.setEnabled(profileBox.getSelectedItem() != null);
		profileError.setText(error == null ? "" : html(error));
		profileError.setVisible(error != null);
		profileBar.setVisible(!profiles.isEmpty() || error != null);
		revalidate();
		repaint();
	}

	/** Compact hides the price chart and the flip list. */
	public void setCompact(boolean compact)
	{
		if (this.compact != compact)
		{
			this.compact = compact;
			chartCard.setVisible(false);
			update(loggedIn, sessionProfit, allTimeProfit, flips);
		}
	}

	/** Shows or hides the preset picker. */
	public void showSetup(boolean show)
	{
		setupCard.setVisible(show);
		setupCard.setBorder(new EmptyBorder(10, 12, 10, 10));
		openSetupButton.setVisible(!show);
		if (show)
		{
			tourCard.setVisible(false);
		}
		revalidate();
		repaint();
	}

	private JComponent tourCard()
	{
		JPanel text = column();
		tourTitle.setFont(FontManager.getRunescapeBoldFont());
		tourText.setText(steps(
			"The card below shows one next step: buy, sell, abort an offer, or wait.",
			"In the GE, the slot, button or item to click is highlighted. In a price or quantity box, your hotkey (E) fills in the suggested value.",
			"You click and confirm everything yourself. Nothing is done for you.",
			"Leave it and come back when a reminder arrives. If several slots are free, you get a plan for all of them."));
		stack(text, tourTitle, 2);
		stack(text, tourText, 4);
		tourCard.add(text, BorderLayout.CENTER);
		tourDoneButton.addActionListener(e ->
		{
			tourCard.setVisible(false);
			revalidate();
			repaint();
		});
		JPanel buttons = row();
		buttons.add(tourDoneButton);
		tourCard.add(buttons, BorderLayout.SOUTH);
		tourCard.setVisible(false);
		painters.add(() ->
		{
			tourCard.setColors(theme.surface, theme.border);
			tourCard.setStripe(theme.accent);
			tourTitle.setForeground(theme.text);
			tourText.setForeground(theme.muted);
		});
		return tourCard;
	}

	private JComponent planCard(PanelActions actions)
	{
		JPanel text = column();
		planTitle.setFont(FontManager.getRunescapeBoldFont());
		planSummary.setFont(FontManager.getRunescapeSmallFont());
		stack(text, planTitle, 2);
		stack(text, planSummary, 2);
		stack(text, planSteps, 6);
		stack(text, planNote, 6);
		planNote.setText(html("Do each step yourself, in order. The highlight follows step 1, and the list updates as you go."));
		planCard.add(text, BorderLayout.CENTER);
		hidePlanButton.addActionListener(e -> actions.dismissPlan());
		JPanel buttons = row();
		buttons.add(hidePlanButton);
		planCard.add(buttons, BorderLayout.SOUTH);
		painters.add(() ->
		{
			planCard.setColors(theme.surface, theme.border);
			planCard.setStripe(theme.accent);
			planTitle.setForeground(theme.text);
			planSummary.setForeground(theme.profit);
			planNote.setForeground(theme.muted);
		});
		return planCard;
	}

	/** The welcome-back plan: every step for the free slots, or null to hide it. */
	public void showPlan(Suggestion.Plan plan)
	{
		this.plan = plan;
		planSteps.removeAll();
		boolean show = plan != null && plan.steps != null && plan.steps.size() > 1;
		if (show)
		{
			planTitle.setText(html("Welcome back: " + plan.steps.size() + " steps"));
			String summary = "Uses " + QuantityFormatter.quantityToStackSize(plan.coinsUsed) + " gp";
			if (plan.expectedProfit > 0)
			{
				summary += ", about +" + QuantityFormatter.quantityToStackSize(plan.expectedProfit) + " gp";
			}
			planSummary.setText(summary);
			for (int i = 0; i < plan.steps.size(); i++)
			{
				Suggestion step = plan.steps.get(i);
				if (step == null)
				{
					continue;
				}
				JLabel line = new JLabel(html((i + 1) + ". " + step.headline()));
				line.setFont(i == 0 ? FontManager.getRunescapeBoldFont() : FontManager.getRunescapeFont());
				line.setForeground(i == 0 && step.action != null ? theme.action(step.action) : theme.text);
				stack(planSteps, line, 2);
			}
		}
		planCard.setVisible(show);
		planCard.setBorder(new EmptyBorder(10, 12, 10, 10));
		planSteps.revalidate();
		revalidate();
		repaint();
	}

	private JComponent slotsCard()
	{
		JPanel text = column();
		slotsTitle.setFont(FontManager.getRunescapeBoldFont());
		slotsSummary.setFont(FontManager.getRunescapeSmallFont());
		slotsNote.setFont(FontManager.getRunescapeSmallFont());
		stack(text, slotsTitle, 2);
		stack(text, slotsSummary, 2);
		stack(text, slotsTodo, 6);
		stack(text, slotsList, 6);
		stack(text, slotsNote, 6);
		slotsCard.add(text, BorderLayout.CENTER);
		painters.add(() ->
		{
			slotsCard.setColors(theme.surface, theme.border);
			slotsCard.setStripe(theme.accent);
			slotsTitle.setForeground(theme.text);
			slotsSummary.setForeground(theme.profit);
			slotsNote.setForeground(theme.muted);
		});
		return slotsCard;
	}

	/** The slot manager: a to-do list for this check-in and what each slot holds after it. Null hides it. */
	public void showSlots(SlotPlan plan)
	{
		this.slotPlan = plan;
		slotsTodo.removeAll();
		slotsList.removeAll();
		boolean show = plan != null && plan.slots != null && !plan.slots.isEmpty();
		if (show)
		{
			int todo = plan.steps == null ? 0 : plan.steps.size();
			String heading = plan.isOvernight() ? "Overnight plan" : "GE slots";
			slotsTitle.setText(html(todo == 0 ? heading + ": all set" : heading + ": " + todo + (todo == 1 ? " step" : " steps")));
			String summary = "";
			if (plan.coinsUsed > 0)
			{
				summary = "Places " + QuantityFormatter.quantityToStackSize(plan.coinsUsed) + " gp";
				if (plan.expectedProfit > 0)
				{
					summary += ", about +" + QuantityFormatter.quantityToStackSize(plan.expectedProfit) + " gp";
				}
			}
			slotsSummary.setText(summary);
			slotsSummary.setVisible(!summary.isEmpty());
			if (plan.steps != null)
			{
				for (SlotPlan.Step step : plan.steps)
				{
					JLabel line = new JLabel(html((step.order == null ? "" : step.order + ". ") + step.todo()));
					line.setFont(FontManager.getRunescapeFont());
					line.setForeground(step.loss ? theme.loss : theme.text);
					stack(slotsTodo, line, 2);
				}
			}
			for (SlotPlan.Step slot : plan.slots)
			{
				JLabel line = new JLabel(html("Slot " + (slot.slot + 1) + ": " + slot.status()));
				line.setFont(FontManager.getRunescapeSmallFont());
				line.setForeground(theme.muted);
				stack(slotsList, line, 1);
			}
			if (plan.holds != null)
			{
				for (SlotPlan.Hold hold : plan.holds)
				{
					JLabel line = new JLabel(html("Holding " + hold.name + " out of the GE for now"));
					line.setFont(FontManager.getRunescapeSmallFont());
					line.setForeground(theme.muted);
					stack(slotsList, line, 1);
				}
			}
			String next = plan.nextCheckIn(java.time.ZoneId.systemDefault());
			if (plan.isOvernight())
			{
				slotsNote.setText(html((next == null ? "" : "Back around " + next + ". ")
					+ "Do each step yourself, in order, then log off."));
			}
			else
			{
				slotsNote.setText(html((next == null ? "" : "Next check-in around " + next + ". ")
					+ "Do each step yourself, in order."));
			}
		}
		slotsCard.setVisible(show);
		slotsCard.setBorder(new EmptyBorder(10, 12, 10, 10));
		slotsTodo.revalidate();
		slotsList.revalidate();
		revalidate();
		repaint();
	}

	private JComponent overnightCard(PanelActions actions)
	{
		JPanel text = column();
		overnightTitle.setFont(FontManager.getRunescapeBoldFont());
		overnightText.setFont(FontManager.getRunescapeSmallFont());
		int[] choices = com.gpgenius.suggestion.OvernightState.CHOICES;
		for (int i = 0; i < choices.length; i++)
		{
			final int hours = choices[i];
			ThemedButton b = button(hours + "h", "Away for about " + hours + " hours", ThemedButton.Style.SEGMENT);
			b.addActionListener(e ->
			{
				overnightChoice = hours;
				showOvernight(overnightAvailable, overnightOn, overnightChoice);
			});
			overnightHourButtons[i] = b;
			overnightHours.add(b);
		}
		overnightStart.addActionListener(e -> actions.startOvernight(overnightChoice));
		overnightStop.addActionListener(e -> actions.stopOvernight());
		JPanel buttons = row();
		buttons.add(overnightStart);
		buttons.add(overnightStop);
		stack(text, overnightTitle, 2);
		stack(text, overnightText, 2);
		stack(text, overnightHours, 6);
		stack(text, buttons, 6);
		overnightCard.add(text, BorderLayout.CENTER);
		painters.add(() ->
		{
			overnightCard.setColors(theme.surface, theme.border);
			overnightCard.setStripe(theme.chartLow);
			overnightTitle.setForeground(theme.text);
			overnightText.setForeground(theme.muted);
		});
		return overnightCard;
	}

	/**
	 * The overnight card. Hidden until the player is signed in with suggestions on.
	 *
	 * @param available whether overnight mode can be used right now
	 * @param on        whether it is on
	 * @param hours     hours away, shown as the selected choice
	 */
	public void showOvernight(boolean available, boolean on, int hours)
	{
		overnightAvailable = available;
		overnightOn = on;
		overnightChoice = hours;
		overnightTitle.setText(html(on ? "Overnight mode: " + hours + "h away" : "Logging off?"));
		overnightText.setText(html(on
			? "Suggestions and the GE slot plan are set for a night away. Place each step, then log off."
			: "Get slower, safer offers for every free slot, sized to your coins and the time away."));
		int[] choices = com.gpgenius.suggestion.OvernightState.CHOICES;
		for (int i = 0; i < choices.length; i++)
		{
			overnightHourButtons[i].setSelected(choices[i] == hours);
		}
		overnightHours.setVisible(!on);
		overnightStart.setVisible(!on);
		overnightStop.setVisible(on);
		overnightCard.setVisible(available);
		overnightCard.setBorder(new EmptyBorder(10, 12, 10, 10));
		revalidate();
		repaint();
	}

	/** A message under the export button, or null to clear it. Re-enables the button. */
	public void showExportMessage(String message)
	{
		exportMessage.setText(message == null ? "" : html(message));
		exportMessage.setVisible(message != null);
		if (!"Exporting...".equals(message))
		{
			exportButton.setEnabled(true);
		}
		revalidate();
	}

	private JComponent importRow(PanelActions actions)
	{
		dashboardButton.addActionListener(e -> actions.openTradeDashboard());
		exportButton.addActionListener(e ->
		{
			exportButton.setEnabled(false);
			showExportMessage("Exporting...");
			actions.exportTrades();
		});
		exportMessage.setFont(FontManager.getRunescapeSmallFont());
		JPanel trades = row();
		trades.add(dashboardButton);
		trades.add(exportButton);
		stack(importRow, trades, 0);
		stack(importRow, exportMessage, 4);
		painters.add(() -> exportMessage.setForeground(theme.muted));

		importMessage.setFont(FontManager.getRunescapeSmallFont());
		importButton.addActionListener(e ->
		{
			importButton.setEnabled(false);
			showImportMessage("Importing...");
			actions.importHistory();
		});
		JPanel buttons = row();
		buttons.add(importButton);
		stack(importRow, buttons, 6);
		stack(importRow, importMessage, 4);
		importRow.setBorder(new EmptyBorder(8, 0, 0, 0));
		painters.add(() -> importMessage.setForeground(theme.muted));
		return importRow;
	}

	/** The result of the last import, or null to clear it. Re-enables the button. */
	public void showImportMessage(String message)
	{
		importMessage.setText(message == null ? "" : html(message));
		importMessage.setVisible(message != null);
		if (!"Importing...".equals(message))
		{
			importButton.setEnabled(true);
		}
		revalidate();
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
		agreeButton.addActionListener(e -> actions.agree());
		JPanel buttons = row();
		buttons.add(agreeButton);
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
		stack(text, marketWarning, 2);
		stack(text, explanation, 2);
		stack(text, suggestionError, 2);
		suggestionCard.add(text, BorderLayout.CENTER);
		explanation.setVisible(false);

		skipButton.addActionListener(e -> actions.skip());
		whyButton.addActionListener(e ->
		{
			whyButton.setEnabled(false);
			showExplanation("Asking the market analyst...");
			actions.explain();
		});
		blockButton.addActionListener(e -> actions.block());
		favouriteButton.addActionListener(e ->
		{
			actions.favourite();
			favouriteButton.setEnabled(false);
		});
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
		actionRow.add(skipButton);
		actionRow.add(whyButton);
		actionRow.add(favouriteButton);
		actionRow.add(blockButton);
		rateRow.add(goodButton);
		rateRow.add(badButton);
		stack(feedbackRow, actionRow, 4);
		stack(feedbackRow, rateRow, 0);
		suggestionCard.add(feedbackRow, BorderLayout.SOUTH);

		painters.add(this::recolorSuggestion);
		return suggestionCard;
	}

	private void recolorSuggestion()
	{
		Color color = suggestionAction == null ? theme.muted : theme.action(suggestionAction);
		suggestionCard.setColors(theme.surface, theme.border);
		suggestionCard.setStripe(suggestionAction == null ? null : color);
		actionPill.set(pillText, pillColor == null ? theme.muted : theme.action(pillColor));
		suggestionItem.setForeground(theme.text);
		suggestionPrice.setForeground(theme.text);
		suggestionProfit.setForeground(theme.profit);
		suggestionReason.setForeground(theme.muted);
		explanation.setForeground(theme.muted);
		suggestionError.setForeground(theme.loss);
		marketWarning.setForeground(theme.warning);
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

	private JComponent goalCard(PanelActions actions)
	{
		goalTitle.setFont(FontManager.getRunescapeBoldFont());
		goalPill.setFont(FontManager.getRunescapeSmallFont());
		goalProgress.setFont(FontManager.getRunescapeSmallFont());
		goalAdvice.setFont(FontManager.getRunescapeSmallFont());
		goalError.setFont(FontManager.getRunescapeSmallFont());
		JPanel head = new JPanel(new BorderLayout(4, 0));
		head.setOpaque(false);
		head.add(goalTitle, BorderLayout.CENTER);
		head.add(goalPill, BorderLayout.EAST);
		JPanel text = column();
		stack(text, head, 0);
		stack(text, goalBar, 6);
		stack(text, goalProgress, 4);
		stack(text, goalAdvice, 4);
		stack(text, goalError, 4);
		goalCard.add(text, BorderLayout.CENTER);
		setGoalButton.addActionListener(e -> askGoal(actions));
		clearGoalButton.addActionListener(e ->
		{
			if (javax.swing.JOptionPane.showConfirmDialog(this, "Stop tracking this goal?", "GP Genius",
				javax.swing.JOptionPane.OK_CANCEL_OPTION) == javax.swing.JOptionPane.OK_OPTION)
			{
				actions.clearGoal();
			}
		});
		JPanel buttons = row();
		buttons.add(setGoalButton);
		buttons.add(clearGoalButton);
		goalCard.add(buttons, BorderLayout.SOUTH);
		painters.add(() ->
		{
			goalCard.setColors(theme.surface, theme.border);
			goalCard.setStripe(theme.accent);
			goalTitle.setForeground(theme.text);
			goalProgress.setForeground(theme.muted);
			goalAdvice.setForeground(theme.text);
			goalError.setForeground(theme.loss);
		});
		goalCard.setVisible(false);
		return goalCard;
	}

	/** The goal form. Every offer is still placed by the player; the goal only shapes suggestions. */
	private void askGoal(PanelActions actions)
	{
		JComboBox<String> kind = new JComboBox<>(new String[]{"Make this much profit", "Reach this cash stack"});
		javax.swing.JTextField amount = new javax.swing.JTextField(10);
		amount.setToolTipText("For example 100m or 1.5b");
		java.util.List<com.gpgenius.goal.GoalInput.Deadline> days =
			com.gpgenius.goal.GoalInput.deadlines(java.time.ZonedDateTime.now());
		JComboBox<com.gpgenius.goal.GoalInput.Deadline> by =
			new JComboBox<>(days.toArray(new com.gpgenius.goal.GoalInput.Deadline[0]));
		by.setSelectedIndex(com.gpgenius.goal.GoalInput.indexOf(days, java.time.DayOfWeek.FRIDAY));
		JComboBox<String> who = new JComboBox<>(new String[]{"This character", "All my characters"});
		javax.swing.JCheckBox adapt = new javax.swing.JCheckBox("Adapt suggestions and check-ins to the goal", true);
		Progress.Goal current = progress == null ? null : progress.goal;
		if (current != null)
		{
			kind.setSelectedIndex(current.isStack() ? 1 : 0);
			amount.setText(QuantityFormatter.quantityToStackSize(current.target));
			who.setSelectedIndex(current.accountId == null ? 1 : 0);
			adapt.setSelected(current.adapt);
		}
		JPanel form = new JPanel(new GridLayout(0, 1, 0, 4));
		form.add(new JLabel("Goal"));
		form.add(kind);
		form.add(new JLabel("Amount (such as 100m or 1.5b)"));
		form.add(amount);
		form.add(new JLabel("By the end of"));
		form.add(by);
		form.add(new JLabel("For"));
		form.add(who);
		form.add(adapt);
		while (true)
		{
			int answer = javax.swing.JOptionPane.showConfirmDialog(this, form, "Set a goal",
				javax.swing.JOptionPane.OK_CANCEL_OPTION, javax.swing.JOptionPane.PLAIN_MESSAGE);
			if (answer != javax.swing.JOptionPane.OK_OPTION)
			{
				return;
			}
			Long target = com.gpgenius.goal.GoalInput.parseAmount(amount.getText());
			Object picked = by.getSelectedItem();
			if (target == null || !(picked instanceof com.gpgenius.goal.GoalInput.Deadline))
			{
				javax.swing.JOptionPane.showMessageDialog(this, "Enter an amount such as 100m or 1.5b.", "GP Genius",
					javax.swing.JOptionPane.WARNING_MESSAGE);
				continue;
			}
			String deadline = ((com.gpgenius.goal.GoalInput.Deadline) picked).at
				.format(java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME);
			actions.setGoal(kind.getSelectedIndex() == 1 ? "stack" : "profit", target, deadline,
				who.getSelectedIndex() == 1, adapt.isSelected());
			return;
		}
	}

	private JComponent profitCard(PanelActions actions)
	{
		profitTitle.setFont(FontManager.getRunescapeBoldFont());
		scopeBox.setFont(FontManager.getRunescapeSmallFont());
		scopeBox.setToolTipText("Show one character or all of them together");
		scopeBox.addActionListener(e ->
		{
			Object picked = scopeBox.getSelectedItem();
			if (!fillingScopes && picked instanceof ScopeChoice)
			{
				scopeKey = ((ScopeChoice) picked).key;
				renderProfit();
			}
		});
		resetSessionButton.addActionListener(e ->
		{
			String who = Progress.ALL.equals(scopeKey) ? "every character" : "this character";
			if (javax.swing.JOptionPane.showConfirmDialog(this, "Start a new session for " + who + "?", "GP Genius",
				javax.swing.JOptionPane.OK_CANCEL_OPTION) == javax.swing.JOptionPane.OK_OPTION)
			{
				actions.resetSession(scopeKey);
			}
		});
		profitRows.setOpaque(false);
		stackLine.setFont(FontManager.getRunescapeSmallFont());
		JPanel head = new JPanel(new BorderLayout(4, 0));
		head.setOpaque(false);
		head.add(profitTitle, BorderLayout.WEST);
		head.add(scopeBox, BorderLayout.CENTER);
		JPanel text = column();
		stack(text, head, 0);
		stack(text, profitRows, 6);
		stack(text, stackLine, 6);
		profitCard.add(text, BorderLayout.CENTER);
		JPanel buttons = row();
		buttons.add(resetSessionButton);
		profitCard.add(buttons, BorderLayout.SOUTH);
		painters.add(() ->
		{
			profitCard.setColors(theme.surface, theme.border);
			profitTitle.setForeground(theme.text);
			stackLine.setForeground(theme.text);
		});
		return profitCard;
	}

	/** Profit at a glance, the goal and the suggested stack from the server. Null falls back to local totals. */
	public void showProgress(Progress progress, String error)
	{
		this.progress = progress;
		this.progressError = error;
		fillingScopes = true;
		scopeBox.removeAllItems();
		java.util.List<ScopeChoice> choices = new ArrayList<>();
		choices.add(new ScopeChoice(Progress.CURRENT, "This character"));
		if (progress != null && progress.characters != null && progress.characters.size() > 1)
		{
			choices.add(new ScopeChoice(Progress.ALL, "All characters"));
			for (Progress.Character c : progress.characters)
			{
				if (c.id != null && !c.id.equals(progress.currentId))
				{
					choices.add(new ScopeChoice(c.id, c.name));
				}
			}
		}
		ScopeChoice selected = choices.get(0);
		for (ScopeChoice c : choices)
		{
			scopeBox.addItem(c);
			if (c.key.equals(scopeKey))
			{
				selected = c;
			}
		}
		scopeKey = selected.key;
		scopeBox.setSelectedItem(selected);
		scopeBox.setVisible(choices.size() > 1);
		fillingScopes = false;
		renderProfit();
		renderGoal();
	}

	private void renderProfit()
	{
		profitRows.removeAll();
		Progress.Character shown = progress == null ? null : progress.scope(scopeKey);
		if (shown == null)
		{
			profitRow("Session", sessionProfit, null);
			profitRow("All time", allTimeProfit, null);
			stackLine.setVisible(false);
		}
		else
		{
			java.util.List<String> keys = progress.windows == null || progress.windows.isEmpty()
				? java.util.Arrays.asList("session", "hour", "12h", "day", "week", "month", "all")
				: progress.windows;
			// Session first: it's the one players watch while they flip.
			if (keys.contains("session"))
			{
				profitRow(progress.label("session"), shown.window("session").profit,
					shown.sessionPerHour == null ? null : signedGp(shown.sessionPerHour) + "/hr");
			}
			for (String key : keys)
			{
				if (!"session".equals(key))
				{
					Progress.Window w = shown.window(key);
					profitRow(progress.label(key), w.profit, w.flips == 1 ? "1 flip" : w.flips + " flips");
				}
			}
			String tier = shown.suggestedStackLabel;
			stackLine.setText(tier == null ? "" : html("Suggested minimum cash stack: " + tier
				+ ". Keep at least this much flipping to hold on to the gains you've reached."));
			stackLine.setVisible(tier != null && !compact);
		}
		resetSessionButton.setVisible(loggedIn);
		profitCard.setBorder(new EmptyBorder(10, 12, 10, 10));
		profitRows.revalidate();
		profitRows.repaint();
		revalidate();
	}

	private void profitRow(String name, long value, String detail)
	{
		JLabel label = new JLabel(name);
		label.setFont(FontManager.getRunescapeSmallFont());
		label.setForeground(theme.muted);
		JLabel amount = new JLabel(signedGp(value));
		amount.setFont(FontManager.getRunescapeBoldFont());
		amount.setForeground(value < 0 ? theme.loss : value > 0 ? theme.profit : theme.text);
		amount.setHorizontalAlignment(JLabel.RIGHT);
		if (detail != null)
		{
			amount.setToolTipText(detail);
			label.setToolTipText(detail);
		}
		profitRows.add(label);
		profitRows.add(amount);
	}

	private void renderGoal()
	{
		boolean signedIn = progress != null;
		Progress.Goal g = signedIn ? progress.goal : null;
		goalCard.setVisible(signedIn && !compact || g != null);
		if (g == null)
		{
			goalTitle.setText(html("No goal set"));
			goalPill.set("", theme.muted);
			goalBar.setVisible(false);
			goalProgress.setText(html("Pick a target and a date, such as 100M by Friday. You'll see your pace here."));
			goalAdvice.setVisible(false);
			setGoalButton.setText("Set a goal");
			clearGoalButton.setVisible(false);
		}
		else
		{
			goalTitle.setText(html(com.gpgenius.goal.GoalText.title(g)));
			Color pillColor = "behind".equals(g.status) || "missed".equals(g.status) ? theme.warning
				: "done".equals(g.status) || "ahead".equals(g.status) ? theme.profit : theme.accent;
			goalPill.set(g.statusText == null ? "" : g.statusText, pillColor);
			goalBar.set(g.fraction, theme.raised, theme.profit);
			goalBar.setVisible(true);
			goalProgress.setText(html(com.gpgenius.goal.GoalText.progress(g, java.time.Instant.now())));
			String advice = com.gpgenius.goal.GoalText.advice(g);
			goalAdvice.setText(html(advice));
			goalAdvice.setVisible(!advice.isEmpty());
			setGoalButton.setText(g.isLive() ? "Change" : "New goal");
			clearGoalButton.setVisible(true);
		}
		goalError.setText(progressError == null ? "" : html(progressError));
		goalError.setVisible(progressError != null);
		goalCard.setBorder(new EmptyBorder(10, 12, 10, 10));
		revalidate();
		repaint();
	}

	/** Shows the chart, or hides the card when there is no item to chart. */
	public void showChart(PriceHistory history, String period, String error)
	{
		chartCard.setVisible(!compact && (history != null || error != null));
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
	 * @param consented whether the player has agreed to the current data notice
	 */
	public void showAccount(AuthState state, DeviceStart linking, Me me, String error, boolean consented)
	{
		signInButton.setText(consented ? "Sign in" : "Agree & sign in");
		signInButton.setVisible(state == AuthState.SIGNED_OUT);
		agreeButton.setVisible(state == AuthState.SIGNED_IN && !consented);
		reopenButton.setVisible(state == AuthState.LINKING);
		cancelButton.setVisible(state == AuthState.LINKING);
		websiteButton.setVisible(state == AuthState.SIGNED_IN);
		signOutButton.setVisible(state == AuthState.SIGNED_IN);
		// Until sign-in finishes there's nothing to suggest; the getting-started steps say what to do.
		suggestionCard.setVisible(state == AuthState.SIGNED_IN && consented);

		String plan = null;
		switch (state)
		{
			case LINKING:
				accountTitle.setText("Code: " + (linking == null ? "" : linking.userCode));
				accountDetail.setText(html("Approve this code on the page that opened in your browser."));
				break;
			case SIGNED_IN:
				if (!consented)
				{
					accountTitle.setText("Please review");
					accountDetail.setText(html(DataConsent.NOTICE));
				}
				else if (me == null)
				{
					accountTitle.setText("Signed in");
					accountDetail.setText("");
				}
				else if (me.accountPremium())
				{
					plan = me.pro() ? "PRO" : "PREMIUM";
					accountTitle.setText(me.pro() ? "Pro" : "Premium");
					accountDetail.setText(html(me.paidPlanText()));
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
					accountDetail.setText(html(me.freePlanText()));
				}
				break;
			case SIGNED_OUT:
			default:
				if (!consented)
				{
					accountTitle.setText("Before you sign in");
					accountDetail.setText(html(DataConsent.NOTICE));
					break;
				}
				accountTitle.setText("Get started");
				accountDetail.setText(steps(
					"Press Sign in. A page opens in your browser.",
					"Sign in there with email or Discord, then approve the code shown here.",
					"Open the Grand Exchange. Your first suggestion appears below."));
				break;
		}
		planPill.set(plan, "PREMIUM".equals(plan) || "PRO".equals(plan) ? theme.accent : theme.muted);
		accountError.setText(error == null ? "" : html(error));
		accountError.setVisible(error != null);

		announcements.removeAll();
		if (state == AuthState.SIGNED_IN && me != null)
		{
			for (Me.Announcement a : Announcements.visible(me.announcements, closedAnnouncements))
			{
				stack(announcements, announcementCard(a), 6);
			}
		}
		announcements.setVisible(announcements.getComponentCount() > 0);
		announcements.setBorder(new EmptyBorder(0, 0, announcements.isVisible() ? 8 : 0, 0));
		announcements.revalidate();
	}

	private RoundedPanel announcementCard(Me.Announcement a)
	{
		RoundedPanel note = card();
		Color stripe = a.isCritical() ? theme.loss : a.isWarning() ? theme.warning : theme.accent;
		note.setColors(theme.surface, theme.border);
		note.setStripe(stripe);
		String text = a.title == null || a.title.isEmpty() ? a.message : a.title + ": " + a.message;
		JLabel label = new JLabel(html(text));
		label.setForeground(a.isCritical() ? theme.loss : a.isWarning() ? theme.warning : theme.text);
		note.add(label, BorderLayout.CENTER);
		JPanel buttons = row();
		if (a.linkUrl != null && !a.linkUrl.isEmpty())
		{
			ThemedButton more = button("Read more", "Opens in your browser", ThemedButton.Style.GHOST);
			more.setTheme(theme);
			more.addActionListener(e -> actions.openAnnouncementLink(a.linkUrl));
			buttons.add(more);
		}
		if (a.canDismiss())
		{
			ThemedButton close = button("Close", "Hide this message here and on the website", ThemedButton.Style.GHOST);
			close.setTheme(theme);
			close.addActionListener(e ->
			{
				// Hide it straight away; the server remembers it for next time.
				closedAnnouncements.add(a.id);
				announcements.remove(note);
				announcements.setVisible(announcements.getComponentCount() > 0);
				announcements.revalidate();
				announcements.repaint();
				actions.dismissAnnouncement(a.id);
			});
			buttons.add(close);
		}
		if (buttons.getComponentCount() > 0)
		{
			note.add(buttons, BorderLayout.SOUTH);
		}
		return note;
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
			if ((suggestion.is(Suggestion.BUY) || suggestion.is(Suggestion.SELL) || suggestion.fix() != null)
				&& suggestion.price != null && suggestion.quantity != null)
			{
				price = String.format("%s%,d x %s gp", suggestion.fix() == null ? "" : "New: ", suggestion.quantity,
					QuantityFormatter.formatNumber(suggestion.price));
			}
			if (suggestion.expectedProfit != null && suggestion.expectedProfit > 0)
			{
				profit = "+" + gp(suggestion.expectedProfit) + " est. profit";
			}
			// Until the first flip is done, every step comes with its reason, as part of the first-run tour.
			boolean firstFlip = loggedIn && flips.isEmpty();
			if (suggestion.reason != null && (advanced || firstFlip || suggestion.loss || suggestion.is(Suggestion.WAIT) || suggestion.is(Suggestion.ABORT)))
			{
				reason = suggestion.reason;
			}
			for (String line : suggestion.details())
			{
				reason = reason.isEmpty() ? line : reason + "\n" + line;
			}
		}
		suggestionAction = action;
		boolean fixed = suggestion != null && enabled && suggestion.fix() != null;
		pillText = fixed ? suggestion.pillLabel() : label(action);
		pillColor = fixed ? suggestion.colorAction() : action;
		actionPill.set(pillText, pillColor == null ? theme.muted : theme.action(pillColor));
		suggestionItem.setText(html(item));
		suggestionPrice.setText(price);
		suggestionPrice.setVisible(!price.isEmpty());
		suggestionProfit.setText(profit);
		suggestionProfit.setVisible(!profit.isEmpty());
		suggestionReason.setText(html(reason));
		suggestionReason.setVisible(!reason.isEmpty());
		suggestionError.setText(error == null ? "" : html(error));
		suggestionError.setVisible(error != null);
		// Market warnings show in simple and advanced mode alike, even when the reason is hidden.
		String warning = enabled && suggestion != null ? String.join(" ", suggestion.warnings(reason)) : "";
		marketWarning.setText(html(warning));
		marketWarning.setVisible(!warning.isEmpty());
		recolorSuggestion();

		boolean hasItem = enabled && suggestion != null && suggestion.itemId != null;
		boolean rateable = hasItem && suggestion.suggestionId != null;
		skipButton.setVisible(hasItem);
		// Pro only: the server says whether explanations are on for this account's plan.
		whyButton.setVisible(hasItem && suggestion.explanations);
		if (changed || !whyButton.isVisible())
		{
			whyButton.setEnabled(true);
			explanation.setVisible(false);
			explainedId = id;
		}
		blockButton.setVisible(hasItem && suggestion.is(Suggestion.BUY));
		favouriteButton.setVisible(hasItem && suggestion.is(Suggestion.BUY));
		goodButton.setVisible(advanced && rateable);
		badButton.setVisible(advanced && rateable);
		rateRow.setVisible(goodButton.isVisible());
		feedbackRow.setVisible(hasItem);
		if (changed)
		{
			favouriteButton.setEnabled(true);
			goodButton.setEnabled(true);
			badButton.setEnabled(true);
		}
		revalidate();
	}

	/** The answer to "Why?" for the suggestion on show (Pro). Ignored once the suggestion has changed. */
	public void showExplanation(String text)
	{
		if (!Objects.equals(explainedId, shownSuggestionId))
		{
			return;
		}
		explanation.setText(html(text == null ? "" : text));
		explanation.setVisible(text != null && !text.isEmpty());
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

	public void update(boolean loggedIn, long sessionProfit, long allTimeProfit, List<Flip> recentFlips)
	{
		this.loggedIn = loggedIn;
		this.sessionProfit = sessionProfit;
		this.allTimeProfit = allTimeProfit;
		this.flips = recentFlips;

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
		renderProfit();
		importRow.setVisible(loggedIn && !compact);
		flipsHeading.setVisible(!compact);
		flipList.setVisible(!compact);
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

	/** Wraps text for a card. Server text is escaped so it can't inject Swing HTML. */
	static String html(String text)
	{
		return html(text, CARD_TEXT_WIDTH);
	}

	static String html(String text, int width)
	{
		return "<html><body style='width:" + width + "px'>"
			+ text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\n", "<br>")
			+ "</body></html>";
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
