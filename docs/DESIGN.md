# DESIGN — Royan RPG Card Game (locked rules)

> Design authority. Engine + tests must match this file. Change via explicit balance decision (log at bottom).

## Fantasy
Sky-isles mercenary. You are a Guild Captain: draft a war-band, build a deck, cross a 3-act
branching map, and kill the Sky-Tyrant's captains (2-phase bosses). Runs ~8 min, campaign ~45 min.

## Core loop
Hub (deck + tavern upgrades) → act map (15/18/22 nodes: combat / elite / rest / shop / event /
boss) → combat → pick-1-of-3 loot + XP → repeat → boss → next act.

## Combat (M1 — implemented)
- Turn: gain 3 energy, draw 4 cards, enemy intents telegraphed, play cards, end turn (discard hand).
- Piles: draw (shuffled) → hand → discard; empty draw reshuffles discard. Seeded RNG per battle.
- Card types: Strike (damage), Guard (block), Trick (damage + debuff / utility), Power (combat buff),
  Curse (unplayable, clogs hand).
- Card fields are data: damage, block, draw, heal, weak, vulnerable, strength; target SELF /
  ENEMY_ONE / ALL_ENEMIES. Cost 0–3.
- Aspect triangle: MIGHT → GUILE → FOCUS → MIGHT. Advantage ×1.5, disadvantage ×0.75.
- Rows: FRONT / BACK. BACK-row targets take ×0.75 while any FRONT ally lives (cover).
- Weak (attacker): ×0.75 damage. Vulnerable (target): ×1.25 damage taken. Durations tick down
  each round.
- Block absorbs damage, then HP. Block clears at the start of the owner's turn.
- Hero aspect = class aspect (Knight MIGHT / Ranger GUILE / Runemage FOCUS).
- Victory: all enemies dead. Defeat: hero HP 0. Companions (M4): hero + 0–2 vs 1–3 enemies.

## Heroes & decks (M1: Knight only; M3/M5: all three)
- Knight (MIGHT): bruiser, block + heavy hits. Ranger (GUILE): cheap strikes, draw, weak.
  Runemage (FOCUS): burst, vulnerable, powers.
- Deck: start 12, max 30, max 3 copies. Starter Knight: 4 Strike, 3 Guard, 2 Heavy Blow,
  1 Rally, 1 Quick Cut, 1 Bulwark.

## Progression (M2/M3)
- Levels 1–10, pick-1-of-3 boons on level-up. XP from combat + events.
- Economy: gold (shops/shrines), dust (card upgrades, 3:1), shards (relic rerolls). No premium.
- 20 relics, 24 narrative events, tavern (heal / recruit / upgrade), shrines (card surgery).

## Enemy AI (M1: weighted intents; M4: 4 behaviors + 2-phase bosses)
- Intents: ATTACK / DEFEND / BUFF / DEBUFF, rolled per enemy per turn from behavior weights.
- M1 behaviors are data (attack/defend/buff weights per enemy).

## Balance log
- 2024-07-?? (M1): multipliers locked — advantage 1.5, disadvantage/cover/weak 0.75, vulnerable 1.25.
