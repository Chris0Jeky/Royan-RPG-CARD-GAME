package com.chris.cardgame.combat;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.SplittableRandom;

import com.chris.cardgame.model.CardDef;
import com.chris.cardgame.model.Combatant;
import com.chris.cardgame.model.EnemyDef;

public class CombatState {
    private final Combatant hero;
    private final List<Combatant> companions;
    private final List<Combatant> enemies;
    private final List<EnemyDef> enemyDefs;
    private final List<Intent> intents;
    private final Deque<CardDef> drawPile = new ArrayDeque<>();
    private final List<CardDef> hand = new ArrayList<>();
    private final List<CardDef> discardPile = new ArrayList<>();
    private final SplittableRandom rng;
    private int energy;
    private int turn;
    private boolean over;
    private boolean victory;

    CombatState(Combatant hero, List<Combatant> companions, List<Combatant> enemies,
            List<EnemyDef> enemyDefs, long seed) {
        this.hero = hero;
        this.companions = companions;
        this.enemies = enemies;
        this.enemyDefs = enemyDefs;
        this.intents = new ArrayList<>();
        for (int i = 0; i < enemies.size(); i++) {
            this.intents.add(new Intent(IntentKind.ATTACK, 0));
        }
        this.rng = new SplittableRandom(seed);
    }

    public Combatant hero() {
        return hero;
    }

    public List<Combatant> companions() {
        return companions;
    }

    public List<Combatant> enemies() {
        return enemies;
    }

    public List<EnemyDef> enemyDefs() {
        return enemyDefs;
    }

    public List<Intent> intents() {
        return intents;
    }

    public Deque<CardDef> drawPile() {
        return drawPile;
    }

    public List<CardDef> hand() {
        return hand;
    }

    public List<CardDef> discardPile() {
        return discardPile;
    }

    public SplittableRandom rng() {
        return rng;
    }

    public int energy() {
        return energy;
    }

    void setEnergy(int energy) {
        this.energy = energy;
    }

    public int turn() {
        return turn;
    }

    void nextTurn() {
        this.turn++;
    }

    public boolean over() {
        return over;
    }

    void setOver(boolean over) {
        this.over = over;
    }

    public boolean victory() {
        return victory;
    }

    void setVictory(boolean victory) {
        this.victory = victory;
    }
}
