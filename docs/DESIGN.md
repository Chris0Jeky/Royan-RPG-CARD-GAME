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
