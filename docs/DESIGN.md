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
  Card aspect (not hero aspect) drives attack advantage; defender uses own aspect.
- Rows: FRONT / BACK. BACK-row targets take ×0.75 while any FRONT ally lives (cover).
- Weak (attacker): ×0.75 damage. Vulnerable (target): ×1.25 damage taken. Durations tick down
  each round.
- Block absorbs damage, then HP. Block clears at the start of the owner's turn.
- Hero aspect = class aspect (Knight MIGHT / Ranger GUILE / Runemage FOCUS).
- Victory: all enemies dead. Defeat: hero HP 0. Companions (M4): hero + 0–2 vs 1–3 enemies.

## Heroes & decks (M5 — all three playable)
- Knight (MIGHT, 90 HP): bruiser, block + heavy hits. Ranger (GUILE, 80 HP): cheap strikes, draw, weak.
  Runemage (FOCUS, 72 HP): burst, vulnerable, powers.
- Deck: start 12, max 30, max 3 copies. Starter Knight: 3 Strike, 4 Guard, 2 Heavy Blow,
  1 Rally, 1 Quick Cut, 1 Bulwark.

## Progression (M2/M3)
- Levels 1–10, pick-1-of-3 boons on level-up. XP from combat + events.
- Economy: gold (shops/shrines), dust (card upgrades, 3:1), shards (relic rerolls). No premium.
- 20 relics, 28 narrative events (incl. two 2-part chains), tavern (heal / recruit / upgrade), shrines (card surgery).

## Enemy AI (M4 — implemented)
- 4 behaviors: AGGRO (attacks relentlessly), TURTLE (block + buffs), BURST (2 attacks then BUFF cycle),
  TRICKSTER (attacks + DEBUFF weakens). Intents telegraphed every turn.
- Bosses (Mara / Rust King / Vex) have 2 phases: at 50% HP they cleanse, heal, gain strength,
  and switch attack/behavior/ASPECT (Mara MIGHT→GUILE, Rust King FOCUS→MIGHT, Vex GUILE→FOCUS),
  so every hero meets one favored and one hated phase per boss.
  Enemies focus the hero; companions support from the back line.

## Playing the game (M5 — implemented)
- `play [CLASS] [seed]`: interactive campaign (validated prompts everywhere; `quit` saves).
- `continue`: resume the autosave (`.royan-save/save.json`, deleted on victory/defeat).
- `auto [seed] [CLASS]`: scripted demo campaign (also the balance probe + QA oracle).

## Events & companions (M4 — implemented)
- 28 narrative events with costed choices (gold/dust/shards/HP gates); chained sequels unlock via requiresSeen; no repeats within an act;
  auto-policy avoids lethal choices. Effects: currencies, heal/damage, boons, drafts, relics,
  curses, deck-thinning, companion recruits.
- 5 companions (Striker/Guardian/Medic), max 2 per run, recruited via tavern/events/guild desk;
  they act before enemies each round and rest between battles.

## Balance log
- 2024-07-?? (M1): multipliers locked — advantage 1.5, disadvantage/cover/weak 0.75, vulnerable 1.25.
- 2025-01-?? (M5): per-class HP (K90/R80/M72); bosses aspect-shift; duelist+assassin added; pools rebalanced.
  Final auto-win rates K 11/30, R 22/30, M 8/30 (greedy pilot; humans do better). Ranger easy, Mage hard — accepted.
- 2024-11-?? (M4): 10 enemies (4 behaviors) + 3 two-phase bosses, 24 events, 5 companions.
  Auto-win rates K 2/30, R 6/30, M 27/30 — Knight suffers vs FOCUS-heavy pools; Mage overperforms. M5 must rebalance.
- 2026-09-27 (W4): +4 chained events (28 total; requiresSeen gating, base pool untouched — pinned seeds bit-identical); chain payoffs slightly above curve for double-gated rarity. Hero stories, companion banter (web only); no engine-number changes.
- 2026-09-27 (W5): 90-campaign re-probe K 11/30, R 22/30, M 8/30 — M5 lock bit-identical; no tuning. Browser QA green.
- 2026-09-27 (V3 content): +4 chained events (32 total) as chains 3+4, rooted at chain finales (ledger-heir/ledger-settled after tollkeepers-price; kid-captain/kid-flag after stowaway-farewell) via requiresSeen — triple-gated, base pool untouched, pinned auto seeds bit-identical. Payoffs slightly above curve for triple-gated rarity (relic/companion/draft options behind gold+HP gates).
- 2026-09-27 (V3 content): quick-skirmish backend gains 5 escalating tiers (run/Skirmish + data/skirmish.json pools; strictly rising pool-HP bands, packs grow 1-2 foes at T1 to 3 at T5). Bosses deliberately excluded: skirmish decks are starter decks with no boons/relics/companions, so a boss phase would be a stat wall, not a fight — escalation comes from pool strength + pack size instead. Seeded pick (tier, rng) keeps skirmishes deterministic; no campaign/auto-path numbers touched.
- 2026-09-27 (V3 content): DAILY SEED playtester feature (run/DailySeed: UTC date -> long via SplitMix64 of the epoch day; pinned 2026-09-27/-28 + 2024-01-01, day-boundary tested). Backend only — GameSession threading left for the merge (pass DailySeed.today() as the run seed). No balance impact (seed selection, not numbers).
- 2024-10-?? (M3): 90 cards (24/class + 18 neutral), card aspect drives advantage, AoE/multi-hit/energy arts;
  20 relics, dust (draft salvage/shrines, card removal) + shards (elites/boss, tavern relics);
  auto-win rates KNIGHT 11/20, RANGER 20/20, RUNEMAGE 19/20 — Ranger/Mage overtuned, revisit at M5.
- 2024-08-?? (M2): enemy HP pools cut for 3-5 turn normals (rat 14, imp 12, pirate 24/7, golem 36/10);
  hero 80 HP; rest 35%, act transition 40%; elite/boss draft 2 cards; Second Wind heals 6;
  enemy DEFEND 5; War Paint boon (+2 base strength) added for damage scaling.
  Reference: seed 6 wins the campaign (level 6, 20 nodes).
