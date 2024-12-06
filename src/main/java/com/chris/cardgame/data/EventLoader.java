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
