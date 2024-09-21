package com.chris.cardgame.run;

import java.io.PrintStream;
import java.util.List;
import java.util.SplittableRandom;

import com.chris.cardgame.data.CardLoader;
import com.chris.cardgame.loot.LootGen;
import com.chris.cardgame.model.CardDef;

public class Events {
    private final CardLoader cards;
    private final LootGen loot;

    public Events(CardLoader cards) {
        this.cards = cards;
        this.loot = new LootGen(cards);
    }

    public void resolve(RunState state, PrintStream out) {
        SplittableRandom rng = state.rng();
        switch (rng.nextInt(4)) {
            case 0 -> {
                int gold = 30 + rng.nextInt(31);
                state.addGold(gold);
                out.println("  Event: Abandoned Cache - found " + gold + " gold.");
            }
            case 1 -> {
                int heal = 10 + rng.nextInt(11);
                state.hero().heal(heal);
                out.println("  Event: Old Shrine - restored " + heal + " HP.");
            }
            case 2 -> {
                int loss = 5 + rng.nextInt(6);
                state.hero().takeDamage(loss);
                int gold = 40 + rng.nextInt(21);
                state.addGold(gold);
                out.println("  Event: Toll Bridge - paid " + loss + " HP, earned " + gold + " gold.");
            }
            default -> {
                List<CardDef> options = loot.cardOptions(state.heroClass(), state.deck(), false, rng);
                if (!options.isEmpty() && state.gold() >= 40) {
                    state.spendGold(40);
                    state.addCard(options.get(0));
                    out.println("  Event: Wandering Smith - bought " + options.get(0).name() + " for 40 gold.");
                } else {
                    out.println("  Event: Wandering Smith - browsed wares, bought nothing.");
                }
            }
        }
    }
}
