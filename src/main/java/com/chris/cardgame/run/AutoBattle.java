package com.chris.cardgame.run;

import java.io.PrintStream;
import java.util.List;

import com.chris.cardgame.combat.CombatEngine;
import com.chris.cardgame.combat.CombatState;
import com.chris.cardgame.model.CardDef;
import com.chris.cardgame.model.Combatant;
import com.chris.cardgame.model.CompanionRole;
import com.chris.cardgame.model.EnemyDef;
import com.chris.cardgame.model.Row;

/**
 * Greedy auto-pilot for scripted battles. Lives in {@code run} (not
 * {@code cli}) so run resolution, balance probes, and UI shells can drive
 * battles without the engine importing any UI package.
 */
public final class AutoBattle {
    private AutoBattle() {
    }

    public record Result(boolean victory, int turns, int heroHp, int enemiesSlain,
            int totalEnemies, List<Integer> companionHp) {
    }

    public static Result run(Combatant hero, List<Companion> companions,
            List<CardDef> deck, List<EnemyDef> enemies, long seed, int maxTurns,
            PrintStream out) {
        CombatEngine engine = new CombatEngine();
        List<Combatant> fighters = companions.stream().map(Companion::toCombatant).toList();
        List<CompanionRole> roles = companions.stream()
                .map(companion -> companion.def().role()).toList();
        List<Integer> powers = companions.stream()
                .map(companion -> companion.def().power()).toList();
        CombatState state = engine.newBattle(hero, fighters, roles, powers, deck, enemies, seed);
        out.println("=== Battle: " + hero.name() + " + " + fighters.size() + " allies vs "
                + enemies.size() + " foes (seed " + seed + ") ===");
        while (!state.over() && state.turn() <= maxTurns) {
            describe(state, out);
            autoTurn(engine, state, out);
            if (!state.over()) {
                engine.endTurn(state);
            }
        }
        drainEvents(state, out);
        int slain = (int) state.enemies().stream().filter(e -> !e.alive()).count();
        List<Integer> companionHp = state.companions().stream().map(Combatant::hp).toList();
        Result result = new Result(state.victory(), state.turn(), state.hero().hp(),
                slain, state.enemies().size(), companionHp);
        out.println(result.victory() ? ">>> VICTORY in " + result.turns() + " turns"
                : ">>> DEFEAT after " + result.turns() + " turns");
        return result;
    }

    private static void autoTurn(CombatEngine engine, CombatState state, PrintStream out) {
        while (true) {
            int pick = pickCard(state);
            if (pick < 0) {
                return;
            }
            CardDef card = state.hand().get(pick);
            int target = firstAliveTarget(state);
            engine.playCard(state, pick, target);
            out.println("  plays " + card.name() + " (" + card.cost() + " energy)");
            if (state.over()) {
                return;
            }
        }
    }

    private static int pickCard(CombatState state) {
        List<CardDef> hand = state.hand();
        int best = -1;
        int bestScore = Integer.MIN_VALUE;
        for (int i = 0; i < hand.size(); i++) {
            CardDef card = hand.get(i);
            if (card.unplayable() || card.cost() > state.energy()) {
                continue;
            }
            int score = score(card, state);
            if (score > bestScore) {
                bestScore = score;
                best = i;
            }
        }
        return best;
    }

    private static int score(CardDef card, CombatState state) {
        boolean hurt = state.hero().hp() <= state.hero().maxHp() * 3 / 5;
        long foes = state.enemies().stream().filter(Combatant::alive).count();
        int targets = card.aoe() ? (int) Math.max(1, foes) : 1;
        int damage = card.damage() * card.hits() * targets;
        return switch (card.type()) {
            case GUARD -> hurt ? 100 + card.block() : 20 + card.block();
            case STRIKE -> 60 + damage;
            case TRICK -> 50 + damage + card.draw() * 5 + (card.weak() + card.vulnerable()) * 4 * targets
                    + card.heal() * (hurt ? 8 : 2) + card.energy() * 15;
            case POWER -> 40 + card.strength() * 10 + card.energy() * 15 + card.draw() * 5;
            case CURSE -> Integer.MIN_VALUE;
        };
    }

    private static int firstAliveTarget(CombatState state) {
        List<Combatant> enemies = state.enemies();
        for (int i = 0; i < enemies.size(); i++) {
            if (enemies.get(i).alive() && enemies.get(i).row() == Row.FRONT) {
                return i;
            }
        }
        for (int i = 0; i < enemies.size(); i++) {
            if (enemies.get(i).alive()) {
                return i;
            }
        }
        return 0;
    }

    private static void drainEvents(CombatState state, PrintStream out) {
        state.events().forEach(event -> out.println("  !! " + event));
        state.events().clear();
    }

    private static void describe(CombatState state, PrintStream out) {
        drainEvents(state, out);
        out.println("-- turn " + state.turn() + " | energy " + state.energy()
                + " | hero " + state.hero() + " --");
        for (int i = 0; i < state.companions().size(); i++) {
            out.println("  ally " + i + ": " + state.companions().get(i));
        }
        for (int i = 0; i < state.enemies().size(); i++) {
            Combatant enemy = state.enemies().get(i);
            String intent = enemy.alive() ? " [" + state.intents().get(i) + "]" : " [DOWN]";
            out.println("  foe " + i + ": " + enemy + intent);
        }
        StringBuilder hand = new StringBuilder("  hand:");
        for (int i = 0; i < state.hand().size(); i++) {
            CardDef card = state.hand().get(i);
            hand.append(" [").append(i).append("] ").append(card.name())
                    .append("(").append(card.cost()).append(")");
        }
        out.println(hand);
    }
}
