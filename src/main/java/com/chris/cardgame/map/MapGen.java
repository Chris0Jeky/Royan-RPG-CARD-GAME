package com.chris.cardgame.map;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;

public class MapGen {
    private static final Map<Integer, int[]> LAYERS = Map.of(
            1, new int[]{3, 3, 2, 3, 3, 1},
            2, new int[]{3, 3, 3, 3, 2, 3, 1},
            3, new int[]{4, 3, 4, 3, 3, 4, 1});

    public ActMap generate(int act, long seed) {
        int[] layers = LAYERS.get(act);
