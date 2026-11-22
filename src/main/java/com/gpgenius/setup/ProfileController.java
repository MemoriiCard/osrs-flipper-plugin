package com.gpgenius.setup;

import com.gpgenius.GpGeniusConfig;
import com.gpgenius.api.FlipperApiClient;
import com.gpgenius.api.Profiles;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;

/**
 * Fast-start presets and saved profiles. Profiles live on the server, so they follow the player to
 * any computer; the plugin settings show the active one and edits are saved back to it.
 *
 * <p>Picking a preset writes its visible starting choices (risk and check-in) to the settings and
 * saves a profile for it. Every suggestion then carries the profile id, and the server applies the
 * preset. Server callbacks arrive on OkHttp threads; every method here is thread-safe.
 */
@Slf4j
public class ProfileController
{
	/** Settings a profile carries. Changing one of these saves the active profile. */
	public static final List<String> PROFILE_KEYS;

	static
	{
		List<String> keys = new ArrayList<>(java.util.Arrays.asList(
			"riskLevel", "checkIn", "itemScope", "categories", "coinCap", "maxItemPercent",
			"minItemPrice", "maxItemPrice", "minProfit"));
		keys.addAll(LookSettings.KEYS);
		PROFILE_KEYS = Collections.unmodifiableList(keys);
	}

	/** Wait this long after the last edit before saving, so a spinner doesn't send a request per click. */
	static final long SAVE_DELAY_MILLIS = 3_000;

	private final GpGeniusConfig config;
	private final ConfigManager configManager;
	private final FlipperApiClient api;
	private final Runnable onChange;

	private List<Preset> presets = Preset.BUILT_IN;
	private List<Profiles.Profile> profiles = new ArrayList<>();
	private boolean loaded;
	private boolean loading;
	private String error;
	/** True while a switch writes settings, so those writes aren't saved back as edits. */
	private boolean applying;
	private long saveDueMillis;

	public ProfileController(GpGeniusConfig config, ConfigManager configManager, FlipperApiClient api, Runnable onChange)
	{
		this.config = config;
		this.configManager = configManager;
		this.api = api;
		this.onChange = onChange;
	}

	public synchronized List<Preset> getPresets()
	{
		return presets;
	}

	public synchronized List<Profiles.Profile> getProfiles()
	{
		return new ArrayList<>(profiles);
	}

	public synchronized String getError()
	{
		return error;
	}

	public synchronized boolean isLoaded()
	{
		return loaded;
	}

	public String activeId()
	{
		String id = config.activeProfileId();
		return id == null || id.trim().isEmpty() ? null : id.trim();
	}

	/** Fetches profiles and presets once after sign-in. */
	public void load()
	{
		synchronized (this)
		{
			if (loading)
			{
				return;
			}
			loading = true;
		}
		api.profiles(listing ->
		{
			synchronized (this)
			{
				loading = false;
				loaded = true;
				error = null;
				presets = Preset.fromServer(listing.presets);
				profiles = listing.profiles == null ? new ArrayList<>() : new ArrayList<>(listing.profiles);
			}
			// Another computer may have switched profile; follow the server.
			String serverActive = listing.activeId;
			if (serverActive != null && !serverActive.equals(activeId()))
			{
				Profiles.Profile p = find(serverActive);
				if (p != null)
				{
					apply(p);
				}
			}
			else if (serverActive == null && activeId() != null)
			{
				setConfig("activeProfileId", "");
			}
			onChange.run();
		}, message ->
		{
			synchronized (this)
			{
				loading = false;
				error = message;
			}
			onChange.run();
		});
	}

	/** Signed out: forget the profiles until the next sign-in. */
	public synchronized void reset()
	{
		loaded = false;
		profiles = new ArrayList<>();
		presets = Preset.BUILT_IN;
	}

	/**
	 * One click from the setup card: the preset's starting choices go into the settings straight away
	 * (so the next suggestion uses it within seconds) and a profile is saved for it in the background.
	 */
	public void choosePreset(Preset preset)
	{
		applying(() ->
		{
			setConfig("riskLevel", preset.risk);
			setConfig("checkIn", preset.checkIn);
			setConfig("setupPreset", preset.id);
			setConfig("activeProfileId", "");
			setConfig("setupDone", true);
		});
		Profiles.Create body = new Profiles.Create();
		body.name = uniqueName(preset.label);
		body.preset = preset.id;
		body.choices = ProfileChoices.toChoices(current());
		body.look = look();
		body.activate = true;
		api.createProfile(body, this::added, this::failed);
		onChange.run();
	}

	/** A new profile from the current settings, using the current preset. */
	public void create(String name)
	{
		Profiles.Create body = new Profiles.Create();
		body.name = name.trim();
		body.preset = presetId();
		body.choices = ProfileChoices.toChoices(current());
		body.look = look();
		body.activate = true;
		api.createProfile(body, this::added, this::failed);
	}

	public void switchTo(String id)
	{
		Profiles.Profile p = find(id);
		if (p == null || id.equals(activeId()))
		{
			return;
		}
		flush();
		apply(p);
		api.activateProfile(id, ignored -> { }, this::failed);
		onChange.run();
	}

	public void rename(String id, String name)
	{
		Profiles.Update body = new Profiles.Update();
		body.name = name.trim();
		api.updateProfile(id, body, this::replaced, this::failed);
	}

