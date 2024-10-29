package com.chris.cardgame.run;

import java.io.PrintStream;
import java.util.Comparator;
import java.util.List;
import java.util.SplittableRandom;

import com.chris.cardgame.data.CardLoader;
import com.chris.cardgame.data.RelicLoader;
import com.chris.cardgame.loot.LootGen;
import com.chris.cardgame.model.CardDef;
import com.chris.cardgame.model.RelicDef;

public class Shop {
    public static final int HEAL_COST = 30;
    public static final int HEAL_AMOUNT = 10;

    private final LootGen loot;
    private final RelicLoader relics;

    public Shop(CardLoader cards, RelicLoader relics) {
        this.loot = new LootGen(cards);
        this.relics = relics;
    }

    public static int price(CardDef card) {
        return switch (card.rarity()) {
            case COMMON -> 45;
            case UNCOMMON -> 70;
            case RARE -> 120;
            case ELITE -> 180;
        };
    }

    public static int relicPrice(RelicDef relic) {
        return switch (relic.rarity()) {
            case COMMON -> 120;
            case UNCOMMON -> 170;
            case RARE -> 240;
            case ELITE -> 350;
        };
    }

    public void visit(RunState state, PrintStream out) {
        SplittableRandom rng = state.rng();
        List<CardDef> stock = loot.cardOptions(state.heroClass(), state.deck(), true, rng);
        out.println("  Shop stock: " + stock.stream()
                .map(card -> card.name() + " (" + price(card) + "g)")
                .reduce((a, b) -> a + ", " + b).orElse("empty"));
        stock.stream()
                .sorted(Comparator.comparingInt(Shop::price).reversed())
                .filter(card -> state.gold() >= price(card))
                .findFirst()
                .ifPresent(card -> {
                    if (state.spendGold(price(card)) && state.addCard(card)) {
                        out.println("  Bought " + card.name() + " for " + price(card) + " gold.");
                    }
                });
        java.util.Set<String> owned = state.relics().stream()
                .map(RelicDef::id).collect(java.util.stream.Collectors.toSet());
        relics.offer(owned, false, rng).ifPresent(relic -> {
            if (state.gold() >= relicPrice(relic)) {
                state.spendGold(relicPrice(relic));
                state.addRelic(relic);
                out.println("  Bought relic " + relic.name() + " for " + relicPrice(relic) + " gold.");
            }
        });
        if (state.hero().hp() < state.hero().maxHp() * 7 / 10 && state.gold() >= HEAL_COST) {
            state.spendGold(HEAL_COST);
            state.hero().heal(HEAL_AMOUNT);
            out.println("  Paid " + HEAL_COST + " gold for healing (" + HEAL_AMOUNT + " HP).");
        }
    }
}
