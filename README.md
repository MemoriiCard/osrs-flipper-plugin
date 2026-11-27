# GP Genius

A RuneLite plugin that suggests Grand Exchange flips tuned to how you play, walks you through each
offer, and tracks your profit.

## Features

- **Suggestions for your play style.** Pick a risk level (low, medium, high) and how often you check
  your offers, from every 5 minutes to once a day. Short check-ins get fast, tight flips; long ones get
  patient offers with wider margins. Block items, set a minimum profit, keep slots free, sell-only mode,
  free-to-play items only.
- **One next step at a time.** Buy, sell, abort a stale offer, or wait. The plugin highlights the slot,
  button or item to click next, and a hotkey (E by default) fills the suggested price or quantity into
  the GE prompt. You still press every key and click every button.
- **One-click start.** Pick Active, AFK, Overnight, Low budget or High volume and the first suggestion
  arrives within seconds. Start again any time from the Presets button at the top of the panel.
- **Saved profiles.** Each preset you pick is saved as a profile on your account, with its own risk,
  check-in time, coin cap, per-item cap, filters, theme, compact layout, sound choice, highlight look and accessibility modes (not hotkeys). Switch, add,
  rename or delete profiles from the bar at the top of the panel; edits to those settings save to the
  active profile. Profiles follow you to any computer you sign in on.
- **Your limits.** Under *Limits (advanced)*: coins to flip with (the rest of your stack is never
  counted), the most one item may take, an item price range, categories (runes, potions, food, ammo,
  skilling supplies, gear) and favourite items to prefer. Pick any items, free-to-play only, or members
  only.
- **Welcome-back plan.** When you log in or come back to the GE after a check-in window away with
  several free slots, the panel lists every step for them (sells first, then reprices, then buys) with
  the coins used and the expected profit. You still do each step by hand; the highlight follows the
  first one.
- **Check-in reminders.** At most one notification per check-in window, summing up what happened:
  "2 sells done, +184k. 1 offer to reprice. Next: Buy 30 x Abyssal whip." Choose what's included, set
  quiet hours, and use RuneLite's own notification options (sound, flash, only when unfocused).
- **Import from Flipping Utilities.** "Import history" under your flips copies the trades that plugin
  saved for the logged-in character, so your profit and history don't start at zero. Imported flips
  are labelled and never count towards how accurate suggestions are.
- **Dump alerts (premium).** A notification when an item is sold far below its usual price, with the
  buy price, quantity and expected profit. "Use" makes it the current suggestion.
- **Offer upkeep.** When the server no longer expects an offer to fill at its price in reasonable time,
  the suggestion says the better use of its slot: **RAISE** a buy (abort it, then the plugin walks you
  through rebuying at the new price), **SELL PART** (stop buying and sell what it bought), **LOWER** a
  sell to what buyers pay, or **CLOSE** a sell that would only fill at a loss and keep the items until
  the price recovers. The slot is outlined in the colour of what comes next with a "+", "-", "$" or "X"
  badge. Turn it off with "Offer upkeep tips"; the plain stale-offer abort still applies.
- **Themes.** Grand Exchange, RuneLite, Matrix, Amethyst, Neon Cyan and Dragon. Every theme is dark
  and contrast-checked, and the in-game highlight follows the theme.
