package com.chris.cardgame.loot;

import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;

public record Boon(String id, String name, String desc, int maxHp, int heal, int gold, int strength) {

    public static final List<Boon> ALL = List.of(
