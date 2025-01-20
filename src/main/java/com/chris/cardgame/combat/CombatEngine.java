package com.chris.cardgame.combat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import com.chris.cardgame.ai.EnemyAi;
import com.chris.cardgame.model.CardDef;
import com.chris.cardgame.model.Combatant;
import com.chris.cardgame.model.CompanionRole;
import com.chris.cardgame.model.EnemyDef;
import com.chris.cardgame.model.PhaseTwo;
import com.chris.cardgame.model.Row;

public class CombatEngine {
    public static final int ENERGY_PER_TURN = 3;
    public static final int DRAW_PER_TURN = 4;
    public static final int ENEMY_DEFEND_BLOCK = 5;
    public static final int ENEMY_BUFF_STRENGTH = 2;

    public CombatState newBattle(Combatant hero, List<Combatant> companions, List<CardDef> deck,
            List<EnemyDef> enemyDefs, long seed) {
        return newBattle(hero, companions, List.of(), List.of(), deck, enemyDefs, seed);
    }

    public CombatState newBattle(Combatant hero, List<Combatant> companions,
            List<CompanionRole> companionRoles, List<Integer> companionPower, List<CardDef> deck,
            List<EnemyDef> enemyDefs, long seed) {
        List<Combatant> enemies = enemyDefs.stream().map(Combatant::enemy).toList();
        CombatState state = new CombatState(hero, new ArrayList<>(companions),
                List.copyOf(companionRoles), List.copyOf(companionPower),
                new ArrayList<>(enemies), List.copyOf(enemyDefs), seed);
        hero.resetForBattle();
        companions.forEach(Combatant::resetForBattle);
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
        int draws = DRAW_PER_TURN;
        if (state.turn() == 1) {
            state.setEnergy(state.energy() + state.hero().firstTurnEnergy());
            draws += state.hero().firstTurnDraw();
            state.hero().gainBlock(state.hero().plating());
        }
        for (int i = 0; i < draws; i++) {
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
        if (card.targetsEnemy() && !card.aoe()) {
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
        state.setEnergy(state.energy() + card.energy());
        for (int i = 0; i < card.draw(); i++) {
            drawOne(state);
        }
        if (card.targetsEnemy()) {
            List<Combatant> targets = card.aoe() ? aliveEnemies(state) : List.of(target);
            for (Combatant foe : targets) {
                for (int hit = 0; hit < card.hits() && foe.alive(); hit++) {
                    if (card.damage() > 0) {
                        boolean cover = foe.row() == Row.BACK && frontAlive(state);
                        int damage = DamageCalc.attackDamage(card.aspect(), hero.weak(),
                                hero.strength(), foe, card.damage(), cover);
                        foe.takeDamage(damage);
                    }
                }
                if (foe.alive() || card.damage() == 0) {
                    foe.applyWeak(card.weak());
                    foe.applyVulnerable(card.vulnerable());
                }
            }
        }
        state.discardPile().add(card);
        checkEnd(state);
    }

    public void endTurn(CombatState state) {
        requireLive(state);
        state.discardPile().addAll(state.hand());
        state.hand().clear();
        state.enemies().forEach(Combatant::clearBlock);
        companionsAct(state);
        for (int i = 0; i < state.enemies().size(); i++) {
            Combatant enemy = state.enemies().get(i);
            if (!enemy.alive()) {
                continue;
            }
            Intent intent = state.intents().get(i);
            switch (intent.kind()) {
                case ATTACK -> {
                    int damage = DamageCalc.attackDamage(enemy, state.hero(),
                            state.currentAtk().get(i), false);
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

    private void companionsAct(CombatState state) {
        for (int i = 0; i < state.companions().size(); i++) {
            Combatant ally = state.companions().get(i);
            if (!ally.alive()) {
                continue;
            }
            CompanionRole role = state.companionRoles().get(i);
            int power = state.companionPower().get(i);
            switch (role) {
                case STRIKER -> {
                    Combatant target = firstAliveFoe(state);
                    if (target != null) {
                        boolean cover = target.row() == Row.BACK && frontAlive(state);
                        int before = target.hp();
                        int damage = DamageCalc.attackDamage(ally, target, power, cover);
                        target.takeDamage(damage);
                        state.log(ally.name() + " strikes " + target.name() + " for "
                                + (before - target.hp()) + ".");
                    }
                }
                case GUARDIAN -> {
                    state.hero().gainBlock(power);
                    state.log(ally.name() + " shields the Captain (+" + power + " block).");
                }
                case MEDIC -> {
                    Combatant patient = lowestAlly(state);
                    int before = patient.hp();
                    patient.heal(power);
                    state.log(ally.name() + " tends " + patient.name() + " (+"
                            + (patient.hp() - before) + " HP).");
                }
            }
        }
    }

    private Combatant firstAliveFoe(CombatState state) {
        for (Combatant enemy : state.enemies()) {
            if (enemy.alive() && enemy.row() == Row.FRONT) {
                return enemy;
            }
        }
        for (Combatant enemy : state.enemies()) {
            if (enemy.alive()) {
                return enemy;
            }
        }
        return null;
    }

    private Combatant lowestAlly(CombatState state) {
        Combatant lowest = state.hero();
        double lowestFrac = (double) state.hero().hp() / state.hero().maxHp();
        for (Combatant ally : state.companions()) {
            if (!ally.alive()) {
                continue;
            }
            double frac = (double) ally.hp() / ally.maxHp();
            if (frac < lowestFrac) {
                lowestFrac = frac;
                lowest = ally;
            }
        }
        return lowest;
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

    private void rollIntents(CombatState state) {
        EnemyAi ai = new EnemyAi();
        for (int i = 0; i < state.enemies().size(); i++) {
            Combatant enemy = state.enemies().get(i);
            if (!enemy.alive()) {
                continue;
            }
            IntentKind kind = ai.roll(state.currentBehavior().get(i),
                    i, state.aiMemory(), state.rng());
            int preview = switch (kind) {
                case ATTACK -> DamageCalc.attackDamage(enemy, state.hero(),
                        state.currentAtk().get(i), false);
                case DEFEND -> ENEMY_DEFEND_BLOCK;
                case BUFF -> ENEMY_BUFF_STRENGTH;
                case DEBUFF -> 1;
            };
            state.intents().set(i, new Intent(kind, preview));
        }
    }

    private List<Combatant> aliveEnemies(CombatState state) {
        List<Combatant> alive = new ArrayList<>();
        state.enemies().forEach(enemy -> {
            if (enemy.alive()) {
                alive.add(enemy);
            }
        });
        return alive;
    }

    private boolean frontAlive(CombatState state) {
        return state.enemies().stream()
                .anyMatch(enemy -> enemy.alive() && enemy.row() == Row.FRONT);
    }

    private void checkEnd(CombatState state) {
        checkPhaseTransitions(state);
        boolean allDead = state.enemies().stream().noneMatch(Combatant::alive);
        boolean heroDead = !state.hero().alive();
        state.setVictory(allDead && !heroDead);
        state.setOver(allDead || heroDead);
    }

    private void checkPhaseTransitions(CombatState state) {
        for (int i = 0; i < state.enemies().size(); i++) {
            Combatant enemy = state.enemies().get(i);
            PhaseTwo phase = state.enemyDefs().get(i).phaseTwo();
            if (phase == null || state.transitioned().contains(i) || !enemy.alive()) {
                continue;
            }
            if (enemy.hp() * 2 > enemy.maxHp()) {
                continue;
            }
            state.transitioned().add(i);
            state.currentAtk().set(i, phase.atk());
            state.currentBehavior().set(i, phase.behavior());
            enemy.setAspect(phase.aspect());
            enemy.cleanse();
            enemy.heal(phase.heal());
            enemy.gainStrength(phase.strength());
            state.log(enemy.name() + " transforms! " + phase.herald());
        }
    }

    private void requireLive(CombatState state) {
        if (state.over()) {
            throw new IllegalStateException("battle is over");
        }
    }
}
