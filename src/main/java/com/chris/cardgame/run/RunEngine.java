package com.chris.cardgame.run;

import java.io.PrintStream;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.chris.cardgame.cli.GameLoop;
import com.chris.cardgame.data.CardLoader;
import com.chris.cardgame.data.CompanionLoader;
import com.chris.cardgame.data.EnemyLoader;
import com.chris.cardgame.data.EventLoader;
import com.chris.cardgame.data.RelicLoader;
import com.chris.cardgame.loot.Boon;
import com.chris.cardgame.loot.EncounterGen;
import com.chris.cardgame.loot.LootGen;
import com.chris.cardgame.map.ActMap;
import com.chris.cardgame.map.MapNode;
import com.chris.cardgame.model.CardDef;
import com.chris.cardgame.model.EnemyDef;
import com.chris.cardgame.model.RelicDef;

public class RunEngine {
    public static final int SALVAGE_DUST = 10;
    public static final int SALVAGE_DECK_SIZE = 24;

    private final EncounterGen encounters;
    private final LootGen loot;
    private final RelicLoader relics;
    private final Events events;
    private final Shop shop;
    private final Tavern tavern;

    public RunEngine(CardLoader cards, EnemyLoader enemies, RelicLoader relics,
            CompanionLoader companions, EventLoader events) {
        this.encounters = new EncounterGen(enemies);
        this.loot = new LootGen(cards);
        this.relics = relics;
        this.events = new Events(events, cards, relics, companions);
        this.shop = new Shop(cards, relics);
        this.tavern = new Tavern(relics, companions);
    }

    public boolean resolve(RunState state, MapNode node, PrintStream out) {
        out.println("Node " + node.id() + " [" + node.type() + "] - hero " + state.hero()
                + " | deck " + state.deck().size() + " | gold " + state.gold()
                + " | dust " + state.dust() + " | shards " + state.shards()
                + " | relics " + state.relics().size() + " | lvl " + state.level());
        return switch (node.type()) {
            case COMBAT -> combat(state, encounters.combat(state.act(), state.rng()), node.type(), out);
            case ELITE -> combat(state, encounters.elite(state.act(), state.rng()), node.type(), out);
            case BOSS -> combat(state, encounters.boss(state.act()), node.type(), out);
            case REST -> {
                int heal = Math.max(1, state.hero().maxHp() * 35 / 100);
                state.hero().heal(heal);
                out.println("  Rested: +" + heal + " HP.");
                yield true;
            }
            case SHOP -> {
                shop.visit(state, out);
                yield true;
            }
            case TAVERN -> {
                tavern.visit(state, out);
                yield state.hero().alive();
            }
            case EVENT -> {
                events.resolve(state, out);
                yield state.hero().alive();
            }
        };
    }

    public MapNode chooseNext(RunState state, ActMap map, MapNode node) {
        return node.children().stream()
                .map(map::node)
                .max(Comparator.comparingInt(child -> childScore(state, child)))
                .orElseThrow(() -> new IllegalStateException("dead end at " + node.id()));
    }

    private int childScore(RunState state, MapNode child) {
        return switch (child.type()) {
            case BOSS -> 1000;
            case ELITE -> 50;
            case COMBAT -> 40;
            case TAVERN -> 38;
            case SHOP -> state.gold() > 60 ? 37 : 15;
            case EVENT -> 30;
            case REST -> state.hero().hp() < state.hero().maxHp() * 17 / 20 ? 45 : 10;
        };
    }

    private boolean combat(RunState state, List<EnemyDef> foes,
            com.chris.cardgame.map.NodeType kind, PrintStream out) {
        boolean elite = kind == com.chris.cardgame.map.NodeType.ELITE
                || kind == com.chris.cardgame.map.NodeType.BOSS;
        GameLoop.BattleResult result = new GameLoop().runAutoBattle(
                state.hero(), state.companions(), state.deck(), foes,
                state.rng().nextLong(), 60, out);
        if (!result.victory()) {
            return false;
        }
        syncCompanions(state, result, out);
        int patchUp = state.healAfterCombat();
        if (patchUp > 0) {
            state.hero().heal(patchUp);
            out.println("  Relics mend " + patchUp + " HP.");
        }
        rewards(state, foes, elite, kind == com.chris.cardgame.map.NodeType.BOSS, out);
        return true;
    }

    private void syncCompanions(RunState state, GameLoop.BattleResult result, PrintStream out) {
        for (int i = state.companions().size() - 1; i >= 0; i--) {
            Companion ally = state.companions().get(i);
            ally.setHp(result.companionHp().get(i));
            if (!ally.alive()) {
                state.companions().remove(i);
                out.println("  " + ally.def().name() + " falls and must be carried home.");
            } else {
                ally.rest();
            }
        }
    }

    private void rewards(RunState state, List<EnemyDef> foes, boolean elite, boolean boss,
            PrintStream out) {
        int base = foes.stream()
                .mapToInt(foe -> loot.rollGold(foe.goldMin(), foe.goldMax(), state.rng()))
                .sum() + (elite ? 25 : 0);
        int gold = base * (100 + state.goldPctBonus()) / 100;
        int xp = foes.stream().mapToInt(EnemyDef::xp).sum();
        state.addGold(gold);
        out.println("  Spoils: +" + gold + " gold, +" + xp + " XP.");
        if (elite) {
            int shards = boss ? 2 : 1;
            state.addShards(shards);
            out.println("  Claimed " + shards + " sky-shard(s).");
            Set<String> owned = state.relics().stream()
                    .map(RelicDef::id).collect(Collectors.toSet());
            relics.offer(owned, true, state.rng()).ifPresentOrElse(
                    relic -> {
                        state.addRelic(relic);
                        out.println("  Relic claimed: " + relic.name() + " (" + relic.effect()
                                + " +" + relic.value() + ").");
                    },
                    () -> {
                        state.addGold(50);
                        out.println("  Relic vaults empty: +50 gold instead.");
                    });
        }
        List<Boon> boons = state.addXp(xp);
        boons.forEach(boon -> out.println("  Level " + state.level() + "! Boon: " + boon.name()
                + " (" + boon.desc() + ")."));
        if (state.deck().size() >= SALVAGE_DECK_SIZE) {
            state.addDust(SALVAGE_DUST);
            out.println("  Deck is honed: salvaged draft for +" + SALVAGE_DUST + " dust.");
            return;
        }
        List<CardDef> options = loot.cardOptions(state.heroClass(), state.deck(), elite, state.rng());
        if (options.isEmpty()) {
            out.println("  No draft options (collection exhausted).");
            return;
        }
        options.sort(Comparator.comparingInt(card -> card.rarity().ordinal()));
        int drafts = elite ? 2 : 1;
        for (int i = 0; i < drafts && !options.isEmpty(); i++) {
            CardDef pick = options.remove(options.size() - 1);
            if (state.addCard(pick)) {
                out.println("  Drafted: " + pick.name() + " (" + pick.rarity() + ").");
            } else {
                out.println("  Draft skipped (deck full): " + pick.name() + ".");
            }
        }
    }
}
