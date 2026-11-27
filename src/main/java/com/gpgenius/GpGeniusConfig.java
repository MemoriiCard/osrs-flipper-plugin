package com.gpgenius;

import com.gpgenius.auth.DataConsent;
import com.gpgenius.auth.ServerUrl;

import java.awt.event.KeyEvent;
import java.util.EnumSet;
import java.util.Set;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import com.gpgenius.notify.SoundCue;
import com.gpgenius.ui.HighlightPalette;
import com.gpgenius.ui.TextScale;
import com.gpgenius.ui.Theme;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Keybind;
import net.runelite.client.config.Notification;
import net.runelite.client.config.Range;
import net.runelite.client.config.Units;

@ConfigGroup(GpGeniusConfig.GROUP)
public interface GpGeniusConfig extends Config
{
	String GROUP = "gpgenius";

	enum RiskLevel
	{
		LOW,
		MEDIUM,
		HIGH
	}

	enum ViewMode
	{
		SIMPLE,
		ADVANCED
	}

	/** What to do with an item that would now sell for less than it cost. */
	enum LossHandling
	{
		AUTO("Let GP Genius pick"),
		HOLD("Hold until it recovers"),
		RELIST("Relist at break-even"),
		CUT("Sell now and take the loss");

		private final String label;

		LossHandling(String label)
		{
			this.label = label;
		}

		@Override
		public String toString()
		{
			return label;
		}
	}

	/** Which items to suggest by membership. */
	enum ItemScope
	{
		ANY("Any items"),
		F2P("Free-to-play only"),
		MEMBERS("Members only");

		private final String label;

		ItemScope(String label)
		{
			this.label = label;
		}

		@Override
		public String toString()
		{
			return label;
		}
	}

	/** Item groups the player can limit suggestions to. Names match the server's categories. */
	enum Category
	{
		RUNES("Runes"),
		POTIONS("Potions"),
		FOOD("Food"),
		AMMO("Ammo"),
		SKILLING("Skilling supplies"),
		GEAR("Gear"),
		OTHER("Everything else");

		private final String label;

		Category(String label)
		{
			this.label = label;
		}

		/** The server's name for the category. */
		public String key()
		{
			return name().toLowerCase(java.util.Locale.ROOT);
		}

		@Override
		public String toString()
		{
			return label;
		}
	}

	/** How often the player comes back to their offers. */
	enum CheckIn
	{
		MINUTES_5("Every 5 minutes", 5),
		MINUTES_15("Every 15 minutes", 15),
		MINUTES_30("Every 30 minutes", 30),
		HOUR_1("Every hour", 60),
		HOURS_2("Every 2 hours", 120),
		HOURS_4("Every 4 hours", 240),
		HOURS_8("Every 8 hours", 480),
		HOURS_12("Every 12 hours", 720),
		DAY_1("Once a day", 1440);

		private final String label;
		public final int minutes;

		CheckIn(String label, int minutes)
		{
			this.label = label;
			this.minutes = minutes;
		}

		@Override
		public String toString()
		{
			return label;
		}
	}

	@ConfigSection(
		name = "Suggestions",
		description = "What kind of flips to suggest",
		position = 0
	)
	String suggestionSection = "suggestions";

	@ConfigSection(
		name = "Offer setup",
		description = "Highlights and hotkeys on the Grand Exchange",
		position = 3
	)
	String offerSection = "offers";

	@ConfigSection(
		name = "Tracking",
		description = "Flip tracking and the side panel",
		position = 7
	)
	String trackingSection = "tracking";

	@ConfigSection(
		name = "Highlight look",
		description = "Colour, size and strength of the next-step highlight. Saved with your profile.",
		position = 4
	)
	String highlightSection = "highlightLook";

	@ConfigSection(
		name = "Accessibility",
		description = "Bigger text, high contrast and sound cues. Saved with your profile.",
		position = 5
	)
	String accessibilitySection = "accessibility";

