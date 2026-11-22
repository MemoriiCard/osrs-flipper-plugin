package com.gpgenius.setup;

import com.gpgenius.GpGeniusConfig;
import com.gpgenius.api.Profiles;
import com.gpgenius.notify.SoundCue;
import com.gpgenius.ui.HighlightPalette;
import com.gpgenius.ui.HighlightStyle;
import com.gpgenius.ui.TextScale;
import com.gpgenius.ui.Theme;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The look half of a saved profile: panel layout, theme, highlight style and accessibility modes, in
 * both directions between the plugin settings and what the server stores. Hotkeys are left out on
 * purpose: they depend on the keyboard, so they stay on each computer.
 */
public final class LookSettings
{
	/** Settings saved in a profile's look. Changing one of these saves the active profile. */
	public static final List<String> KEYS = Collections.unmodifiableList(Arrays.asList(
		"theme", "compactLayout", "sounds",
		"highlightPalette", "highlightThickness", "highlightPadding", "highlightOpacity", "highlightFill",
		"highlightPulse", "highlightLabel", "highlightPrompt", "highlightSpotlight",
		"textSize", "highContrast", "soundCues", "soundCueVolume"));

	private LookSettings()
	{
	}

	/** The look to save, from the current settings. */
	public static Profiles.Look from(GpGeniusConfig config)
	{
		Profiles.Look look = new Profiles.Look();
		look.compact = config.compactLayout();
		look.sounds = config.sounds();
		look.theme = config.theme() == null ? null : config.theme().name();
		look.highlightPalette = config.highlightPalette() == null ? null : config.highlightPalette().name();
		look.highlightThickness = config.highlightThickness();
		look.highlightPadding = config.highlightPadding();
		look.highlightOpacity = config.highlightOpacity();
		look.highlightFill = config.highlightFill();
		look.highlightPulse = config.highlightPulse();
		look.highlightLabel = config.highlightLabel();
		look.highlightPrompt = config.highlightPrompt();
		look.highlightSpotlight = config.highlightSpotlight();
		look.textSize = config.textSize() == null ? null : config.textSize().name();
		look.highContrast = config.highContrast();
		List<String> cues = new ArrayList<>();
		if (config.soundCues() != null)
		{
			for (SoundCue c : config.soundCues())
			{
				cues.add(c.name());
			}
		}
		look.soundCues = cues;
		look.soundCueVolume = config.soundCueVolume();
		return look;
	}

	/**
	 * The settings to write when switching to a profile, by config key. Missing or unknown values are
	 * left out so the player's current setting stays; numbers are kept inside the settings' ranges.
	 */
	public static Map<String, Object> toSettings(Profiles.Look look)
	{
		Map<String, Object> out = new LinkedHashMap<>();
		if (look == null)
		{
			return out;
		}
		out.put("compactLayout", look.compact);
		out.put("sounds", look.sounds);
		putIfNotNull(out, "theme", named(Theme.class, look.theme));
		putIfNotNull(out, "highlightPalette", named(HighlightPalette.class, look.highlightPalette));
		putIfNotNull(out, "highlightThickness",
			clamp(look.highlightThickness, HighlightStyle.MIN_THICKNESS, HighlightStyle.MAX_THICKNESS));
		putIfNotNull(out, "highlightPadding", clamp(look.highlightPadding, 0, HighlightStyle.MAX_PADDING));
		putIfNotNull(out, "highlightOpacity", clamp(look.highlightOpacity, 10, 100));
		putIfNotNull(out, "highlightFill", clamp(look.highlightFill, 0, 100));
		putIfNotNull(out, "highlightPulse", look.highlightPulse);
		putIfNotNull(out, "highlightLabel", look.highlightLabel);
		putIfNotNull(out, "highlightPrompt", look.highlightPrompt);
		putIfNotNull(out, "highlightSpotlight", look.highlightSpotlight);
		putIfNotNull(out, "textSize", named(TextScale.Size.class, look.textSize));
		putIfNotNull(out, "highContrast", look.highContrast);
		if (look.soundCues != null)
		{
			Set<SoundCue> cues = EnumSet.noneOf(SoundCue.class);
			for (String name : look.soundCues)
			{
				SoundCue c = named(SoundCue.class, name);
				if (c != null)
				{
					cues.add(c);
				}
			}
			out.put("soundCues", cues);
		}
		putIfNotNull(out, "soundCueVolume", clamp(look.soundCueVolume, 1, 100));
		return out;
	}

	/** The profile after the active one, wrapping round; null when there is nothing to switch to. */
	public static String nextProfileId(List<Profiles.Profile> profiles, String activeId)
	{
		if (profiles == null || profiles.isEmpty())
		{
			return null;
		}
		int at = -1;
		for (int i = 0; i < profiles.size(); i++)
		{
			if (profiles.get(i).id.equals(activeId))
			{
				at = i;
			}
		}
		String next = profiles.get((at + 1) % profiles.size()).id;
		return next.equals(activeId) ? null : next;
	}

	static <E extends Enum<E>> E named(Class<E> type, String name)
	{
		if (name != null)
		{
			for (E e : type.getEnumConstants())
			{
				if (e.name().equalsIgnoreCase(name.trim()))
				{
					return e;
				}
			}
		}
		return null;
	}

	private static Integer clamp(Integer v, int lo, int hi)
	{
		return v == null ? null : Math.max(lo, Math.min(hi, v));
	}

	private static void putIfNotNull(Map<String, Object> out, String key, Object value)
	{
		if (value != null)
		{
			out.put(key, value);
		}
	}
}
