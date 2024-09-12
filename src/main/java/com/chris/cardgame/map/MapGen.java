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
            linkLayers(nodes, byLayer.get(layer), byLayer.get(layer + 1), rng);
        }
        List<String> entries = List.copyOf(byLayer.get(0));
        String bossId = byLayer.get(byLayer.size() - 1).get(0);
        return new ActMap(act, nodes, entries, bossId);
    }

    private NodeType nodeType(int act, int layer, int layerCount, SplittableRandom rng) {
        if (layer == 0) {
            return NodeType.COMBAT;
        }
        if (layer == layerCount - 1) {
            return NodeType.BOSS;
        }
        if (layer == layerCount - 2) {
            return NodeType.REST;
        }
        double roll = rng.nextDouble();
        if (roll < 0.45) {
            return NodeType.COMBAT;
        }
        if (roll < 0.65) {
            return NodeType.EVENT;
        }
        if (roll < 0.77) {
            return NodeType.SHOP;
        }
        if (roll < 0.89) {
            return NodeType.REST;
        }
        return NodeType.ELITE;
    }

    private void linkLayers(Map<String, MapNode> nodes, List<String> from, List<String> to,
            SplittableRandom rng) {
        for (int i = 0; i < from.size(); i++) {
            int primary = (int) Math.round((double) i * (to.size() - 1) / Math.max(1, from.size() - 1));
            connect(nodes, from.get(i), to.get(clamp(primary, to.size())));
            if (to.size() > 1 && rng.nextDouble() < 0.5) {
                int extra = primary + (rng.nextBoolean() ? 1 : -1);
                if (extra >= 0 && extra < to.size() && extra != primary) {
                    connect(nodes, from.get(i), to.get(extra));
                }
            }
        }
        for (int j = 0; j < to.size(); j++) {
            String target = to.get(j);
            boolean orphan = from.stream().noneMatch(id -> nodes.get(id).children().contains(target));
            if (orphan) {
                int nearest = (int) Math.round((double) j * (from.size() - 1) / Math.max(1, to.size() - 1));
                connect(nodes, from.get(clamp(nearest, from.size())), target);
            }
        }
    }

    private void connect(Map<String, MapNode> nodes, String from, String to) {
        List<String> children = nodes.get(from).children();
        if (!children.contains(to)) {
            children.add(to);
        }
    }

    private int clamp(int value, int size) {
        return Math.max(0, Math.min(size - 1, value));
    }
}
