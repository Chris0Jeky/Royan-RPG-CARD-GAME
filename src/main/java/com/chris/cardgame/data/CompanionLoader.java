package com.chris.cardgame.data;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.chris.cardgame.model.CompanionDef;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class CompanionLoader {
    private final Map<String, CompanionDef> companions = new LinkedHashMap<>();

    public static CompanionLoader load() {
        try (InputStream in = CompanionLoader.class.getResourceAsStream("/data/companions.json")) {
            if (in == null) {
                throw new IllegalStateException("missing /data/companions.json on classpath");
            }
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(in);
            CompanionLoader loader = new CompanionLoader();
            for (JsonNode node : root.get("companions")) {
                CompanionDef def = mapper.treeToValue(node, CompanionDef.class);
                loader.companions.put(def.id(), def);
            }
            return loader;
        } catch (IOException e) {
