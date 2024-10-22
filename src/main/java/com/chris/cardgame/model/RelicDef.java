package com.chris.cardgame.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RelicDef(String id, String name, Rarity rarity, RelicEffect effect, int value,
        String flavor) {
}
