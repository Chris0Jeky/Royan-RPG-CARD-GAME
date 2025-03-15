# royan-design — Content & balance rules

Use when adding or tuning cards, enemies, relics, events, companions, or pools.

## Budgets (M5 lock — change deliberately, log in DESIGN.md)

- 90 cards: 24 per hero + 18 neutral. Cost 0–3. Starters: 12 cards (3/4/2/1/1/1 shape).
- 12 enemies (9 normal + 3 bosses), 20 relics, 24 events (≥2 choices each), 5 companions.
- Hero HP: Knight 90 / Ranger 80 / Runemage 72. Hero energy 3, draw 4.
- Multipliers: advantage ×1.5, disadvantage/cover/weak ×0.75, vulnerable ×1.25.

## Aspect coverage (the skew killer)

- Pools must mix all three aspects every act; no hero may face >60% hated-aspect
  fights in an act. Bosses shift aspect in phase 2 (M→G, F→M, G→F rotation).
- Every class needs ≥2 playable answers to its hated aspect (off-aspect damage,
  vuln/weak, or block to stall). Knight→GUILE, Ranger→FOCUS, Mage→MIGHT.

## Fight-length targets

- Normals 3–5 turns, elites 5–8, bosses 8–12 (greedy auto-pilot).
- Incoming damage per fight should cost ≤40% HP with basic blocking.

## Procedure

1. Edit JSON data (never hardcode content).
2. Update the pinning tests (sizes, exact numbers) in the same change.
3. Run `mvn test`, then the 30-seed balance oracle per hero (see docs/QA.md).
4. Bands: every hero wins 3–28/30. Move victory pins deliberately with disclosure.
5. Log the change + resulting rates in DESIGN.md balance log.
