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
