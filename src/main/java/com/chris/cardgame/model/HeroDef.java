package com.chris.cardgame.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record HeroDef(
        String id,
        String title,
        String origin,
        String motive,
        String triumph,
        String epitaph) {
}
