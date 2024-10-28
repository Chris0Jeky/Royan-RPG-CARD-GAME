package com.chris.cardgame.data;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SplittableRandom;
import java.util.stream.Collectors;

import com.chris.cardgame.model.Rarity;
import com.chris.cardgame.model.RelicDef;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class RelicLoader {
    private final Map<String, RelicDef> relics = new LinkedHashMap<>();

    public static RelicLoader load() {
        try (InputStream in = RelicLoader.class.getResourceAsStream("/data/relics.json")) {
            if (in == null) {
                throw new IllegalStateException("missing /data/relics.json on classpath");
            }
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(in);
            RelicLoader loader = new RelicLoader();
            for (JsonNode node : root.get("relics")) {
                RelicDef def = mapper.treeToValue(node, RelicDef.class);
                loader.relics.put(def.id(), def);
            }
            return loader;
        } catch (IOException e) {
            throw new IllegalStateException("failed to load relics.json", e);
        }
    }

    public RelicDef get(String id) {
        RelicDef def = relics.get(id);
        if (def == null) {
            throw new IllegalArgumentException("unknown relic: " + id);
        }
        return def;
    }

    public List<RelicDef> all() {
        return List.copyOf(relics.values());
    }

    public Optional<RelicDef> offer(Set<String> owned, boolean elite, SplittableRandom rng) {
        List<RelicDef> pool = relics.values().stream()
                .filter(relic -> !owned.contains(relic.id()))
                .toList();
        if (pool.isEmpty()) {
            return Optional.empty();
        }
        List<RelicDef> remaining = new ArrayList<>(pool);
        int total = remaining.stream().mapToInt(relic -> weight(relic.rarity(), elite)).sum();
        int roll = rng.nextInt(total);
        for (RelicDef relic : remaining) {
            roll -= weight(relic.rarity(), elite);
            if (roll < 0) {
                return Optional.of(relic);
            }
        }
        return Optional.of(remaining.get(remaining.size() - 1));
    }
