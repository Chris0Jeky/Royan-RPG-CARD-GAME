package com.chris.cardgame.run;

import java.io.PrintStream;
import java.util.Set;
import java.util.stream.Collectors;

import com.chris.cardgame.data.RelicLoader;
import com.chris.cardgame.model.RelicDef;

public class Tavern {
    public static final int HEAL_COST = 40;
    public static final int REMOVE_COST_DUST = 50;
    public static final int RELIC_COST_GOLD = 100;
    public static final int RELIC_COST_SHARDS = 1;

    private final RelicLoader relics;

    public Tavern(RelicLoader relics) {
        this.relics = relics;
    }

    public void visit(RunState state, PrintStream out) {
        out.println("  Tavern: stew, songs, and sharp company.");
        Set<String> owned = state.relics().stream().map(RelicDef::id).collect(Collectors.toSet());
        if (state.shards() >= RELIC_COST_SHARDS && state.gold() >= RELIC_COST_GOLD) {
            relics.offer(owned, true, state.rng()).ifPresent(relic -> {
                if (state.spendShards(RELIC_COST_SHARDS) && state.spendGold(RELIC_COST_GOLD)) {
                    state.addRelic(relic);
                    out.println("  Traded a shard + " + RELIC_COST_GOLD + " gold for relic: "
                            + relic.name() + ".");
                }
            });
        }
        if (state.dust() >= REMOVE_COST_DUST && state.removeBasic()) {
            state.spendDust(REMOVE_COST_DUST);
            out.println("  Paid " + REMOVE_COST_DUST + " dust to strike a basic card from the deck.");
        }
        if (state.hero().hp() < state.hero().maxHp() && state.gold() >= HEAL_COST) {
            state.spendGold(HEAL_COST);
            int heal = state.hero().maxHp() / 2;
            state.hero().heal(heal);
