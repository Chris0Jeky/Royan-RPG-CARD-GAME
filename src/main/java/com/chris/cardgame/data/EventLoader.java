package com.chris.cardgame.data;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.chris.cardgame.model.EventDef;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class EventLoader {
    private final Map<String, EventDef> events = new LinkedHashMap<>();

    public static EventLoader load() {
        try (InputStream in = EventLoader.class.getResourceAsStream("/data/events.json")) {
            if (in == null) {
                throw new IllegalStateException("missing /data/events.json on classpath");
            }
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(in);
            EventLoader loader = new EventLoader();
            for (JsonNode node : root.get("events")) {
                EventDef def = mapper.readValue(node.traverse(), EventDef.class);
                loader.events.put(def.id(), def);
            }
            return loader;
        } catch (IOException e) {
            throw new IllegalStateException("failed to load events.json", e);
        }
    }

    public EventDef get(String id) {
        EventDef def = events.get(id);
        if (def == null) {
