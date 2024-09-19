package com.chris.cardgame.loot;

public final class XpCurve {
    public static final int MAX_LEVEL = 10;

    private XpCurve() {
    }

    public static int xpForNext(int level) {
        if (level < 1) {
            throw new IllegalArgumentException("level starts at 1");
        }
        if (level >= MAX_LEVEL) {
            return Integer.MAX_VALUE;
        }
        return 20 + level * 12;
