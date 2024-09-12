package com.chris.cardgame.map;

import java.util.List;

public record MapNode(String id, int act, int layer, NodeType type, List<String> children) {
}
