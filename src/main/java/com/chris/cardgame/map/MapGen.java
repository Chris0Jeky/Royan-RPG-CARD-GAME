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
        if (layers == null) {
            throw new IllegalArgumentException("no such act: " + act);
        }
        SplittableRandom rng = new SplittableRandom(seed);
        Map<String, MapNode> nodes = new LinkedHashMap<>();
        List<List<String>> byLayer = new ArrayList<>();

        for (int layer = 0; layer < layers.length; layer++) {
            List<String> ids = new ArrayList<>();
            for (int i = 0; i < layers[layer]; i++) {
                String id = "a" + act + "-L" + layer + "-" + i;
                NodeType type = nodeType(act, layer, layers.length, rng);
                nodes.put(id, new MapNode(id, act, layer, type, new ArrayList<>()));
                ids.add(id);
            }
            byLayer.add(ids);
        }
        for (int layer = 0; layer < layers.length - 1; layer++) {
