package com.chris.cardgame.run;

import java.io.PrintStream;
import java.util.Set;
import java.util.stream.Collectors;

import com.chris.cardgame.data.RelicLoader;
import com.chris.cardgame.model.RelicDef;

public class Tavern {
    public static final int HEAL_COST = 40;
    public static final int REMOVE_COST_DUST = 50;
    public static final int RELIC_COST_GOLD = 100;
    public static final int RELIC_COST_SHARDS = 1;

    private final RelicLoader relics;

    public Tavern(RelicLoader relics) {
        this.relics = relics;
