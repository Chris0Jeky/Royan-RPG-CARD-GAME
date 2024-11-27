package com.chris.cardgame.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record EventDef(String id, String title, String text, List<EventChoice> choices) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record EventChoice(String id, String text, EventCost requires, EventEffect effects) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record EventCost(int gold, int dust, int shards, int hp) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record EventEffect(int gold, int dust, int shards, int heal, int damage, int maxHp,
            int strength, boolean draft, boolean relic, boolean curse, boolean removeBasic,
            boolean companion) {
    }
}
