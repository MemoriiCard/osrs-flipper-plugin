# OSRS Flipper

A RuneLite plugin that tracks your Grand Exchange flips and profit, with flip suggestions,
offer highlights, and hotkeys on the way.

## What it does today

- Records every GE fill for each of your accounts, including fills that happen while you're logged out.
- Pairs buys with later sells of the same item (oldest first) and works out profit after GE tax.
- Shows session profit, all-time profit, and your recent flips in a side panel.

Trades are saved on your computer in `.runelite/osrs-flipper/`. Nothing is sent anywhere yet.

## Rules this plugin follows

It never clicks, types, or acts in game for you. Everything it does is a display or a value you
choose to use, in line with Jagex's third-party client guidelines and RuneLite's Plugin Hub rules.

## Development

Requires Java 11.

```
./gradlew build                 # compile and run unit tests
```

Run `OsrsFlipperPluginTest` from IntelliJ with VM option `-ea` to start RuneLite with the plugin loaded.
