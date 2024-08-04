package com.chris.cardgame.data;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.chris.cardgame.model.EnemyDef;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class EnemyLoader {
    private final Map<String, EnemyDef> enemies = new LinkedHashMap<>();

    public static EnemyLoader load() {
        try (InputStream in = EnemyLoader.class.getResourceAsStream("/data/enemies.json")) {
            if (in == null) {
                throw new IllegalStateException("missing /data/enemies.json on classpath");
            }
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(in);
            EnemyLoader loader = new EnemyLoader();
            for (JsonNode node : root.get("enemies")) {
                EnemyDef def = mapper.treeToValue(node, EnemyDef.class);
                loader.enemies.put(def.id(), def);
            }
            return loader;
        } catch (IOException e) {
            throw new IllegalStateException("failed to load enemies.json", e);
        }
    }

    public EnemyDef get(String id) {
