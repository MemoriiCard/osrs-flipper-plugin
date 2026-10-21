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
- No data about other players; game accounts are identified by RuneLite's account hash only.
- Tokens are stored outside RuneLite's config (so they never sync to a RuneLite profile) and only sent
  over HTTPS.

## Left to do before submitting

1. **Deploy the backend** on its own domain with HTTPS (for example `api.<your domain>`), and set
   `FLIPPER_PUBLIC_BASE_URL`, `FLIPPER_SECRET_KEY` and the Stripe keys there.
2. **Point the plugin at it:** change the default of `apiUrl` in `OsrsFlipperConfig` from
   `http://localhost:8000` to the HTTPS address. The plugin refuses plain HTTP to anything but
   localhost, so the default has to be HTTPS.
3. **Test in-game** on a members and a free-to-play world: sign in, one full flip with highlights and
   the hotkey, a dump alert, the guard on a mistyped price, and logging out mid-offer.
4. **Merge the plugin PRs** into `main` in order, then take the commit hash of `main`.
5. **Open a pull request on [runelite/plugin-hub](https://github.com/runelite/plugin-hub)** adding
   `plugins/osrs-flipper` with:

   ```
   repository=https://github.com/MemoriiCard/osrs-flipper-plugin.git
   commit=<commit hash from step 4>
   ```

   Reviewers read the code, so the repository must be public at that point. The Hub builds the plugin
   itself; nothing is uploaded.
6. **Updates:** each new version is another plugin-hub pull request with a new commit hash.

## Keeping the server side private

The plugin is open source by the Hub's rules, but everything worth protecting lives on the server:
the suggestion engine, prices, Claude prompts, licensing and billing. The plugin only shows what the
server sends to the signed-in, entitled account, so a modified plugin can't unlock premium items or
see other players' data.
