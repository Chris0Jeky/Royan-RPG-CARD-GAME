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
        this.rng = new SplittableRandom(seed);
        this.gold = 50;
        this.xp = 0;
        this.level = 1;
        this.act = 1;
    }

    public Combatant hero() {
        return hero;
    }

    public HeroClass heroClass() {
        return heroClass;
    }

    public List<CardDef> deck() {
        return deck;
    }

    public SplittableRandom rng() {
        return rng;
    }

    public long seed() {
        return seed;
    }

    public int gold() {
        return gold;
    }

    public int xp() {
        return xp;
    }

    public int level() {
        return level;
    }

    public int act() {
        return act;
    }

    public void setAct(int act) {
        this.act = act;
    }

    public void addGold(int amount) {