- **Highlights you can't miss.** Under *Highlight look*: red-green safe, blue-yellow safe, high
  visibility or white-only colours; outline thickness, size and opacity; fill strength; a gentle pulse
  (off for motion sensitivity); a label that says exactly what to click or type ("Set price to
  1,250"); an outline on the price or quantity box while it's open; and an optional spotlight that
  dims everything else. Outbid slots are also dashed with a "!" and aborts get a cross, so colour is
  never the only signal.
- **Accessibility.** Larger panel text (Large or Largest), a black-and-white high-contrast mode, and
  optional sound cues for a new next step, a finished offer or an outbid offer. Cues are off by
  default and rate limited.
- **Optional hotkeys.** Highlights on/off, refresh suggestion, next profile, simple/advanced view and
  compact panel. All unset by default and kept per computer.
- **Milestones and streaks.** The goal card shows your top profit badge (1M up to 10B), the next one and
  your streak of profitable days, and a notification says when you reach a new one ("Milestone
  reached"). Share badges and choose a Discord rank role from the website dashboard.
- **Price chart.** Day, week and month charts of the item you're setting up or the suggested item.
- **Your history with the item.** One line under the suggestion with your own record for that item, such
  as "You: 12 flips, +3.4M, 83% wins, 85,000 gp each, avg 41 min". The website dashboard has the full
  page (click any item). Turn it off with "My history with the item".
- **Offer slot colours.** Active offers are outlined green while their price still fills and red,
  dashed and marked "!" once the market has moved past it (colours follow *Highlight colours*).
- **Market warnings.** When an item's prices are unstable, or a game update or event is due, the
  server pauses or lowers suggestions for it and the card shows a short warning, in simple and
  advanced mode.
- **Mis-click guard.** If a price looks like a typo (far from the market or the suggestion), Confirm
  moves off left-click and a warning explains why. It stays on the right-click menu.
- **Goal tracker.** Set a target such as "reach a 100M stack by Friday" or "make 50M profit by
  Sunday", for one character or all of them. The panel shows progress, time left, whether you're ahead
  or behind, and about how much an hour is still needed. With *Adapt* on, the server picks the preset and
  check-in rhythm that suit your pace while the goal runs, and check-in reminders follow it. You still
  set up and confirm every offer yourself.
- **Profit at a glance.** Session, 1 hour, 12 hours, day, week, month and all time, for this
  character, any other character or all of them combined. *Reset session* starts a new session for the
  characters shown and leaves every other total alone. The same table is on the website dashboard.
- **Suggested minimum cash stack.** A round figure such as 200M: the smallest stack we suggest
  flipping with to hold on to the gains you've reached. It's worked out on the server and only appears
  once your stack is known.
- **Logging off? Overnight mode.** Pick how many hours you'll be away and *Plan overnight*: the
  suggestion and the GE slot plan switch to slower, safer offers for every free slot, sized for the time
  you're gone. You set up each offer yourself before logging off. It turns itself off 90 minutes after
  you come back.
- **Trade dashboard and CSV export.** A pop-out window with your totals, six charts and a sortable table
  of every trade, and *Export CSV* (open and close time, prices, tax, gross and net profit) to a file
  you choose. Only your own trades are shown.
- **Profit tracking.** Every GE fill is recorded, buys and sells are paired oldest first, and profit is
  worked out after GE tax. Session and all-time profit and recent flips show in the side panel; full
  history, charts and CSV export are on the website.
- **Simple or advanced panel.** Simple shows the next step and your profit. Advanced adds each step's detail,
  Good/Bad ratings and every flip's prices. *Compact panel* hides the chart and flip list.
- **Your alts don't compete.** Your game accounts are steered to different items unless you allow it.
- **Watchlist.** List items with a buy and a sell target under *Watchlist* in the settings, for example
  `Abyssal whip, 1.4m, 1.6m`. The panel shows each item's latest prices and marks a target once it's
  reached, with one notification per crossing. Free watches 3 items, Premium up to 100.
- **Slot roles (opt-in).** Keep your last few GE slots for long, patient flips with their own check-in
  time and risk, while the others flip on your normal settings. Suggestions for those slots say
  *Long-hold slot*.
- **Sets & decanting (Premium, opt-in).** Armour sets to pack or unpack and potions to decant at the GE,
  shown only when every item trades steadily, the margin has held for hours and the profit after tax
  clears your own minimum. You still place each buy, exchange at the NPC and place each sell yourself.
- **Weekly recap.** Last week's profit, flips, tax, best item and goal progress at the top of the panel
  every Monday (hide it until next week with *Hide*). The website dashboard has it too, with a share
  link, and you can opt in there to get it as a Discord DM.

## Free, Premium and Pro

Plans differ in how fresh prices are, how often suggestions refresh and features. No plan is suggested a flip before another.

- **Free**: suggestions from the 75 most traded free-to-play items worth up to 5M, ranked by margin and volume, refreshed every few minutes, plus tracking, highlights and hotkeys.
- **Premium**: every item on prices about a minute old, spike-proof pricing, slot planning, market warnings, overnight mode and dump alerts. Members items are only suggested on members worlds.
- **Pro**: everything in Premium on near-real-time prices, faster re-quotes of offers that stopped filling, and a **Why?** button with a short explanation from the AI market analyst.

The side panel shows your plan and how often it refreshes; the plugin follows the refresh time the server sends. New logins get a 4-hour premium trial on one game account once a Discord account at least 30 days old is linked on the website. Plans are managed on the website.

## Privacy

Signing in is optional, but suggestions need it. Before anything is sent, the side panel shows what
is shared and the player clicks **Agree & sign in**; until then flips are tracked on this computer
only. When signed in, the plugin sends your GE offers, the
trades it tracked, the coins in your inventory, your character name and RuneLite's account ID to the
GP Genius server, which stores the account ID only in a scrambled form. It never sees your Jagex
login or anything about other players. Turn off "Sync flips to website" to keep trades on your
computer only. Tracked trades are also saved locally in `.runelite/gp-genius/`.

## Rules this plugin follows

It never clicks, types, or acts in game for you. Highlights and colours only draw; the hotkey fills a
value into a prompt you opened, and you confirm it. The welcome-back plan is a list to read, and
reminders only tell you to come back; nothing is queued or sent for you. This keeps to Jagex's third-party client guidelines
and RuneLite's Plugin Hub rules.

Suggestions are only shown for the character that is logged in. Each request remembers the account and
character name it was sent for, and an answer that arrives after you log out, switch character or change
name is dropped (`CharacterGuard`).

## Development

Requires Java 11.

```
./gradlew build                 # compile and run unit tests
```

Run `GpGeniusPluginTest` from IntelliJ with VM option `-ea` to start RuneLite with the plugin loaded.
For a local backend, set **Connection > Server URL** to `http://localhost:8000`.
