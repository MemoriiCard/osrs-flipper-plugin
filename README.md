# OSRS Flipper

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
- **Dump alerts (premium).** A notification when an item is sold far below its usual price, with the
  buy price, quantity and expected profit. "Use" makes it the current suggestion.
- **Offer slot colours.** Active offers are outlined green while their price still fills and red once
  the market has moved past it.
- **Mis-click guard.** If a price looks like a typo (far from the market or the suggestion), Confirm
  moves off left-click and a warning explains why. It stays on the right-click menu.
- **Profit tracking.** Every GE fill is recorded, buys and sells are paired oldest first, and profit is
  worked out after GE tax. Session and all-time profit and recent flips show in the side panel; full
  history, charts and CSV export are on the website.
- **Simple or advanced panel.** Simple shows the next step and your profit. Advanced adds the reasoning,
  Good/Bad ratings, "Why this item?" and every flip's prices.
- **Your alts don't compete.** Your game accounts are steered to different items unless you allow it.

## Free and premium

Free accounts get suggestions from a pool of popular items, plus tracking, highlights and hotkeys.
Premium unlocks every item and dump alerts. Plans are managed on the website.

## Privacy

Signing in is optional, but suggestions need it. When signed in, the plugin sends your GE offers, the
trades it tracked, the coins in your inventory, your character name and RuneLite's account ID to the
OSRS Flipper server, which stores the account ID only in a scrambled form. It never sees your Jagex
login or anything about other players. Turn off "Sync flips to website" to keep trades on your
computer only. Tracked trades are also saved locally in `.runelite/osrs-flipper/`.

## Rules this plugin follows

It never clicks, types, or acts in game for you. Highlights and colours only draw; the hotkey fills a
value into a prompt you opened, and you confirm it. This keeps to Jagex's third-party client guidelines
and RuneLite's Plugin Hub rules.

## Development

Requires Java 11.

```
./gradlew build                 # compile and run unit tests
```

Run `OsrsFlipperPluginTest` from IntelliJ with VM option `-ea` to start RuneLite with the plugin loaded.
For a local backend, set **Connection > Server URL** to `http://localhost:8000`.
