package com.chris.cardgame.run;

import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;

import com.chris.cardgame.loot.Boon;
import com.chris.cardgame.loot.LootGen;
import com.chris.cardgame.loot.XpCurve;
import com.chris.cardgame.model.CardDef;
import com.chris.cardgame.model.Combatant;
import com.chris.cardgame.model.HeroClass;

public class RunState {
    private final Combatant hero;
    private final HeroClass heroClass;
    private final List<CardDef> deck;
    private final SplittableRandom rng;
    private final long seed;
    private int gold;
    private int xp;
    private int level;
    private int act;

    public RunState(String heroName, HeroClass heroClass, List<CardDef> starterDeck, long seed) {
        this.hero = Combatant.hero(heroName, heroClass, 80);
        this.heroClass = heroClass;
        this.deck = new ArrayList<>(starterDeck);
        this.seed = seed;
