package com.gpgenius.api;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Fast-start presets and saved profiles, as the server sends them. A preset is only a name, a
 * one-line description and its visible starting choices; what it does to suggestions is decided on
 * the server.
 */
public final class Profiles
{
	private Profiles()
	{
	}

	/** GET /v1/profiles */
	public static class Listing
	{
		public List<Profile> profiles = new ArrayList<>();
		@SerializedName("active_id")
		public String activeId;
		public List<PresetInfo> presets = new ArrayList<>();
	}

	public static class PresetInfo
	{
		public String id;
		public String label;
		public String blurb;
		public String risk;
		@SerializedName("check_in_minutes")
		public int checkInMinutes;
	}

	public static class Profile
	{
		public String id;
		public String name;
		public String preset;
		/** The player's own choices, keyed by suggestion-request field names. */
		public Map<String, Object> choices = new LinkedHashMap<>();
		public Look look = new Look();
		public boolean active;
	}

	/**
	 * How the panel and highlights look for this profile. The highlight and accessibility fields are
	 * null when a profile was saved before they existed (or by an older server); null leaves the
	 * player's current setting alone.
	 */
	public static class Look
	{
		public boolean compact;
		/** A Theme name; unknown names keep the current theme. */
		public String theme;
		public boolean sounds = true;

		/** A HighlightPalette name. */
		@SerializedName("highlight_palette")
		public String highlightPalette;
		@SerializedName("highlight_thickness")
		public Integer highlightThickness;
		@SerializedName("highlight_padding")
		public Integer highlightPadding;
		@SerializedName("highlight_opacity")
		public Integer highlightOpacity;
		@SerializedName("highlight_fill")
		public Integer highlightFill;
		@SerializedName("highlight_pulse")
		public Boolean highlightPulse;
		@SerializedName("highlight_label")
		public Boolean highlightLabel;
		@SerializedName("highlight_prompt")
		public Boolean highlightPrompt;
		@SerializedName("highlight_spotlight")
		public Boolean highlightSpotlight;
		/** A TextScale.Size name. */
		@SerializedName("text_size")
		public String textSize;
		@SerializedName("high_contrast")
		public Boolean highContrast;
		/** SoundCue names. */
		@SerializedName("sound_cues")
		public List<String> soundCues;
		@SerializedName("sound_cue_volume")
		public Integer soundCueVolume;
	}

	/** POST /v1/profiles */
	public static class Create
	{
		public String name;
		public String preset;
		public Map<String, Object> choices = new LinkedHashMap<>();
		public Look look;
		public boolean activate = true;
	}

	/** POST /v1/profiles/{id}: only the non-null parts change. */
	public static class Update
	{
		public String name;
		public String preset;
		public Map<String, Object> choices;
		public Look look;
	}
}
