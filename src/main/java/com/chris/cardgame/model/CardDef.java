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
        String flavor) {

    public CardDef {