	public void delete(String id)
	{
		api.deleteProfile(id, () ->
		{
			synchronized (this)
			{
				profiles.removeIf(p -> p.id.equals(id));
			}
			if (id.equals(activeId()))
			{
				// The server made another profile active; pick it up.
				setConfig("activeProfileId", "");
				load();
			}
			onChange.run();
		}, this::failed);
	}

	/** A setting changed. If it belongs to the active profile, save it shortly. */
	public synchronized void settingChanged(String key, long now)
	{
		if (!applying && activeId() != null && PROFILE_KEYS.contains(key))
		{
			saveDueMillis = now + SAVE_DELAY_MILLIS;
		}
	}

	/** Call regularly; saves a pending edit once the player has stopped changing settings. */
	public void tick(long now)
	{
		synchronized (this)
		{
			if (saveDueMillis == 0 || now < saveDueMillis)
			{
				return;
			}
		}
		flush();
	}

	private void flush()
	{
		String id;
		synchronized (this)
		{
			if (saveDueMillis == 0)
			{
				return;
			}
			saveDueMillis = 0;
		}
		id = activeId();
		if (id == null)
		{
			return;
		}
		Profiles.Update body = new Profiles.Update();
		body.choices = ProfileChoices.toChoices(current());
		body.look = look();
		api.updateProfile(id, body, this::replaced, this::failed);
	}

	private void added(Profiles.Profile p)
	{
		synchronized (this)
		{
			profiles.removeIf(old -> old.id.equals(p.id));
			profiles.add(p);
			error = null;
		}
		if (p.active)
		{
			setConfig("activeProfileId", p.id);
			setConfig("setupPreset", p.preset);
		}
		onChange.run();
	}

	private void replaced(Profiles.Profile p)
	{
		synchronized (this)
		{
			for (int i = 0; i < profiles.size(); i++)
			{
				if (profiles.get(i).id.equals(p.id))
				{
					profiles.set(i, p);
				}
			}
			error = null;
		}
		onChange.run();
	}

	private void failed(String message)
	{
		log.debug("Profile request failed: {}", message);
		synchronized (this)
		{
			error = message;
		}
		onChange.run();
	}

	/** Writes a profile's choices and look into the settings. */
	private void apply(Profiles.Profile p)
	{
		Preset preset = Preset.byId(getPresets(), p.preset);
		ProfileChoices.Values v = ProfileChoices.fromChoices(p.choices, preset);
		applying(() ->
		{
			setConfig("riskLevel", v.risk);
			setConfig("checkIn", v.checkIn);
			setConfig("itemScope", v.itemScope);
			setConfig("categories", v.categories);
			setConfig("coinCap", v.coinCap);
			setConfig("maxItemPercent", v.maxItemPercent);
			setConfig("minItemPrice", v.minItemPrice);
			setConfig("maxItemPrice", v.maxItemPrice);
			setConfig("minProfit", v.minProfit);
			for (java.util.Map.Entry<String, Object> e : LookSettings.toSettings(p.look).entrySet())
			{
				setConfig(e.getKey(), e.getValue());
			}
			setConfig("setupPreset", p.preset == null ? "" : p.preset);
			setConfig("activeProfileId", p.id);
			setConfig("setupDone", true);
		});
	}

	private void applying(Runnable writes)
	{
		synchronized (this)
		{
			applying = true;
		}
		try
		{
			writes.run();
		}
		finally
		{
			synchronized (this)
			{
				applying = false;
				saveDueMillis = 0;
			}
		}
	}

	private ProfileChoices.Values current()
	{
		ProfileChoices.Values v = new ProfileChoices.Values();
		v.risk = config.riskLevel();
		v.checkIn = config.checkIn();
		v.itemScope = config.itemScope();
		v.categories = config.categories() == null || config.categories().isEmpty()
			? EnumSet.noneOf(GpGeniusConfig.Category.class)
			: EnumSet.copyOf(config.categories());
		v.coinCap = config.coinCap();
		v.maxItemPercent = config.maxItemPercent();
		v.minItemPrice = config.minItemPrice();
		v.maxItemPrice = config.maxItemPrice();
		v.minProfit = config.minProfit();
		return v;
	}

	private Profiles.Look look()
	{
		return LookSettings.from(config);
	}

	/** Switches to the next saved profile, for the hotkey. */
	public void switchToNext()
	{
		String next = LookSettings.nextProfileId(getProfiles(), activeId());
		if (next != null)
		{
			switchTo(next);
		}
	}

	private String presetId()
	{
		Preset p = Preset.byId(getPresets(), config.setupPreset());
		return p == null ? Preset.BUILT_IN.get(0).id : p.id;
	}

	private synchronized Profiles.Profile find(String id)
	{
		for (Profiles.Profile p : profiles)
		{
			if (p.id.equals(id))
			{
				return p;
			}
		}
		return null;
	}

	/** "AFK", or "AFK 2" if that's taken. */
	synchronized String uniqueName(String base)
	{
		List<String> taken = new ArrayList<>();
		for (Profiles.Profile p : profiles)
		{
			taken.add(p.name.toLowerCase(java.util.Locale.ROOT));
		}
		String name = base;
		for (int n = 2; taken.contains(name.toLowerCase(java.util.Locale.ROOT)); n++)
		{
			name = base + " " + n;
		}
		return name;
	}

	private void setConfig(String key, Object value)
	{
		configManager.setConfiguration(GpGeniusConfig.GROUP, key, value);
	}

}
