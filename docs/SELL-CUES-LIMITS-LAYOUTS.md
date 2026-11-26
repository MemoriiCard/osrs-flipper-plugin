# Sell-now cues, buy limit timers, panel layouts

## One rule for every nudge

Players must not be barraged. Sell-now cues, dump alerts and offers that need updating all go
through `NudgeGate`:

- The same nudge is said once, and not again until it has been quiet for a check-in window (at
  least 30 minutes).
- Everything is batched into the one check-in reminder (`CheckInDigest`), at the player's own
  "How often you check" time.
- Only urgent nudges may bring that reminder forward: buyers paying the player's target right now, a
  dump alert, or an offer that needs updating. At most twice per check-in window, at least 5 minutes
  apart, never during quiet hours, and never while the GE is open (the highlights already show it).
  *Urgent reminders early* (Check-in reminders section) turns this off.

The server decides what is urgent (`sell_cues[].urgent`); the plugin only rate limits.

## Sell-now highlight

Held items the server flags get a quiet outline and a small "$" in the inventory, in the normal
inventory tab and the GE's. Hovering says what to do ("Sell now at 1,610,000", "Falling: sell at
...", "Target reached: list at ..."). Urgent ones pulse if the highlight pulse is on. The player
still sells by hand. Setting: *Sell-now highlight* (Offer setup).

## Buy limit timers

The *Buy limits* card under the next step lists each item bought in its current 4-hour window:
bought out of the limit, what's left, and a live countdown to the reset. Items at their limit say
other items are suggested meanwhile. The count comes from the server (this character's uploaded buys
plus fills on its open offers).

## Panel layouts and themes

*Panel layout*: Detailed (everything, the default), Compact (no price chart or flip list, as the old
compact switch), Minimal (next step with its price, and profit totals), or Custom with your own pick
under *Panel parts (Custom)*: step price, estimated profit, details, goal, dump alerts, buy limits,
price chart, profit totals, cash stack, recent flips, and the flip list's quantity and price columns.
The *Compact panel* switch and its hotkey still work and mean Compact. Layout and parts are saved
with the profile.

Two new themes, Deep Sea and Slate, pass the same contrast checks as the others (ThemeTest) and are
on the website too.
