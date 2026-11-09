# Testing GP Genius before it's on the Plugin Hub

Until the Plugin Hub approves the plugin, testers run a copy of RuneLite with the plugin built in.
It's the normal RuneLite client plus this plugin, nothing else.

## For Nick: publishing the tester client

1. Once, set the beta server address: on GitHub open the repository's **Settings → Secrets and variables →
   Actions → Variables**, and add `FLIPPER_SERVER_URL` with your server's address (for example
   `https://gpgenius.gg`).
2. Open the **Actions** tab, choose **Tester release**, and click **Run workflow** on `main`. (Pushing a tag
   such as `v0.5.1` does the same.)
3. A few minutes later a new release appears under **Releases** with `GP-Genius-Beta-Windows.zip`, the
   `.jar` for Mac and Linux, and their SHA-256 checksums. The website's **Download** page links to the latest
   release, so testers always get the newest one.
4. Invite testers from the website's **Admin → Invites**. Opening the link makes them testers with every
   feature free.

Run the workflow again whenever a new version is merged.

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
Unzip `GP-Genius-Beta-Windows.zip` and double-click **Start GP Genius Beta**. On Mac or Linux,
run `java -ea -jar gp-genius-beta.jar` in the folder you downloaded it to.

### 4. Sign in
Open the **GP Genius** side panel from the sidebar. Read the data notice, click **Agree & sign in**,
and approve the code on the web page that opens. The release already points at the beta server; you
don't need to change **Server URL**.

## What to try and report

Play as you normally would and flip with it. Things especially worth reporting:

- A suggestion that lost money or never filled. Say what the item was, roughly when, and your
  **Risk level** and **I check my offers** settings.
- A highlight or hotkey that pointed at the wrong thing.
- Anything confusing in the side panel or on the website.
- Press **Block** on items you never want suggested; undo it in settings under **Blocked items**.
- Use the **Good** and **Bad** buttons on suggestions (Advanced view). They're recorded, so they help even without a
  note.

Your trades sync to the website, so the dashboard there shows exactly what happened. The website's
**Help** page answers the common questions.
