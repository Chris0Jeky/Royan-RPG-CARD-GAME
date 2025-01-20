package com.chris.cardgame.model;

public enum HeroClass {
    KNIGHT(Aspect.MIGHT, 90), RANGER(Aspect.GUILE, 80), RUNEMAGE(Aspect.FOCUS, 72),
    NEUTRAL(Aspect.MIGHT, 80);

    private final Aspect aspect;
    private final int startingHp;

    HeroClass(Aspect aspect, int startingHp) {
        this.aspect = aspect;
        this.startingHp = startingHp;
    }

    public Aspect aspect() {
        return aspect;
    }

    public int startingHp() {
        return startingHp;
    }
}