	@ConfigSection(
		name = "Hotkeys",
		description = "Optional keys for panel actions. All are off until you set them. Kept on this computer only.",
		position = 8,
		closedByDefault = true
	)
	String hotkeySection = "hotkeys";

	@ConfigSection(
		name = "Dump alerts",
		description = "Items just sold far below their usual price (premium)",
		position = 6
	)
	String alertSection = "alerts";

	@ConfigSection(
		name = "Limits (advanced)",
		description = "Caps on coins and items. Leave these at 0 or empty for no limit.",
		position = 1,
		closedByDefault = true
	)
	String limitsSection = "limits";

	@ConfigSection(
		name = "Check-in reminders",
		description = "One notification per check-in when your offers need you",
		position = 2
	)
	String reminderSection = "reminders";

	@ConfigSection(
		name = "Connection",
		description = "Where the plugin gets suggestions from",
		position = 9,
		closedByDefault = true
	)
	String connectionSection = "connection";

	@ConfigItem(
		keyName = DataConsent.CONFIG_KEY,
		name = "",
		description = "",
		hidden = true
	)
	default String dataConsent()
	{
		return "";
	}

	@ConfigItem(
		keyName = "theme",
		name = "Theme",
		description = "Colours for the side panel, price chart and in-game highlights. Every theme is dark.",
		position = -2
	)
	default Theme theme()
	{
		return Theme.GRAND_EXCHANGE;
	}

	@ConfigItem(
		keyName = "compactLayout",
		name = "Compact panel",
		description = "Only the next step and your profit: hides the price chart and the flip list. Saved with your profile.",
		position = -1
	)
	default boolean compactLayout()
	{
		return false;
	}

	@ConfigItem(
		keyName = "sounds",
		name = "Notification sounds",
		description = "Turn off to keep reminders and alerts silent. Saved with your profile.",
		position = -1
	)
	default boolean sounds()
	{
		return true;
	}

	@ConfigItem(
		keyName = "viewMode",
		name = "Panel view",
		description = "Simple shows the next step and your profit. Advanced adds each step's detail, ratings and every recent flip.",
		position = -1
	)
	default ViewMode viewMode()
	{
		return ViewMode.SIMPLE;
	}

	@ConfigItem(
		keyName = "suggestionsEnabled",
		name = "Get suggestions",
		description = "Send your GE offers, coins and flip settings to the GP Genius server to get flip suggestions.",
		section = suggestionSection,
		position = 0
	)
	default boolean suggestionsEnabled()
	{
		return true;
	}

	@ConfigItem(
		keyName = "riskLevel",
		name = "Risk level",
		description = "Low sticks to steady, high-volume items. High allows jumpier items with bigger margins.",
		section = suggestionSection,
		position = 1
	)
	default RiskLevel riskLevel()
	{
		return RiskLevel.MEDIUM;
	}

	@ConfigItem(
		keyName = "slotManager",
		name = "Slot manager",
		description = "Show a plan for every GE slot: which offers to leave, collect, relist or cut, and what to sell and buy in the free ones. You still do every step yourself.",
		section = suggestionSection,
		position = 9
	)
	default boolean slotManager()
	{
		return true;
	}

	@ConfigItem(
		keyName = "checkIn",
		name = "I check my offers",
		description = "How often you come back to adjust offers. Often suggests fast flips; rarely suggests patient offers with wider margins and buy limits that last.",
		section = suggestionSection,
		position = 2
	)
	default CheckIn checkIn()
	{
		return CheckIn.MINUTES_30;
	}

	@Range(min = 0, max = 7)
	@ConfigItem(
		keyName = "reservedSlots",
		name = "Reserved GE slots",
		description = "Slots to keep free for your own trades.",
		section = suggestionSection,
		position = 3
	)
	default int reservedSlots()
	{
		return 0;
	}

	@ConfigItem(
		keyName = "sellOnly",
		name = "Sell-only mode",
		description = "Only suggest selling what you already bought.",
		section = suggestionSection,
		position = 4
	)
	default boolean sellOnly()
	{
		return false;
	}

