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
