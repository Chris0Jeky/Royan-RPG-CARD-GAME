package com.chris.cardgame.loot;

public final class XpCurve {
    public static final int MAX_LEVEL = 10;

    private XpCurve() {
    }

    public static int xpForNext(int level) {
        if (level < 1) {
