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
  check-in time, coin cap, per-item cap, filters, theme, compact layout and sound choice. Switch, add,
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
- **Stale offer repricing.** An offer that sits longer than your check-in window at a price the market
  has left gets flagged, with the price to rebuy or relist at.
- **Themes.** Grand Exchange, RuneLite, Matrix, Amethyst, Neon Cyan and Dragon. Every theme is dark
  and contrast-checked, and the in-game highlight follows the theme.
- **Price chart.** Day, week and month charts of the item you're setting up or the suggested item.
- **Offer slot colours.** Active offers are outlined green while their price still fills and red once
  the market has moved past it.
- **Market warnings.** When an item's prices are unstable, or a game update or event is due, the
  server pauses or lowers suggestions for it and the card shows a short warning, in simple and
  advanced mode.
- **Mis-click guard.** If a price looks like a typo (far from the market or the suggestion), Confirm
  moves off left-click and a warning explains why. It stays on the right-click menu.
- **Profit tracking.** Every GE fill is recorded, buys and sells are paired oldest first, and profit is
  worked out after GE tax. Session and all-time profit and recent flips show in the side panel; full
  history, charts and CSV export are on the website.
- **Simple or advanced panel.** Simple shows the next step and your profit. Advanced adds each step's detail,
  Good/Bad ratings and every flip's prices. *Compact panel* hides the chart and flip list.
- **Your alts don't compete.** Your game accounts are steered to different items unless you allow it.

## Free and premium

Free accounts get suggestions from a pool of popular items, plus tracking, highlights and hotkeys.
Premium unlocks every item and dump alerts. Plans are managed on the website.

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

## Development

Requires Java 11.

```
./gradlew build                 # compile and run unit tests
```

Run `GpGeniusPluginTest` from IntelliJ with VM option `-ea` to start RuneLite with the plugin loaded.
For a local backend, set **Connection > Server URL** to `http://localhost:8000`.
