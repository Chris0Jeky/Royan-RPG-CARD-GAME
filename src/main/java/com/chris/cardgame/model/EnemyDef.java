package com.chris.cardgame.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record EnemyDef(
        String id,
        String name,
        int hp,