	@ConfigItem(
		keyName = "itemScope",
		name = "Items to suggest",
		description = "Any items, free-to-play items only (even on members worlds), or members items only.",
		section = suggestionSection,
		position = 5
	)
	default ItemScope itemScope()
	{
		return ItemScope.ANY;
	}

	/** Replaced by {@link #itemScope()}; read once on start-up to carry an old setting over. */
	@ConfigItem(
		keyName = "f2pOnly",
		name = "",
		description = "",
		hidden = true
	)
	default boolean f2pOnly()
	{
		return false;
	}

	@ConfigItem(
		keyName = "minProfit",
		name = "Minimum profit per flip",
		description = "Skip suggestions expected to make less than this many coins.",
		section = suggestionSection,
		position = 6
	)
	default int minProfit()
	{
		return 0;
	}

	@ConfigItem(
		keyName = "blockedItems",
		name = "Blocked items",
		description = "Item IDs never to suggest, separated by commas. The Block button on a suggestion adds to this list; delete an ID to allow that item again.",
		section = suggestionSection,
		position = 7
	)
	default String blockedItems()
	{
		return "";
	}

	@ConfigItem(
		keyName = "allowSharedItems",
		name = "Same items on my alts",
		description = "Let your other game accounts be suggested the same item at the same time. Off spreads them across different items so they don't outbid each other.",
		section = suggestionSection,
		position = 8
	)
	default boolean allowSharedItems()
	{
		return false;
	}

	@ConfigItem(
		keyName = "lossHandling",
		name = "When a flip goes bad",
		description = "What to suggest when an item would sell for less than you paid after tax. Hold and relist still switch to selling once the item has waited about twice your check-in time.",
		section = suggestionSection,
		position = 9
	)
	default LossHandling lossHandling()
	{
		return LossHandling.AUTO;
	}

	@ConfigItem(
		keyName = "coinCap",
		name = "Coins to flip with",
		description = "The most coins suggestions may use, counting open buy offers. 0 uses all the coins in your inventory.",
		section = limitsSection,
		position = 0
	)
	default int coinCap()
	{
		return 0;
	}

	@Range(min = 0, max = 100)
	@ConfigItem(
		keyName = "maxItemPercent",
		name = "Max per item (%)",
		description = "The most of your flipping coins one item may take. 0 means no limit beyond your risk level.",
		section = limitsSection,
		position = 1
	)
	default int maxItemPercent()
	{
		return 0;
	}

	@ConfigItem(
		keyName = "minItemPrice",
		name = "Item price from",
		description = "Only suggest items that cost at least this much each. 0 means no minimum.",
		section = limitsSection,
		position = 2
	)
	default int minItemPrice()
	{
		return 0;
	}

	@ConfigItem(
		keyName = "maxItemPrice",
		name = "Item price up to",
		description = "Only suggest items that cost at most this much each. 0 means no maximum.",
		section = limitsSection,
		position = 3
	)
	default int maxItemPrice()
	{
		return 0;
	}

	@ConfigItem(
		keyName = "categories",
		name = "Only these categories",
		description = "Limit suggestions to these kinds of item. Select none for every item. Categories are worked out from item names, so a few items may land in the wrong one.",
		section = limitsSection,
		position = 4
	)
	default Set<Category> categories()
	{
		return EnumSet.noneOf(Category.class);
	}

	@ConfigItem(
		keyName = "favouriteItems",
		name = "Favourite items",
		description = "Item IDs to prefer when they're a good flip, separated by commas. The Favourite button on a suggestion adds to this list.",
		section = limitsSection,
		position = 5
	)
	default String favouriteItems()
	{
		return "";
	}

	@ConfigItem(
		keyName = "checkInNotification",
		name = "Check-in reminder",
		description = "At most one notification per check-in window, summing up what happened: offers done, profit, offers to reprice and whether a next step is ready.",
		section = reminderSection,
		position = 0
	)
	default Notification checkInNotification()
	{
		return Notification.ON;
	}

