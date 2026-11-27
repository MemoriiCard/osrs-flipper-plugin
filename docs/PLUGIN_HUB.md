# Submitting to the Plugin Hub

Checked against the Plugin Hub's rules as of October 2026. What's done, then what's left.

## Done in this repository

- Java 11, Gradle, `build=standard`, only the RuneLite client API (OkHttp and Gson come from the client,
  injected). No reflection, no extra dependencies, no native code.
- `runelite-plugin.properties` has a display name, description, tags, and a `warning` line, which the
  Hub requires for plugins that send data to a third-party server.
- `icon.png` is 48 x 72.
- No automation: highlights, colours and the guard only draw or reorder a menu entry. The hotkey writes
  a value into a GE prompt the player opened, on a key the player pressed, and the player confirms it.
- Highlight styles, labels and the spotlight only draw. The optional panel hotkeys only change plugin
  settings or ask the server for a fresh suggestion. Sound cues use `Client.playSoundEffect` and are off
  by default.
- The welcome-back plan is text in the side panel. The plugin highlights one step at a time, as for a
  single suggestion, and the player sets up, prices and confirms every offer. Nothing is queued.
- Check-in reminders go through RuneLite's own `Notifier` and only describe what happened.
- "Import history" reads Flipping Utilities' saved JSON for the logged-in character from
  `.runelite/flipping/`, only when the player presses the button, off the client thread. It never writes
  there.
- A data notice in the side panel that the player agrees to before anything is sent to the server.
- No data about other players; game accounts are identified by RuneLite's account hash only.
- Tokens are stored outside RuneLite's config (so they never sync to a RuneLite profile) and only sent
  over HTTPS.

## Testing before approval

See [TESTING.md](TESTING.md) for the runnable tester jar that CI builds on every run.

## Submit early

Plugin Hub reviews can take one to several weeks, so it's worth submitting as soon as the backend is
deployed and you've done one clean in-game flip, and carrying on friend testing with the tester jar
while it's in review. Each fix after that is a new commit hash in the same plugin-hub pull request.

## Left to do before submitting

1. **Deploy the backend** on its own domain with HTTPS (for example `api.<your domain>`), and set
   `FLIPPER_PUBLIC_BASE_URL`, `FLIPPER_SECRET_KEY` and the Stripe keys there.
2. **Point the plugin at it:** change `ServerUrl.LOCAL`'s fallback (the default of `apiUrl`) from
   `http://localhost:8000` to the HTTPS address. The Hub builds without `-PserverUrl`, so the tester
   builds' baked-in address doesn't apply there. The plugin refuses plain HTTP to anything but
   localhost, so the default has to be HTTPS.
3. **Test in-game** on a members and a free-to-play world: sign in, one full flip with highlights and
   the hotkey, a dump alert, the guard on a mistyped price, and logging out mid-offer.
4. **Merge the plugin PRs** into `main` in order, then take the commit hash of `main`.
5. **Open a pull request on [runelite/plugin-hub](https://github.com/runelite/plugin-hub)** adding
   `plugins/gp-genius` with:

   ```
   repository=https://github.com/MemoriiCard/osrs-flipper-plugin.git
   commit=<commit hash from step 4>
   ```

   Reviewers read the code, so the repository must be public at that point. The Hub builds the plugin
   itself; nothing is uploaded.
6. **Updates:** each new version is another plugin-hub pull request with a new commit hash.

## Submission pack

Everything the plugin-hub pull request and its reviewers ask for, ready to paste or check.

### Listing (from `runelite-plugin.properties`)

- **Name:** GP Genius
- **Description:** Make GP flipping on the Grand Exchange: smart flip suggestions that learn from real
  results, tuned to your risk and how often you play, with highlights, hotkeys, dump alerts and profit
  tracking.
- **Tags:** grand exchange, ge, flipping, flip, merching, profit, trading, money making, gp, suggestions
- **Warning** (shown before install): When signed in, this plugin sends your Grand Exchange offers and
  trades, the coins and GE-bought items in your inventory, your character name, feature usage, error
  reports and your IP address to the GP Genius server, which is not controlled or verified by the
  RuneLite Developers.

### What is sent, and where in the code

The warning above, the in-panel notice (`DataConsent.NOTICE`, version 2) and the website's privacy page
say the same thing. Nothing below is sent before the player presses **Agree & sign in**.

| Sent | When | Code |
| --- | --- | --- |
| GE offers (item, price, quantity, filled) | each suggestion request | `RequestBuilder`, `SuggestionController` |
| Coins in the inventory, GE-bought items still held | each suggestion request | `RequestBuilder.sellable` (only items the tracker saw bought) |
| Members world or not, suggestion settings | each suggestion request | `RequestBuilder` |
| Tracked trades | in batches, if *Sync flips to website* is on | `TradeSync` |
| Character name, RuneLite account hash | sign-in check-in (stored scrambled on the server) | `GpGeniusPlugin.hello` |
| Suggestion skipped/taken and ratings | when the player acts | `FlipperApiClient.feedback` |
| Panel feature counts (no content) | every so often while signed in | `UsageCounter` |
| Error reports, scrubbed | with the check-in, if *Send error reports* is on | `ErrorLog` |
| "Report a problem" text | only when the player sends it | `FlipperApiClient.reportProblem` |

Never sent: Jagex login, bank contents or PIN, chat, other players, files outside `.runelite/gp-genius/`
(and Flipping Utilities' folder, read only on *Import history*).

### Config defaults review

Checked for "nothing surprising out of the box":

- **Off by default:** spotlight, sound cues, slot roles, set and decant flips, quiet hours, every panel
  hotkey.
- **On by default, and why:** suggestions, highlights and slot colours (the plugin's purpose); trade
  tracking and sync (only after the consent notice); error reports (scrubbed, can be turned off);
  check-in, milestone, dump-alert and watchlist notifications and their sounds (they go through
  RuneLite's `Notifier`, so RuneLite's own notification settings apply).
- **Set price/quantity key = E:** only acts while a GE price or quantity prompt the player opened is
  focused, writes the value into it, and the player still confirms. Reviewers have accepted the same
  pattern elsewhere; if they ask, change the default to `Keybind.NOT_SET` in `GpGeniusConfig`.
- **Server URL:** must be the deployed HTTPS address before submitting (step 2 above).

### Screenshots

The Hub listing shows the icon only, but reviewers and the website guide use these. Take them on a
clean character at 100% UI scaling, side panel open, with the default theme:

1. Side panel, signed out: the data notice and **Agree & sign in**.
2. Side panel with a buy suggestion, and the GE slot highlighted in game.
3. The price/quantity prompt with the hotkey's value filled in, before confirming.
4. A completed flip in the recent flips list with profit and tax.
5. A dump alert card.
6. The server-down notice (stop the local backend to get it).
7. **Report a problem** dialog.

Save them under `docs/screenshots/` (PNG, at least 1280 px wide for in-game shots).

### Before you press "Create pull request"

- [ ] CI is green on the commit you submit (build, tests, dependency scan looked at).
- [ ] `ServerUrl` default is the live HTTPS address, and `/healthz` answers there.
- [ ] Repository is public; README's Privacy section matches the table above.
- [ ] One full in-game flip done on that exact commit (TESTING.md).
- [ ] `version` in `build.gradle` bumped.

## Keeping the server side private

The plugin is open source by the Hub's rules, but everything worth protecting lives on the server:
the suggestion engine, prices, Claude prompts, licensing and billing. The plugin only shows what the
server sends to the signed-in, entitled account, so a modified plugin can't unlock premium items or
see other players' data.
