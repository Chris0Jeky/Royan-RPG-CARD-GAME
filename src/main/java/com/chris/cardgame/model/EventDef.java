package com.chris.cardgame.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record EventDef(String id, String title, String text, List<EventChoice> choices) {
