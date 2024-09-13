package com.chris.cardgame.loot;

import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;

public record Boon(String id, String name, String desc, int maxHp, int heal, int gold, int strength) {

    public static final List<Boon> ALL = List.of(
            new Boon("juggernaut", "Juggernaut", "+8 max HP, heal 8", 8, 8, 0, 0),
            new Boon("feast", "Feast", "Heal 20 HP", 0, 20, 0, 0),
            new Boon("sponsor", "Sponsor", "+60 gold", 0, 0, 60, 0),
            new Boon("second-wind-blessing", "Second Wind", "+4 max HP, heal 4, +20 gold", 4, 4, 20, 0),
            new Boon("war-paint", "War Paint", "+2 strength every battle", 0, 0, 0, 2));

    public static List<Boon> offer(SplittableRandom rng) {
        List<Boon> remaining = new ArrayList<>(ALL);
        List<Boon> options = new ArrayList<>();
        for (int i = 0; i < 3 && !remaining.isEmpty(); i++) {
            options.add(remaining.remove(rng.nextInt(remaining.size())));
        }
