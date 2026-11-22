package com.gpgenius.api;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reply from /v1/progress (and from session resets and goal changes): profit at a glance for each
 * character and all of them, the player's goal and its pace, and the suggested minimum cash stack.
 * Only results are sent; how the pace and the stack are worked out stays on the server.
 */
public class Progress
{
	/** Scope keys for {@link #scope(String)}. Anything else is a character id. */
	public static final String CURRENT = "current";
	public static final String ALL = "all";

	/** Window keys in display order: hour, 12h, day, week, month, all, session. */
	public List<String> windows = new ArrayList<>();
	public Map<String, String> labels = new LinkedHashMap<>();
	@SerializedName("current_id")
	public String currentId;
	public List<Character> characters = new ArrayList<>();
	public Character combined;
	public Goal goal;

	public static class Window
	{
		public long profit;
		public int flips;
	}

	public static class Character
	{
		/** Null for "all characters". */
		public String id;
		public String name;
		@SerializedName("session_started_at")
		public String sessionStartedAt;
		@SerializedName("session_per_hour")
		public Long sessionPerHour;
		public Map<String, Window> windows = new LinkedHashMap<>();
		@SerializedName("suggested_stack")
		public Long suggestedStack;
		@SerializedName("suggested_stack_label")
		public String suggestedStackLabel;

		public Window window(String key)
		{
			Window w = windows == null ? null : windows.get(key);
			return w == null ? new Window() : w;
		}
	}

	public static class Goal
	{
		/** profit or stack */
		public String kind;
		public long target;
		public long progress;
		public double fraction;
		public long remaining;
		public String deadline;
		@SerializedName("account_id")
		public String accountId;
		@SerializedName("account_name")
		public String accountName;
		public boolean adapt;
		/** done, missed, ahead, on_track, behind or starting */
		public String status;
		@SerializedName("status_text")
		public String statusText;
		@SerializedName("needed_per_hour")
		public Long neededPerHour;
		public String preset;
		@SerializedName("preset_label")
		public String presetLabel;
		@SerializedName("check_in_minutes")
		public Integer checkInMinutes;

		public boolean isStack()
		{
			return "stack".equals(kind);
		}

		public boolean isLive()
		{
			return !"done".equals(status) && !"missed".equals(status);
		}
	}

	/** The character the plugin is logged in to, or null if the server hasn't seen it yet. */
	public Character current()
	{
		return byId(currentId);
	}

	public Character byId(String id)
	{
		if (id == null || characters == null)
		{
			return null;
		}
		for (Character c : characters)
		{
			if (id.equals(c.id))
			{
				return c;
			}
		}
		return null;
	}

	/** current, all, or a character id. Falls back to the current character, then everyone. */
	public Character scope(String key)
	{
		if (ALL.equals(key))
		{
			return combined;
		}
		Character found = CURRENT.equals(key) ? current() : byId(key);
		if (found != null)
		{
			return found;
		}
		Character mine = current();
		return mine != null ? mine : combined;
	}

	public String label(String key)
	{
		String label = labels == null ? null : labels.get(key);
		return label == null ? key : label;
	}
}
