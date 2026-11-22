package com.gpgenius.ge;

import com.gpgenius.GpGeniusConfig;
import com.gpgenius.suggestion.NextStep;
import com.gpgenius.suggestion.SuggestionController;
import com.gpgenius.sync.UsageCounter;
import java.awt.event.KeyEvent;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.Keybind;
import net.runelite.client.input.KeyListener;

/**
 * The quick-set key types the suggested price or quantity into the open GE prompt; the player still
 * presses Enter. The skip key asks for a different suggestion. The optional panel keys only change
 * plugin settings or ask the server again; none of them touch the game. One key press does at most
 * one thing.
 */
@Singleton
public class OfferHotkeys implements KeyListener
{
	private final GpGeniusConfig config;
	private final ClientThread clientThread;
	private final GrandExchangeWidgets ge;
	private final SuggestionController suggestions;
	private final UsageCounter usage;
	private volatile Shortcuts shortcuts;

	/** Panel actions behind the optional hotkeys. Called on the client thread. */
	public interface Shortcuts
	{
		void toggleHighlights();

		void refresh();

		void nextProfile();

		void toggleView();

		void toggleCompact();
	}

	@Inject
	public OfferHotkeys(GpGeniusConfig config, ClientThread clientThread, GrandExchangeWidgets ge,
		SuggestionController suggestions, UsageCounter usage)
	{
		this.usage = usage;
		this.config = config;
		this.clientThread = clientThread;
		this.ge = ge;
		this.suggestions = suggestions;
	}

	public void setShortcuts(Shortcuts shortcuts)
	{
		this.shortcuts = shortcuts;
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
			return;
		}
		Runnable shortcut = shortcut(e);
		if (shortcut != null)
		{
			// Keep the key out of the chatbox.
			e.consume();
			clientThread.invokeLater(shortcut);
		}
	}

	private Runnable shortcut(KeyEvent e)
	{
		Shortcuts s = shortcuts;
		if (s == null)
		{
			return null;
		}
		if (matches(config.toggleHighlightKeybind(), e))
		{
			return () -> track("hotkey.toggle_highlight", s::toggleHighlights);
		}
		if (matches(config.refreshKeybind(), e))
		{
			return () -> track("hotkey.refresh", s::refresh);
		}
		if (matches(config.nextProfileKeybind(), e))
		{
			return () -> track("hotkey.next_profile", s::nextProfile);
		}
		if (matches(config.toggleViewKeybind(), e))
		{
			return () -> track("hotkey.toggle_view", s::toggleView);
		}
		if (matches(config.toggleCompactKeybind(), e))
		{
			return () -> track("hotkey.toggle_compact", s::toggleCompact);
		}
		return null;
	}

	private void track(String name, Runnable action)
	{
		usage.add(name);
		action.run();
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
