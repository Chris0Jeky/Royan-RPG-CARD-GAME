package com.chris.cardgame.map;

import java.util.List;
import java.util.Map;

public record ActMap(int act, Map<String, MapNode> nodes, List<String> entries, String bossId) {
