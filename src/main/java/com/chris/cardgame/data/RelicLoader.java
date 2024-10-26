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
