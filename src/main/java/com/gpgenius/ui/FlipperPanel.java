package com.gpgenius.ui;

import com.gpgenius.api.Announcements;
import com.gpgenius.api.DeviceStart;
import com.gpgenius.api.DumpAlert;
import com.gpgenius.api.Me;
import com.gpgenius.api.PriceHistory;
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
	private final ThemedButton skipButton = button("Skip", "Get a different suggestion", ThemedButton.Style.SECONDARY);
	private final ThemedButton blockButton = button("Block", "Never suggest this item again. Undo it in settings under Blocked items.", ThemedButton.Style.GHOST);
	private final ThemedButton goodButton = button("Good", "Rate this suggestion as good", ThemedButton.Style.GHOST);
	private final ThemedButton badButton = button("Bad", "Rate this suggestion as bad", ThemedButton.Style.GHOST);
	/** Skip and Block on one row, the advanced view's ratings on a second, so nothing is cut off. */
	private final JPanel feedbackRow = column();
	private final JPanel actionRow = row();
	private final JPanel rateRow = row();
	private String suggestionAction;

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
		content.add(profileBar(actions));
		content.add(setupCard(actions));
		content.add(tourCard());
		content.add(accountCard(actions));
		content.add(Box.createVerticalStrut(8));
		content.add(announcements);
		content.add(planCard(actions));
		content.add(slotsCard());
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
		showImportMessage(null);
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
			slotsTitle.setText(html(todo == 0 ? "GE slots: all set" : "GE slots: " + todo + (todo == 1 ? " step" : " steps")));
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
			slotsNote.setText(html((next == null ? "" : "Next check-in around " + next + ". ")
				+ "Do each step yourself, in order."));
		}
		slotsCard.setVisible(show);
		slotsCard.setBorder(new EmptyBorder(10, 12, 10, 10));
		slotsTodo.revalidate();
		slotsList.revalidate();
		revalidate();
		repaint();
	}

	private JComponent importRow(PanelActions actions)
	{
		importMessage.setFont(FontManager.getRunescapeSmallFont());
		importButton.addActionListener(e ->
		{
			importButton.setEnabled(false);
			showImportMessage("Importing...");
			actions.importHistory();
		});
		JPanel buttons = row();
		buttons.add(importButton);
		stack(importRow, buttons, 0);
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
		stack(text, suggestionError, 2);
		suggestionCard.add(text, BorderLayout.CENTER);

		skipButton.addActionListener(e -> actions.skip());
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
		actionPill.set(label(suggestionAction), color);
		suggestionItem.setForeground(theme.text);
		suggestionPrice.setForeground(theme.text);
		suggestionProfit.setForeground(theme.profit);
		suggestionReason.setForeground(theme.muted);
		suggestionError.setForeground(theme.loss);
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
		planPill.set(plan, "PREMIUM".equals(plan) ? theme.accent : theme.muted);
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
			if ((suggestion.is(Suggestion.BUY) || suggestion.is(Suggestion.SELL)) && suggestion.price != null)
			{
				price = String.format("%,d x %s gp", suggestion.quantity == null ? 0 : suggestion.quantity,
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
