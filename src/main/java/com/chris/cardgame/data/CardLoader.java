package com.chris.cardgame.data;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.chris.cardgame.model.CardDef;
import com.chris.cardgame.model.HeroClass;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class CardLoader {
    private final Map<String, CardDef> cards = new LinkedHashMap<>();
    private final Map<HeroClass, List<String>> starters = new LinkedHashMap<>();

    public static CardLoader load() {
        try (InputStream in = CardLoader.class.getResourceAsStream("/data/cards.json")) {
            if (in == null) {
                throw new IllegalStateException("missing /data/cards.json on classpath");
            }
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(in);
            CardLoader loader = new CardLoader();
            for (JsonNode node : root.get("cards")) {
                CardDef def = mapper.treeToValue(node, CardDef.class);
                loader.cards.put(def.id(), def);
            }
            JsonNode decks = root.get("starterDecks");
            decks.fields().forEachRemaining(entry -> {
