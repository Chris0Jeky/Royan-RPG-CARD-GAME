package com.chris.cardgame.model;

public class Combatant {
    private final String name;
    private final Aspect aspect;
    private final Row row;
    private int maxHp;
    private int hp;
    private int block;
    private int baseStrength;
    private int strength;
    private int plating;
    private int firstTurnEnergy;
    private int firstTurnDraw;
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
    }

    public boolean alive() {
        return hp > 0;
    }

    public void gainBlock(int amount) {
        if (amount > 0) {
            block += amount;
        }
    }

    public void clearBlock() {
        block = 0;
    }

    public void gainStrength(int amount) {
        strength += amount;
    }

    public void applyWeak(int amount) {
        weak += amount;
    }

    public void applyVulnerable(int amount) {
        vulnerable += amount;
    }

    public void tickDebuffs() {
        if (weak > 0) {
            weak--;
        }
        if (vulnerable > 0) {
            vulnerable--;
        }
    }

    public void cleanse() {
        weak = 0;
        vulnerable = 0;
    }

    public void heal(int amount) {
        hp = Math.min(maxHp, hp + Math.max(0, amount));
    }

    public void resetForBattle() {
        block = 0;
        strength = baseStrength;
        weak = 0;
        vulnerable = 0;
    }

    public void gainBaseStrength(int amount) {
        baseStrength += amount;
        strength += amount;
    }

    public int plating() {
        return plating;
    }

    public void gainPlating(int amount) {
        plating += amount;
    }

    public int firstTurnEnergy() {
        return firstTurnEnergy;
    }

    public void gainFirstTurnEnergy(int amount) {
        firstTurnEnergy += amount;
    }

    public int firstTurnDraw() {
        return firstTurnDraw;
    }

    public void gainFirstTurnDraw(int amount) {
        firstTurnDraw += amount;
    }

    public void raiseMaxHp(int amount) {
        maxHp += amount;
        heal(amount);
    }

    public void takeDamage(int amount) {
        int remaining = Math.max(0, amount);
        if (block > 0 && remaining > 0) {
            int absorbed = Math.min(block, remaining);
            block -= absorbed;
            remaining -= absorbed;
        }
        hp = Math.max(0, hp - remaining);
    }

    @Override
    public String toString() {
        return name + " " + hp + "/" + maxHp + " (block " + block + ")";
    }
}
