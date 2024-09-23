package com.chris.cardgame.run;

import java.io.PrintStream;
import java.util.Comparator;
import java.util.List;

import com.chris.cardgame.cli.GameLoop;
import com.chris.cardgame.data.CardLoader;
import com.chris.cardgame.data.EnemyLoader;
import com.chris.cardgame.loot.Boon;
import com.chris.cardgame.loot.EncounterGen;
import com.chris.cardgame.loot.LootGen;
import com.chris.cardgame.map.ActMap;
import com.chris.cardgame.map.MapNode;
import com.chris.cardgame.model.CardDef;
import com.chris.cardgame.model.EnemyDef;

public class RunEngine {
    private final EncounterGen encounters;
    private final LootGen loot;
    private final Events events;
    private final Shop shop;

    public RunEngine(CardLoader cards, EnemyLoader enemies) {
        this.encounters = new EncounterGen(enemies);
        this.loot = new LootGen(cards);
        this.events = new Events(cards);
