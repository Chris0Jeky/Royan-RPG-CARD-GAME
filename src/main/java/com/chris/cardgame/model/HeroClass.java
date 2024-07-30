package com.chris.cardgame.model;

public enum HeroClass {
    KNIGHT(Aspect.MIGHT), RANGER(Aspect.GUILE), RUNEMAGE(Aspect.FOCUS), NEUTRAL(Aspect.MIGHT);

    private final Aspect aspect;

    HeroClass(Aspect aspect) {
        this.aspect = aspect;
    }

    public Aspect aspect() {
