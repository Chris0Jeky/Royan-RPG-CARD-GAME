package com.chris.cardgame.loot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;

import com.chris.cardgame.data.CardLoader;
import com.chris.cardgame.model.CardDef;
import com.chris.cardgame.model.HeroClass;
import com.chris.cardgame.model.Rarity;

public class LootGen {
    public static final int OPTIONS = 3;
    public static final int MAX_COPIES = 3;
    public static final int MAX_DECK = 30;