	@ConfigItem(
		keyName = "remindCompleted",
		name = "Completed offers",
		description = "Include buys and sells that finished.",
		section = reminderSection,
		position = 1
	)
	default boolean remindCompleted()
	{
		return true;
	}

	@ConfigItem(
		keyName = "remindReprice",
		name = "Offers to reprice",
		description = "Include offers flagged as stale or no longer profitable.",
		section = reminderSection,
		position = 2
	)
	default boolean remindReprice()
	{
		return true;
	}

	@ConfigItem(
		keyName = "remindNextStep",
		name = "Next step ready",
		description = "Include a new buy or sell suggestion once a slot frees up.",
		section = reminderSection,
		position = 3
	)
	default boolean remindNextStep()
	{
		return true;
	}

	@ConfigItem(
		keyName = "quietHours",
		name = "Quiet hours",
		description = "No reminders between the hours below (your computer's clock). Anything that happened is sent once they end.",
		section = reminderSection,
		position = 4
	)
	default boolean quietHours()
	{
		return false;
	}

	@Range(min = 0, max = 23)
	@ConfigItem(
		keyName = "quietStart",
		name = "Quiet from (hour)",
		description = "Hour of the day quiet hours start, 0 to 23.",
		section = reminderSection,
		position = 5
	)
	default int quietStart()
	{
		return 23;
	}

	@Range(min = 0, max = 23)
	@ConfigItem(
		keyName = "quietEnd",
		name = "Quiet until (hour)",
		description = "Hour of the day quiet hours end, 0 to 23.",
		section = reminderSection,
		position = 6
	)
	default int quietEnd()
	{
		return 8;
	}

	@ConfigItem(
		keyName = "setupDone",
		name = "",
		description = "",
		hidden = true
	)
	default boolean setupDone()
	{
		return false;
	}

	@ConfigItem(
		keyName = "setupPreset",
		name = "",
		description = "",
		hidden = true
	)
	default String setupPreset()
	{
		return "";
	}

	/** The saved profile in use, by its server id. Empty means none. */
	@ConfigItem(
		keyName = "activeProfileId",
		name = "",
		description = "",
		hidden = true
	)
	default String activeProfileId()
	{
		return "";
	}

	@ConfigItem(
		keyName = "apiUrl",
		name = "Server URL",
		description = "GP Genius server address. Must start with https://. Leave as is unless told otherwise.",
		section = connectionSection,
		position = 0
	)
	default String apiUrl()
	{
		return ServerUrl.DEFAULT;
	}

	@ConfigItem(
		keyName = "highlightOffers",
		name = "Highlight next step",
		description = "Highlight the GE slot, button or item to click next for the current suggestion.",
		section = offerSection,
		position = 0
	)
	default boolean highlightOffers()
	{
		return true;
	}

	@ConfigItem(
		keyName = "quickSetKeybind",
		name = "Set price/quantity key",
		description = "While the GE price or quantity box is open, press this to fill in the suggested value.",
		section = offerSection,
		position = 1
	)
	default Keybind quickSetKeybind()
	{
		return new Keybind(KeyEvent.VK_E, 0);
	}

	@ConfigItem(
		keyName = "skipKeybind",
		name = "Skip suggestion key",
		description = "Skip the current suggestion and get another.",
		section = offerSection,
		position = 2
	)
	default Keybind skipKeybind()
	{
		return Keybind.NOT_SET;
	}

	@ConfigItem(
		keyName = "slotColors",
		name = "Colour offer slots",
		description = "Outline active GE offers green while their price will fill and red once the market has moved past it.",
		section = offerSection,
		position = 3
	)
	default boolean slotColors()
	{
		return true;
	}

	@ConfigItem(
		keyName = "confirmGuard",
		name = "Mis-click guard",
		description = "When an offer's price looks like a typo, move Confirm off left-click. It stays in the right-click menu.",
		section = offerSection,
		position = 4
	)
	default boolean confirmGuard()
	{
		return true;
	}

