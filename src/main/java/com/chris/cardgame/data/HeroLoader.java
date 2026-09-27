package com.chris.cardgame.data;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.chris.cardgame.model.HeroClass;
import com.chris.cardgame.model.HeroDef;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class HeroLoader {
    private final Map<String, HeroDef> heroes = new LinkedHashMap<>();

    public static HeroLoader load() {
        try (InputStream in = HeroLoader.class.getResourceAsStream("/data/heroes.json")) {
            if (in == null) {
                throw new IllegalStateException("missing /data/heroes.json on classpath");
            }
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(in);
            HeroLoader loader = new HeroLoader();
            for (JsonNode node : root.get("heroes")) {
                HeroDef def = mapper.treeToValue(node, HeroDef.class);
                loader.heroes.put(def.id(), def);
            }
            return loader;
        } catch (IOException e) {
            throw new IllegalStateException("failed to load heroes.json", e);
        }
    }

    public HeroDef get(String id) {
        HeroDef def = heroes.get(id);
        if (def == null) {
            throw new IllegalArgumentException("unknown hero: " + id);
        }
        return def;
    }

    public HeroDef get(HeroClass heroClass) {
        return get(heroClass.name());
    }

    public List<HeroDef> all() {
        return List.copyOf(heroes.values());
    }
}
