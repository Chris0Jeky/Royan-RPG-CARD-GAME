package com.chris.cardgame.run;

import java.io.PrintStream;
import java.util.List;
import java.util.SplittableRandom;

import com.chris.cardgame.data.CardLoader;
import com.chris.cardgame.loot.LootGen;
import com.chris.cardgame.model.CardDef;

public class Events {
    private final CardLoader cards;
    private final LootGen loot;

    public Events(CardLoader cards) {
