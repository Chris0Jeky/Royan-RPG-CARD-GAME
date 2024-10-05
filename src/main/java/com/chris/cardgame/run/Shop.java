package com.chris.cardgame.run;

import java.io.PrintStream;
import java.util.Comparator;
import java.util.List;
import java.util.SplittableRandom;

import com.chris.cardgame.data.CardLoader;
import com.chris.cardgame.loot.LootGen;
import com.chris.cardgame.model.CardDef;

public class Shop {
    public static final int HEAL_COST = 30;
    public static final int HEAL_AMOUNT = 10;

    private final LootGen loot;

    public Shop(CardLoader cards) {
        this.loot = new LootGen(cards);
    }

    public static int price(CardDef card) {
        return switch (card.rarity()) {
            case COMMON -> 45;
            case UNCOMMON -> 70;
            case RARE -> 120;
            case ELITE -> 180;
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
