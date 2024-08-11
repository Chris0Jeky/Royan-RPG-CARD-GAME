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
            throw new IllegalStateException("cannot play " + card.name());
        }
        if (state.energy() < card.cost()) {
            throw new IllegalStateException("not enough energy for " + card.name());
        }
        Combatant target = null;
        if (card.targetsEnemy()) {
            if (targetEnemyIndex < 0 || targetEnemyIndex >= state.enemies().size()) {
                throw new IllegalArgumentException("no such enemy: " + targetEnemyIndex);
            }
            target = state.enemies().get(targetEnemyIndex);
            if (!target.alive()) {
                throw new IllegalArgumentException(target.name() + " is already down");
            }
        }
        state.hand().remove(handIndex);
        state.setEnergy(state.energy() - card.cost());

        Combatant hero = state.hero();
        hero.gainBlock(card.block());
        hero.heal(card.heal());
        hero.gainStrength(card.strength());
        for (int i = 0; i < card.draw(); i++) {
            drawOne(state);
        }
        if (target != null) {
            if (card.damage() > 0) {
                boolean cover = target.row() == Row.BACK && frontAlive(state);
                int damage = DamageCalc.attackDamage(hero, target, card.damage(), cover);
                target.takeDamage(damage);
            }
            target.applyWeak(card.weak());
            target.applyVulnerable(card.vulnerable());
        }
        state.discardPile().add(card);
        checkEnd(state);
    }

    public void endTurn(CombatState state) {
        requireLive(state);
        state.discardPile().addAll(state.hand());
        state.hand().clear();
        state.enemies().forEach(Combatant::clearBlock);
        for (int i = 0; i < state.enemies().size(); i++) {
            Combatant enemy = state.enemies().get(i);
            if (!enemy.alive()) {
                continue;
            }
            EnemyDef def = state.enemyDefs().get(i);
            Intent intent = state.intents().get(i);
            switch (intent.kind()) {
                case ATTACK -> {
                    int damage = DamageCalc.attackDamage(enemy, state.hero(), def.atk(), false);
                    state.hero().takeDamage(damage);
                }
                case DEFEND -> enemy.gainBlock(ENEMY_DEFEND_BLOCK);
                case BUFF -> enemy.gainStrength(ENEMY_BUFF_STRENGTH);
                case DEBUFF -> state.hero().applyWeak(1);
            }
            if (!state.hero().alive()) {
                break;
            }
        }
        state.hero().tickDebuffs();
        state.enemies().forEach(Combatant::tickDebuffs);
        checkEnd(state);
        if (!state.over()) {
            startTurn(state);
        }
    }

    private void drawOne(CombatState state) {
        if (state.drawPile().isEmpty()) {
            if (state.discardPile().isEmpty()) {
                return;
            }
            List<CardDef> pile = new ArrayList<>(state.discardPile());
            state.discardPile().clear();
            Collections.shuffle(pile, new Random(state.rng().nextLong()));
            state.drawPile().addAll(pile);
        }
        state.hand().add(state.drawPile().removeFirst());
    }
