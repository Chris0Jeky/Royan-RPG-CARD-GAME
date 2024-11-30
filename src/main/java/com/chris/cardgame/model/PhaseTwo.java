package com.chris.cardgame.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PhaseTwo(int atk, Behavior behavior, int heal, int strength, String herald) {
}