	@Range(min = 1, max = 100)
	@ConfigItem(
		keyName = "confirmGuardPercent",
		name = "Guard when off by (%)",
		description = "How far from the market or the suggested price an offer can be before the guard steps in.",
		section = offerSection,
		position = 5
	)
	default int confirmGuardPercent()
	{
		return 10;
	}

	@ConfigItem(
		keyName = "dumpAlerts",
		name = "Show dump alerts",
		description = "Check once a minute for items just sold far below their usual price. Premium accounts only.",
		section = alertSection,
		position = 0
	)
	default boolean dumpAlerts()
	{
		return true;
	}

	@ConfigItem(
		keyName = "dumpNotification",
		name = "Notify on dump alert",
		description = "Notification when a new dump alert arrives.",
		section = alertSection,
		position = 1
	)
	default Notification dumpNotification()
	{
		return Notification.ON;
	}

	@ConfigItem(
		keyName = "trackTrades",
		name = "Track GE trades",
		description = "Record your Grand Exchange trades on this computer to work out flip profit.",
		section = trackingSection,
		position = 0
	)
	default boolean trackTrades()
	{
		return true;
	}

	@ConfigItem(
		keyName = "syncTrades",
		name = "Sync flips to website",
		description = "Upload your tracked GE trades to your GP Genius account so you can see history and charts on the website. Needs sign-in.",
		section = trackingSection,
		position = 1
	)
	default boolean syncTrades()
	{
		return true;
	}

	@ConfigItem(
		keyName = "sendErrorReports",
		name = "Send error reports",
		description = "When something goes wrong (a request fails, a file can't be saved), send a short description to GP Genius so support can see it in your ticket. Never includes your email, IP address, file-path user name or any code. Needs sign-in.",
		section = trackingSection,
		position = 1
	)
	default boolean sendErrorReports()
	{
		return true;
	}

	@ConfigItem(
		keyName = "recentFlipCount",
		name = "Recent flips shown",
		description = "How many recent flips the side panel lists in the advanced view.",
		section = trackingSection,
		position = 2
	)
	default int recentFlipCount()
	{
		return 25;
	}

	@ConfigItem(
		keyName = "priceChart",
		name = "Price chart",
		description = "Chart the price of the item you're setting up an offer for, or the suggested item.",
		section = trackingSection,
		position = 3
	)
	default boolean priceChart()
	{
		return true;
	}

	@ConfigItem(
		keyName = "highlightPalette",
		name = "Highlight colours",
		description = "Match theme, or a palette that stays clear with red-green or blue-yellow colour blindness. Outbid slots are also dashed and aborts get a cross, so colour is never the only signal.",
		section = highlightSection,
		position = 0
	)
	default HighlightPalette highlightPalette()
	{
		return HighlightPalette.THEME;
	}

	@Range(min = 1, max = 8)
	@ConfigItem(
		keyName = "highlightThickness",
		name = "Outline thickness",
		description = "Width of the highlight outline in pixels.",
		section = highlightSection,
		position = 1
	)
	default int highlightThickness()
	{
		return 3;
	}

	@Range(min = 0, max = 12)
	@ConfigItem(
		keyName = "highlightPadding",
		name = "Highlight size",
		description = "Pixels the highlight reaches past the thing to click, on every side. Bigger is easier to spot.",
		section = highlightSection,
		position = 2
	)
	default int highlightPadding()
	{
		return 2;
	}

	@Range(min = 10, max = 100)
	@Units(Units.PERCENT)
	@ConfigItem(
		keyName = "highlightOpacity",
		name = "Outline opacity",
		description = "How solid the outline is.",
		section = highlightSection,
		position = 3
	)
	default int highlightOpacity()
	{
		return 85;
	}

	@Range(min = 0, max = 100)
	@Units(Units.PERCENT)
	@ConfigItem(
		keyName = "highlightFill",
		name = "Fill opacity",
		description = "How strongly the inside is tinted. 0 draws the outline only.",
		section = highlightSection,
		position = 4
	)
	default int highlightFill()
	{
		return 20;
	}

