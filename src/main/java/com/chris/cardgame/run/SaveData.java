package com.chris.cardgame.run;

import java.util.List;
import java.util.Set;

import com.chris.cardgame.model.HeroClass;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SaveData(int version, long seed, HeroClass heroClass, int act, String nodeId,
