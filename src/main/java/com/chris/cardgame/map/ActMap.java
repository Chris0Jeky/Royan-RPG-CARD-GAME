package com.chris.cardgame.map;

import java.util.List;
import java.util.Map;

public record ActMap(int act, Map<String, MapNode> nodes, List<String> entries, String bossId) {

    public MapNode node(String id) {
        MapNode node = nodes.get(id);
        if (node == null) {
            throw new IllegalArgumentException("unknown node: " + id);
        }
        return node;
    }

    public int layers() {
        return nodes.values().stream().mapToInt(MapNode::layer).max().orElse(0) + 1;
    }
}
