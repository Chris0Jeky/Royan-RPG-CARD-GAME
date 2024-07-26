package com.chris.cardgame.model;

public class Combatant {
    private final String name;
    private final Aspect aspect;
    private final Row row;
    private final int maxHp;
    private int hp;
    private int block;
    private int strength;
    private int weak;
    private int vulnerable;

    public Combatant(String name, Aspect aspect, Row row, int maxHp) {
        this.name = name;
        this.aspect = aspect;
        this.row = row;
        this.maxHp = maxHp;
        this.hp = maxHp;
    }

    public static Combatant hero(String name, HeroClass heroClass, int maxHp) {
