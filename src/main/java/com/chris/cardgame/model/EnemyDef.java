package com.chris.cardgame.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record EnemyDef(
        String id,
        String name,
        int hp,
        int atk,
        Aspect aspect,
        Row row,
        Behavior behavior,
        int xp,
        int goldMin,
        int goldMax,
        boolean boss,
        PhaseTwo phaseTwo,
        String flavor) {

    public EnemyDef {
        if (boss && phaseTwo == null) {
            throw new IllegalArgumentException("boss without phase two: " + id);
        }
    }
}
