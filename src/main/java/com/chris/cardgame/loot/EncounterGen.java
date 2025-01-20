package com.chris.cardgame.loot;

import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;

import com.chris.cardgame.data.EnemyLoader;
import com.chris.cardgame.model.EnemyDef;

public class EncounterGen {
    private static final Map<Integer, List<List<String>>> COMBAT = Map.of(
            1, List.of(List.of("rat"), List.of("rat", "imp"), List.of("cutthroat"),
                    List.of("imp", "imp"), List.of("rat", "rat", "imp"), List.of("pirate")),
            2, List.of(List.of("pirate"), List.of("pirate", "imp"), List.of("golem"),
                    List.of("duelist"), List.of("cutthroat", "imp"),
                    List.of("marauder")),
            3, List.of(List.of("golem", "imp"), List.of("marauder", "imp"),
                    List.of("herald", "imp"), List.of("assassin", "imp"),
                    List.of("duelist", "herald")));
    private static final Map<Integer, List<List<String>>> ELITE = Map.of(
            1, List.of(List.of("pirate", "imp"), List.of("pirate", "rat", "rat")),
            2, List.of(List.of("golem", "imp"), List.of("duelist", "cutthroat")),
            3, List.of(List.of("golem", "herald"), List.of("assassin", "marauder")));
    private static final Map<Integer, List<String>> BOSS = Map.of(
            1, List.of("mara", "rat"),
            2, List.of("rustking", "imp"),
            3, List.of("vex", "herald"));

    private final EnemyLoader enemies;

    public EncounterGen(EnemyLoader enemies) {
        this.enemies = enemies;
    }

    public List<EnemyDef> combat(int act, SplittableRandom rng) {
        return pick(COMBAT.get(act), rng);
    }

    public List<EnemyDef> elite(int act, SplittableRandom rng) {
        return pick(ELITE.get(act), rng);
    }

    public List<EnemyDef> boss(int act) {
        List<String> ids = BOSS.get(act);
        if (ids == null) {
            throw new IllegalArgumentException("no boss for act " + act);
        }
        return ids.stream().map(enemies::get).toList();
    }

    private List<EnemyDef> pick(List<List<String>> pool, SplittableRandom rng) {
        if (pool == null || pool.isEmpty()) {
            throw new IllegalArgumentException("empty encounter pool");
        }
        return pool.get(rng.nextInt(pool.size())).stream().map(enemies::get).toList();
    }
}
