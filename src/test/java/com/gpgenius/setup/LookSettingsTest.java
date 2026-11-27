package com.gpgenius.setup;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import com.google.gson.Gson;
import com.gpgenius.GpGeniusConfig;
import com.gpgenius.api.Profiles;
import com.gpgenius.notify.SoundCue;
import com.gpgenius.ui.HighlightPalette;
import com.gpgenius.ui.TextScale;
import com.gpgenius.ui.Theme;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import org.junit.Test;

public class LookSettingsTest
{
	private static final GpGeniusConfig CUSTOM = new GpGeniusConfig()
	{
		@Override
		public Theme theme()
		{
			return Theme.AMETHYST;
		}

		@Override
		public HighlightPalette highlightPalette()
		{
			return HighlightPalette.RED_GREEN_SAFE;
		}

		@Override
		public int highlightThickness()
		{
			return 6;
		}

		@Override
		public boolean highlightPulse()
		{
			return false;
		}

		@Override
		public boolean highlightSpotlight()
		{
			return true;
		}

		@Override
		public TextScale.Size textSize()
		{
			return TextScale.Size.LARGE;
		}

		@Override
		public boolean highContrast()
		{
			return true;
		}

		@Override
		public Set<SoundCue> soundCues()
		{
			return EnumSet.of(SoundCue.OFFER_DONE, SoundCue.OUTBID);
		}
	};

	@Test
	public void everyLookSettingRoundTripsThroughTheServer()
	{
		Gson gson = new Gson();
		Profiles.Look sent = gson.fromJson(gson.toJson(LookSettings.from(CUSTOM)), Profiles.Look.class);
		Map<String, Object> settings = LookSettings.toSettings(sent);
		assertEquals(Theme.AMETHYST, settings.get("theme"));
		assertEquals(HighlightPalette.RED_GREEN_SAFE, settings.get("highlightPalette"));
		assertEquals(6, settings.get("highlightThickness"));
		assertEquals(false, settings.get("highlightPulse"));
		assertEquals(true, settings.get("highlightSpotlight"));
		assertEquals(TextScale.Size.LARGE, settings.get("textSize"));
		assertEquals(true, settings.get("highContrast"));
		assertEquals(EnumSet.of(SoundCue.OFFER_DONE, SoundCue.OUTBID), settings.get("soundCues"));
		assertEquals(com.gpgenius.ui.PanelLayout.DETAILED, settings.get("panelLayout"));
		assertTrue(((Set<?>) settings.get("panelParts")).contains(com.gpgenius.ui.PanelLayout.Part.LIMITS));
		// Every key written back is one that saves the profile when edited.
		assertTrue(LookSettings.KEYS.containsAll(settings.keySet()));
		assertEquals(LookSettings.KEYS.size(), settings.size());
	}

	@Test
	public void jsonUsesTheServersNames()
	{
		String json = new Gson().toJson(LookSettings.from(CUSTOM));
		assertTrue(json.contains("\"highlight_palette\":\"RED_GREEN_SAFE\""));
		assertTrue(json.contains("\"text_size\":\"LARGE\""));
		assertTrue(json.contains("\"sound_cues\":[\"OFFER_DONE\",\"OUTBID\"]"));
	}

	@Test
	public void profilesSavedBeforeThisLeaveNewSettingsAlone()
	{
		Profiles.Look old = new Gson().fromJson("{\"compact\":true,\"theme\":\"MATRIX\",\"sounds\":false}",
			Profiles.Look.class);
		Map<String, Object> settings = LookSettings.toSettings(old);
		assertEquals(true, settings.get("compactLayout"));
		assertEquals(Theme.MATRIX, settings.get("theme"));
		assertFalse(settings.containsKey("highlightPalette"));
		assertFalse(settings.containsKey("textSize"));
		assertFalse(settings.containsKey("soundCues"));
		assertFalse(settings.containsKey("panelLayout"));
		assertFalse(settings.containsKey("panelParts"));
	}

	@Test
	public void badValuesAreClampedOrDropped()
	{
		Profiles.Look look = new Profiles.Look();
		look.highlightThickness = 99;
		look.highlightOpacity = 0;
		look.highlightPalette = "RAINBOW";
		look.textSize = "huge";
		look.soundCues = Arrays.asList("OUTBID", "AIRHORN");
		Map<String, Object> settings = LookSettings.toSettings(look);
		assertEquals(8, settings.get("highlightThickness"));
		assertEquals(10, settings.get("highlightOpacity"));
		assertFalse(settings.containsKey("highlightPalette"));
		assertFalse(settings.containsKey("textSize"));
		assertEquals(EnumSet.of(SoundCue.OUTBID), settings.get("soundCues"));
	}

	@Test
	public void defaultsAreGentle()
	{
		GpGeniusConfig d = new GpGeniusConfig()
		{
		};
		assertEquals(HighlightPalette.THEME, d.highlightPalette());
		assertTrue(d.highlightLabel());
		assertFalse(d.highlightSpotlight());
		assertTrue(d.soundCues().isEmpty());
		assertEquals(TextScale.Size.NORMAL, d.textSize());
		assertFalse(d.highContrast());
	}

	@Test
	public void nextProfileWrapsRound()
	{
		Profiles.Profile a = profile("a");
		Profiles.Profile b = profile("b");
		assertEquals("b", LookSettings.nextProfileId(Arrays.asList(a, b), "a"));
		assertEquals("a", LookSettings.nextProfileId(Arrays.asList(a, b), "b"));
		assertEquals("a", LookSettings.nextProfileId(Arrays.asList(a, b), null));
		assertNull(LookSettings.nextProfileId(Arrays.asList(a), "a"));
		assertNull(LookSettings.nextProfileId(null, "a"));
	}

	@Test
	public void hotkeysStayOnThisComputer()
	{
		for (String key : LookSettings.KEYS)
		{
			assertFalse(key, key.endsWith("Keybind"));
		}
	}

	private static Profiles.Profile profile(String id)
	{
		Profiles.Profile p = new Profiles.Profile();
		p.id = id;
		return p;
	}
}
