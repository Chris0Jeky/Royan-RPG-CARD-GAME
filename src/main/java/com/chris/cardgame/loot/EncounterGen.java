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
