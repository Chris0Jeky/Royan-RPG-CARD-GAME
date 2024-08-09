package com.chris.cardgame.combat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import com.chris.cardgame.model.CardDef;
import com.chris.cardgame.model.Combatant;
import com.chris.cardgame.model.EnemyDef;
import com.chris.cardgame.model.Row;

public class CombatEngine {
    public static final int ENERGY_PER_TURN = 3;
    public static final int DRAW_PER_TURN = 4;
    public static final int ENEMY_DEFEND_BLOCK = 6;
    public static final int ENEMY_BUFF_STRENGTH = 2;

    public CombatState newBattle(Combatant hero, List<Combatant> companions, List<CardDef> deck,
            List<EnemyDef> enemyDefs, long seed) {
        List<Combatant> enemies = enemyDefs.stream().map(Combatant::enemy).toList();
        CombatState state = new CombatState(hero, new ArrayList<>(companions),
                new ArrayList<>(enemies), List.copyOf(enemyDefs), seed);
        List<CardDef> pile = new ArrayList<>(deck);
        Collections.shuffle(pile, new Random(state.rng().nextLong()));
        state.drawPile().addAll(pile);
        startTurn(state);
        return state;
    }

    public void startTurn(CombatState state) {
        state.nextTurn();
        state.setEnergy(ENERGY_PER_TURN);
        state.hero().clearBlock();
        state.companions().forEach(Combatant::clearBlock);
        for (int i = 0; i < DRAW_PER_TURN; i++) {
            drawOne(state);
        }
        rollIntents(state);
    }

    public void playCard(CombatState state, int handIndex, int targetEnemyIndex) {
        requireLive(state);
        if (handIndex < 0 || handIndex >= state.hand().size()) {
            throw new IllegalArgumentException("no such card in hand: " + handIndex);
        }
        CardDef card = state.hand().get(handIndex);
        if (card.unplayable()) {
