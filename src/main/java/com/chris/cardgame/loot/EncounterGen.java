package com.chris.cardgame.loot;

import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;

import com.chris.cardgame.data.EnemyLoader;
import com.chris.cardgame.model.EnemyDef;

public class EncounterGen {
    private static final Map<Integer, List<List<String>>> COMBAT = Map.of(
            1, List.of(List.of("rat"), List.of("rat", "imp"), List.of("imp", "imp"),
                    List.of("pirate"), List.of("rat", "rat", "imp")),
            2, List.of(List.of("pirate"), List.of("pirate", "imp"), List.of("golem"),
                    List.of("pirate", "rat", "rat")),
            3, List.of(List.of("golem", "imp"), List.of("pirate", "pirate"),
                    List.of("golem", "rat", "imp")));
    private static final Map<Integer, List<List<String>>> ELITE = Map.of(
            1, List.of(List.of("pirate", "imp"), List.of("pirate", "rat", "rat")),
            2, List.of(List.of("golem", "imp"), List.of("pirate", "pirate", "imp")),
            3, List.of(List.of("golem", "pirate", "imp"), List.of("golem", "golem")));
    private static final Map<Integer, List<String>> BOSS = Map.of(
            1, List.of("pirate", "rat"),
            2, List.of("golem", "imp"),
            3, List.of("golem", "pirate"));

