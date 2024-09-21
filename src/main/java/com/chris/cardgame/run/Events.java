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
