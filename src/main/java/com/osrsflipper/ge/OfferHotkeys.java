package com.osrsflipper.ge;

import com.osrsflipper.OsrsFlipperConfig;
import com.osrsflipper.suggestion.NextStep;
import com.osrsflipper.suggestion.SuggestionController;
import com.osrsflipper.sync.UsageCounter;
import java.awt.event.KeyEvent;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.Keybind;
import net.runelite.client.input.KeyListener;

/**
 * The quick-set key types the suggested price or quantity into the open GE prompt; the player still
 * presses Enter. The skip key asks for a different suggestion. One key press does at most one thing.
 */
@Singleton
public class OfferHotkeys implements KeyListener
{
	private final OsrsFlipperConfig config;
	private final ClientThread clientThread;
	private final GrandExchangeWidgets ge;
	private final SuggestionController suggestions;
	private final UsageCounter usage;

	@Inject
	public OfferHotkeys(OsrsFlipperConfig config, ClientThread clientThread, GrandExchangeWidgets ge,
		SuggestionController suggestions, UsageCounter usage)
	{
		this.usage = usage;
		this.config = config;
		this.clientThread = clientThread;
		this.ge = ge;
		this.suggestions = suggestions;
	}

	@Override
	public void keyTyped(KeyEvent e)
	{
	}

	@Override
	public void keyPressed(KeyEvent e)
	{
		// Enter would also submit the prompt, so it can never be a hotkey.
		if (e.getKeyCode() == KeyEvent.VK_ENTER)
		{
			return;
		}
		boolean quickSet = matches(config.quickSetKeybind(), e);
		boolean skip = matches(config.skipKeybind(), e);
		if (quickSet || skip)
		{
			clientThread.invokeLater(() -> handle(quickSet, skip));
		}
	}

	@Override
	public void keyReleased(KeyEvent e)
	{
	}

	private void handle(boolean quickSet, boolean skip)
	{
		GrandExchangeWidgets.PromptType prompt = ge.openPrompt();
		if (prompt != GrandExchangeWidgets.PromptType.NONE)
		{
			if (quickSet)
			{
				long value = NextStep.promptValue(suggestions.getCurrent(), ge.screen(),
					prompt == GrandExchangeWidgets.PromptType.PRICE);
				if (value > 0)
				{
					ge.fillPrompt(value);
					usage.add("hotkey.quick_set");
				}
			}
			return;
		}
		if (skip)
		{
			usage.add("hotkey.skip");
			suggestions.skip();
		}
	}

	private static boolean matches(Keybind keybind, KeyEvent e)
	{
		return keybind != null && keybind != Keybind.NOT_SET && keybind.matches(e);
	}
}
