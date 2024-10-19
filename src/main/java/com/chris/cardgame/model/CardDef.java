package com.chris.cardgame.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CardDef(
        String id,
        String name,
        int cost,
        CardType type,
        Aspect aspect,
        @JsonProperty(defaultValue = "0") int damage,
        @JsonProperty(defaultValue = "0") int block,
        @JsonProperty(defaultValue = "0") int draw,
        @JsonProperty(defaultValue = "0") int heal,
        @JsonProperty(defaultValue = "0") int weak,
        @JsonProperty(defaultValue = "0") int vulnerable,
        @JsonProperty(defaultValue = "0") int strength,
        HeroClass heroClass,
        Rarity rarity,
        @JsonProperty(defaultValue = "false") boolean unplayable,
        @JsonProperty(defaultValue = "0") int hits,
        @JsonProperty(defaultValue = "false") boolean aoe,
        @JsonProperty(defaultValue = "0") int energy,
        String flavor) {

    public CardDef {
        if (cost < 0 || cost > 3) {
            throw new IllegalArgumentException("cost out of range 0-3: " + id);
        }
        if (hits <= 0) {
            hits = 1;
        }
    }

    public boolean targetsEnemy() {
        return damage > 0 || weak > 0 || vulnerable > 0;
    }

    public boolean targetsSelf() {
        return block > 0 || draw > 0 || heal > 0 || strength > 0;
    }
}
