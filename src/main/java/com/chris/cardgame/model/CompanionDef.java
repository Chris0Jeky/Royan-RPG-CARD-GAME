package com.chris.cardgame.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CompanionDef(String id, String name, Aspect aspect, int hp, int power,
        CompanionRole role, String flavor) {
}
