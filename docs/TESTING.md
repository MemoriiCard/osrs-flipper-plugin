# Testing OSRS Flipper before it's on the Plugin Hub

Until the Plugin Hub approves the plugin, testers run a copy of RuneLite with the plugin built in.
It's the normal RuneLite client plus this plugin, nothing else.

## For Nick: getting the tester file

1. Open the repository's **Actions** tab on GitHub, then the latest green **Build** run on `main` (or
   on the pull request you want to test).
2. Under **Artifacts**, download **osrs-flipper-tester-jar** and unzip it. Inside is
   `osrs-flipper-<version>-all.jar`.
3. Send that file to your testers, along with your server address (for example
   `https://osrsflipper.gg`).
4. Give each tester premium from the admin panel once they've signed in: **Users**, find them, then
   **Add free time** with the reason "Beta tester".

Rebuild and resend the file whenever a new version is merged.

## For testers: one-time setup (about 10 minutes)

### 1. Install Java 11 or newer
Download **Temurin 17** from [adoptium.net](https://adoptium.net) and install it with the default
options.

### 2. Let the test client use your Jagex account
The test client can't show the Jagex launcher's sign-in, so you let the normal RuneLite save your login
for it once:

1. Open the **RuneLite (configure)** shortcut. It's next to RuneLite in your Start menu, or on Mac run
   `/Applications/RuneLite.app/Contents/MacOS/RuneLite --configure`.
2. In **Client arguments**, add `--insecure-write-credentials` and click **Save**.
3. Start RuneLite once from the **Jagex Launcher** as usual, then close it.

That saves a login file in your `.runelite` folder that the test client reads. "Insecure" here means
the file isn't encrypted, so don't share your `.runelite` folder. To undo it later, remove the argument
and delete `.runelite/credentials.properties`.

If you play on a legacy (non-Jagex) account, skip this step and log in normally.

### 3. Start the test client
Put the `.jar` file somewhere handy and run, from a terminal in that folder:

```
java -ea -jar osrs-flipper-0.3.0-all.jar
```

(Use the file's actual name. On Windows you can paste this into PowerShell after shift+right-clicking
the folder and choosing **Open PowerShell window here**.)

### 4. Point the plugin at the server
1. In the test client, open settings (the wrench), search for **OSRS Flipper**.
2. Open **Connection** and paste the server address Nick gave you into **Server URL**.
3. Open the **OSRS Flipper** side panel from the sidebar, click **Sign in**, and approve the code on
   the web page that opens.

## What to try and report

Play as you normally would and flip with it. Things especially worth reporting:

- A suggestion that lost money or never filled. Say what the item was, roughly when, and your
  **Risk** and **Timeframe** settings.
- A highlight or hotkey that pointed at the wrong thing.
- Anything confusing in the side panel or on the website.
- Use the **Good** and **Bad** buttons on suggestions. They're recorded, so they help even without a
  note.

Your trades sync to the website, so the dashboard there shows exactly what happened.
