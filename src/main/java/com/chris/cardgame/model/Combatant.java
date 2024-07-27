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
        return new Combatant(name, heroClass.aspect(), Row.FRONT, maxHp);
    }

    public static Combatant enemy(EnemyDef def) {
        return new Combatant(def.name(), def.aspect(), def.row(), def.hp());
    }

    public String name() {
        return name;
    }

    public Aspect aspect() {
        return aspect;
    }

    public Row row() {
        return row;
    }

    public int hp() {
        return hp;
    }

    public int maxHp() {
        return maxHp;
    }

    public int block() {
        return block;
    }

    public int strength() {
        return strength;
    }

    public int weak() {
        return weak;
    }

    public int vulnerable() {
        return vulnerable;
