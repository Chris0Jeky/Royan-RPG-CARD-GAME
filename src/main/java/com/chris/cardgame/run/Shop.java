package com.chris.cardgame.run;

import java.io.PrintStream;
import java.util.Comparator;
import java.util.List;
import java.util.SplittableRandom;

import com.chris.cardgame.data.CardLoader;
import com.chris.cardgame.loot.LootGen;
import com.chris.cardgame.model.CardDef;

public class Shop {
    public static final int HEAL_COST = 30;
    public static final int HEAL_AMOUNT = 10;

    private final LootGen loot;

