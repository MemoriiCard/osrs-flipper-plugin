package com.gpgenius.ui;

import com.gpgenius.setup.Onboarding;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.FontManager;

/**
 * The first-run guide in the side panel (see {@link Onboarding}): one question per step, each
 * prefilled from what the plugin could see, then a summary and "Get my first suggestion".
 * The quick preset buttons stay available underneath for players who want one click.
 */
public class OnboardingCard extends RoundedPanel
{
	private final JLabel stepLabel = new JLabel();
	private final JLabel title = new JLabel();
	private final JLabel question = new JLabel();
	private final JLabel hint = new JLabel();
	private final JLabel error = new JLabel();
	private final JPanel body = column();

	private final JCheckBox allCoins = new JCheckBox("Use all the coins I carry");
	private final JTextField cashField = new JTextField(10);
	private final List<ThemedButton> riskButtons = new ArrayList<>();
	private final JComboBox<String> checkInBox = new JComboBox<>();
	private final JComboBox<Integer> slotsBox = new JComboBox<>();

	private final ThemedButton back = new ThemedButton("Back", null, ThemedButton.Style.GHOST);
	private final ThemedButton next = new ThemedButton("Next", null, ThemedButton.Style.PRIMARY);
	private final ThemedButton skip = new ThemedButton("Skip", "Close the guide and keep the current settings.", ThemedButton.Style.GHOST);
	private final ThemedButton presets = new ThemedButton("Use a preset instead", "One click with a ready-made style.", ThemedButton.Style.GHOST);
	private final List<JComponent> mutedText = new ArrayList<>();

	private Theme theme = Theme.GRAND_EXCHANGE;
	private Onboarding.Step step = Onboarding.Step.CASH;
	private Onboarding.Detected detected = new Onboarding.Detected();
	private Onboarding.Choices choices = new Onboarding.Choices();
	private boolean cashEdited;

	public OnboardingCard(Consumer<Onboarding.Choices> onFinish, Runnable onSkip, Runnable onPresets)
	{
		super(new BorderLayout());
		setAlignmentX(LEFT_ALIGNMENT);
		setBorder(new EmptyBorder(10, 12, 10, 10));
		stepLabel.setFont(FontManager.getRunescapeSmallFont());
		title.setFont(FontManager.getRunescapeBoldFont());
		question.setFont(FontManager.getRunescapeSmallFont());
		hint.setFont(FontManager.getRunescapeSmallFont());
		error.setFont(FontManager.getRunescapeSmallFont());
		mutedText.add(stepLabel);
		mutedText.add(question);
		mutedText.add(hint);

		JPanel text = column();
		stack(text, stepLabel, 0);
		stack(text, title, 2);
		stack(text, question, 2);
		stack(text, body, 6);
		stack(text, hint, 6);
		stack(text, error, 2);

		for (int minutes : Onboarding.CHECK_INS)
		{
			checkInBox.addItem(Onboarding.checkInLabel(minutes));
		}
		for (String risk : Onboarding.RISKS)
		{
			ThemedButton b = new ThemedButton(risk.charAt(0) + risk.substring(1).toLowerCase(java.util.Locale.ROOT),
				riskTip(risk), ThemedButton.Style.SEGMENT);
			b.setFont(FontManager.getRunescapeSmallFont());
			b.addActionListener(e ->
			{
				choices.risk = risk;
				showRisk();
			});
			riskButtons.add(b);
		}
		allCoins.setOpaque(false);
		allCoins.setFont(FontManager.getRunescapeSmallFont());
		allCoins.addActionListener(e -> cashField.setEnabled(!allCoins.isSelected()));
		cashField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener()
		{
			@Override
			public void insertUpdate(javax.swing.event.DocumentEvent e)
			{
				cashEdited = true;
			}

			@Override
			public void removeUpdate(javax.swing.event.DocumentEvent e)
			{
				cashEdited = true;
			}

			@Override
			public void changedUpdate(javax.swing.event.DocumentEvent e)
			{
			}
		});

		for (ThemedButton b : new ThemedButton[]{back, next, skip, presets})
		{
			b.setFont(FontManager.getRunescapeSmallFont());
		}
		back.addActionListener(e ->
		{
			step = step.back();
			render();
		});
		next.addActionListener(e ->
		{
			if (!read())
			{
				return;
			}
			if (step == Onboarding.Step.DONE)
			{
				onFinish.accept(choices);
				return;
			}
			step = step.next();
			render();
		});
		skip.addActionListener(e -> onSkip.run());
		presets.addActionListener(e -> onPresets.run());

		JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
		buttons.setOpaque(false);
		buttons.add(back);
		buttons.add(next);
		JPanel more = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
		more.setOpaque(false);
		more.add(skip);
		more.add(presets);
		stack(text, buttons, 8);
		stack(text, more, 2);
		add(text, BorderLayout.CENTER);
		render();
	}

	/** Starts the guide again from the first step with fresh starting values. */
	public void start(Onboarding.Detected detected, String currentRisk, int currentCheckIn)
	{
		this.detected = detected;
		this.choices = Onboarding.defaults(detected, currentRisk, currentCheckIn);
		this.step = Onboarding.Step.CASH;
		this.cashEdited = false;
		allCoins.setSelected(true);
		cashField.setText(detected.total() > 0 ? Onboarding.gp(detected.total()).replace(" gp", "") : "");
		cashEdited = false;
		render();
	}

	/**
	 * New coin and slot counts, e.g. the bank was opened. Fills in the cash box unless the player
	 * already typed in it, and never moves them off their step.
	 */
	public void detected(Onboarding.Detected d)
	{
		boolean slotsChanged = d.slotsAvailable() != detected.slotsAvailable();
		this.detected = d;
		if (!cashEdited)
		{
			cashField.setText(d.total() > 0 ? Onboarding.gp(d.total()).replace(" gp", "") : "");
			cashEdited = false;
		}
		if (slotsChanged)
		{
			choices.slotsAvailable = d.slotsAvailable();
			choices.slotsToUse = Math.min(Math.max(1, choices.slotsToUse), d.slotsAvailable());
		}
		render();
	}

	public void setTheme(Theme theme)
	{
		this.theme = theme;
		setColors(theme.surface, theme.border);
		setStripe(theme.accent);
		title.setForeground(theme.text);
		for (JComponent c : mutedText)
		{
			c.setForeground(theme.muted);
		}
		allCoins.setForeground(theme.text);
		error.setForeground(theme.loss);
		for (ThemedButton b : riskButtons)
		{
			b.setTheme(theme);
		}
		for (ThemedButton b : new ThemedButton[]{back, next, skip, presets})
		{
			b.setTheme(theme);
		}
		repaint();
	}

	/** Reads the current step's answer. False (with a message) if it can't be used. */
	private boolean read()
	{
		error.setText("");
		switch (step)
		{
			case CASH:
				if (allCoins.isSelected())
				{
					choices.cashCap = 0;
					return true;
				}
				long cap = Onboarding.parseGp(cashField.getText());
				if (cap <= 0)
				{
					error.setText(html("Type an amount such as 25m or 500k, or tick \"Use all the coins I carry\"."));
					return false;
				}
				choices.cashCap = cap;
				return true;
			case TIMEFRAME:
				int i = checkInBox.getSelectedIndex();
				choices.checkInMinutes = Onboarding.CHECK_INS.get(Math.max(0, i));
				return true;
			case SLOTS:
				Integer n = (Integer) slotsBox.getSelectedItem();
				choices.slotsToUse = n == null ? choices.slotsAvailable : n;
				return true;
			default:
				return true;
		}
	}

	private void render()
	{
		body.removeAll();
		stepLabel.setText(step == Onboarding.Step.DONE ? "Ready" : String.format("Step %d of 4", step.number()));
		title.setText(step.title);
		question.setText(html(step.question));
		hint.setText("");
		back.setVisible(step != Onboarding.Step.CASH);
		next.setText(step == Onboarding.Step.DONE ? "Get my first suggestion" : "Next");
		switch (step)
		{
			case CASH:
				stack(body, allCoins, 0);
				JPanel cashRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
				cashRow.setOpaque(false);
				JLabel up = new JLabel("Or up to");
				up.setFont(FontManager.getRunescapeSmallFont());
				up.setForeground(theme.muted);
				cashRow.add(up);
				cashRow.add(cashField);
				cashField.setEnabled(!allCoins.isSelected());
				stack(body, cashRow, 4);
				hint.setText(html(Onboarding.cashHint(detected)));
				break;
			case RISK:
				JPanel seg = new JPanel(new GridLayout(1, riskButtons.size(), 2, 0));
				seg.setOpaque(false);
				for (ThemedButton b : riskButtons)
				{
					seg.add(b);
				}
				stack(body, seg, 0);
				showRisk();
				break;
			case TIMEFRAME:
				checkInBox.setSelectedIndex(Math.max(0, Onboarding.CHECK_INS.indexOf(choices.checkInMinutes)));
				stack(body, checkInBox, 0);
				hint.setText(html("Pick how often you'll really come back. You can change it any time in the settings."));
				break;
			case SLOTS:
				slotsBox.removeAllItems();
				for (int n = detected.slotsAvailable(); n >= 1; n--)
				{
					slotsBox.addItem(n);
				}
				slotsBox.setSelectedItem(Math.min(choices.slotsToUse, detected.slotsAvailable()));
				JPanel slotRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
				slotRow.setOpaque(false);
				slotRow.add(slotsBox);
				JLabel of = new JLabel("of " + detected.slotsAvailable() + " slots");
				of.setFont(FontManager.getRunescapeSmallFont());
				of.setForeground(theme.muted);
				slotRow.add(of);
				stack(body, slotRow, 0);
				hint.setText(html(Onboarding.slotsHint(detected) + " The rest stay free for your own trades."));
				break;
			case DONE:
				for (String line : Onboarding.summary(choices))
				{
					JLabel l = new JLabel("• " + line);
					l.setFont(FontManager.getRunescapeSmallFont());
					l.setForeground(theme.text);
					stack(body, l, 2);
				}
				hint.setText(html("Everything here is in the plugin settings too."));
				break;
		}
		body.revalidate();
		revalidate();
		repaint();
	}

	private void showRisk()
	{
		for (int i = 0; i < riskButtons.size(); i++)
		{
			riskButtons.get(i).setSelected(Onboarding.RISKS.get(i).equals(choices.risk));
		}
		hint.setText(html(riskTip(choices.risk)));
	}

	private static String riskTip(String risk)
	{
		switch (risk)
		{
			case "LOW":
				return "Low: steady, busy items. Smaller margins, fewer surprises.";
			case "HIGH":
				return "High: jumpier items with bigger margins. Prices can move against you.";
			default:
				return "Medium: a balance of steady items and better margins.";
		}
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
