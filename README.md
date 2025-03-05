# Royan RPG Card Game

A roguelike deckbuilder about a sky-isles mercenary Guild Captain. Draft a war-band,
build a deck, cross a 3-act branching map, and kill the Sky-Tyrant's captains —
three two-phase bosses with aspect-shifting second forms.

- 3 heroes (Knight / Ranger / Runemage), 90 cards, 12 enemies, 20 relics, 24 events, 5 companions
- Lane combat with front/back rows, telegraphed intents, and an aspect triangle
- Full RPG layer: XP levels 1–10, boons, gold/dust/shards economy, shops, taverns, shrines
- Playable in the terminal: `play` (interactive), `continue` (autosave resume), `auto` (demo)

## Quickstart

Requires Java 17+ and Maven 3.9+. No other runtime dependencies (offline-safe after
the first Maven pull).

```sh
mvn test                                   # build + 88 tests
mvn -q compile exec:java -Dexec.mainClass=com.chris.cardgame.Main -Dexec.args="play"
```

(PowerShell: insert `--%` before the `-D` flags, e.g.
`mvn -q compile exec:java --% "-Dexec.mainClass=com.chris.cardgame.Main" "-Dexec.args=play"`.)

Then pick a captain and type commands (`play <card> [foe]`, `end`, `quit` saves).
