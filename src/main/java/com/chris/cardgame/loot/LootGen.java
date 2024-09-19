package com.chris.cardgame.loot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;

import com.chris.cardgame.data.CardLoader;
import com.chris.cardgame.model.CardDef;
import com.chris.cardgame.model.HeroClass;
import com.chris.cardgame.model.Rarity;

public class LootGen {
    public static final int OPTIONS = 3;
    public static final int MAX_COPIES = 3;
    public static final int MAX_DECK = 30;

    private final CardLoader cards;

    public LootGen(CardLoader cards) {
        this.cards = cards;
    }

    public List<CardDef> cardOptions(HeroClass heroClass, List<CardDef> deck, boolean elite,
            SplittableRandom rng) {
        Map<String, Integer> counts = new HashMap<>();
        deck.forEach(card -> counts.merge(card.id(), 1, Integer::sum));
        List<CardDef> pool = cards.all().stream()
                .filter(card -> !card.unplayable())
                .filter(card -> card.heroClass() == heroClass || card.heroClass() == HeroClass.NEUTRAL)
                .filter(card -> counts.getOrDefault(card.id(), 0) < MAX_COPIES)
                .toList();
        List<CardDef> options = new ArrayList<>();
        List<CardDef> remaining = new ArrayList<>(pool);
        for (int i = 0; i < OPTIONS && !remaining.isEmpty(); i++) {
            CardDef pick = weightedPick(remaining, elite, rng);
            options.add(pick);
            remaining.remove(pick);
        }
        return options;
    }

    public int goldReward(List<Integer> enemyGold, SplittableRandom rng) {
        return enemyGold.stream().mapToInt(g -> g).sum();
    }

    public int rollGold(int min, int max, SplittableRandom rng) {
        return min + rng.nextInt(max - min + 1);
    }

    private CardDef weightedPick(List<CardDef> pool, boolean elite, SplittableRandom rng) {
        int total = pool.stream().mapToInt(card -> weight(card.rarity(), elite)).sum();
        int roll = rng.nextInt(total);
        for (CardDef card : pool) {
            roll -= weight(card.rarity(), elite);
            if (roll < 0) {
                return card;
            }
        }
        return pool.get(pool.size() - 1);
    }

    private int weight(Rarity rarity, boolean elite) {
        return switch (rarity) {
            case COMMON -> elite ? 40 : 60;
            case UNCOMMON -> elite ? 40 : 30;
            case RARE -> elite ? 20 : 10;
            case ELITE -> 5;
        };
    }
}