	@ConfigItem(
		keyName = "highlightPulse",
		name = "Gentle pulse",
		description = "Slowly pulse the highlight so it catches the eye. Turn off if motion bothers you.",
		section = highlightSection,
		position = 5
	)
	default boolean highlightPulse()
	{
		return true;
	}

	@ConfigItem(
		keyName = "highlightLabel",
		name = "Say what to click",
		description = "Show a short label by the highlight, like \"Set price to 1,250\" or \"Click Confirm\".",
		section = highlightSection,
		position = 6
	)
	default boolean highlightLabel()
	{
		return true;
	}

	@ConfigItem(
		keyName = "highlightPrompt",
		name = "Highlight the typing box",
		description = "When the GE asks for a price or quantity, outline the box and show the number to type.",
		section = highlightSection,
		position = 7
	)
	default boolean highlightPrompt()
	{
		return true;
	}

	@ConfigItem(
		keyName = "highlightSpotlight",
		name = "Spotlight",
		description = "Dim the rest of the game screen while a next step is highlighted, so it can't be missed.",
		section = highlightSection,
		position = 8
	)
	default boolean highlightSpotlight()
	{
		return false;
	}

	@ConfigItem(
		keyName = "textSize",
		name = "Panel text size",
		description = "Bigger text in the side panel.",
		section = accessibilitySection,
		position = 0
	)
	default TextScale.Size textSize()
	{
		return TextScale.Size.NORMAL;
	}

	@ConfigItem(
		keyName = "highContrast",
		name = "High contrast",
		description = "Black and white panel with a yellow accent, in place of the theme. Highlights use it too when set to Match theme.",
		section = accessibilitySection,
		position = 1
	)
	default boolean highContrast()
	{
		return false;
	}

	@ConfigItem(
		keyName = "soundCues",
		name = "Sound cues",
		description = "Short game sounds when a new next step is ready, an offer finishes, or an offer gets outbid. None are on by default. Notification sounds must be on.",
		section = accessibilitySection,
		position = 2
	)
	default Set<SoundCue> soundCues()
	{
		return EnumSet.noneOf(SoundCue.class);
	}

	@Range(min = 1, max = 100)
	@Units(Units.PERCENT)
	@ConfigItem(
		keyName = "soundCueVolume",
		name = "Sound cue volume",
		description = "How loud the sound cues are.",
		section = accessibilitySection,
		position = 3
	)
	default int soundCueVolume()
	{
		return 60;
	}

	@ConfigItem(
		keyName = "toggleHighlightKeybind",
		name = "Highlights on/off",
		description = "Turn the next-step highlight on or off. Use a key with Ctrl, Alt or Shift so chat typing can't trigger it.",
		section = hotkeySection,
		position = 0
	)
	default Keybind toggleHighlightKeybind()
	{
		return Keybind.NOT_SET;
	}

	@ConfigItem(
		keyName = "refreshKeybind",
		name = "Refresh suggestion",
		description = "Ask for a fresh suggestion now.",
		section = hotkeySection,
		position = 1
	)
	default Keybind refreshKeybind()
	{
		return Keybind.NOT_SET;
	}

	@ConfigItem(
		keyName = "nextProfileKeybind",
		name = "Next profile",
		description = "Switch to your next saved profile.",
		section = hotkeySection,
		position = 2
	)
	default Keybind nextProfileKeybind()
	{
		return Keybind.NOT_SET;
	}

	@ConfigItem(
		keyName = "toggleViewKeybind",
		name = "Simple/advanced view",
		description = "Switch the panel between simple and advanced view.",
		section = hotkeySection,
		position = 3
	)
	default Keybind toggleViewKeybind()
	{
		return Keybind.NOT_SET;
	}

	@ConfigItem(
		keyName = "toggleCompactKeybind",
		name = "Compact panel on/off",
		description = "Turn the compact panel on or off.",
		section = hotkeySection,
		position = 4
	)
	default Keybind toggleCompactKeybind()
	{
		return Keybind.NOT_SET;
	}
}
