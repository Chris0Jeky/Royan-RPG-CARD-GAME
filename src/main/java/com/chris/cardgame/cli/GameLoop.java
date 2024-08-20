package com.chris.cardgame.cli;

import java.io.PrintStream;
import java.util.Comparator;
import java.util.List;

import com.chris.cardgame.combat.CombatEngine;
import com.chris.cardgame.combat.CombatState;
import com.chris.cardgame.model.CardDef;
import com.chris.cardgame.model.CardType;
import com.chris.cardgame.model.Combatant;
import com.chris.cardgame.model.EnemyDef;
import com.chris.cardgame.model.Row;

public class GameLoop {
    private final CombatEngine engine = new CombatEngine();

    public record BattleResult(boolean victory, int turns, int heroHp, int enemiesSlain, int totalEnemies) {
    }

    public BattleResult runAutoBattle(Combatant hero, List<CardDef> deck, List<EnemyDef> enemies,
            long seed, int maxTurns, PrintStream out) {
        CombatState state = engine.newBattle(hero, List.of(), deck, enemies, seed);
        out.println("=== Battle: " + hero.name() + " vs " + enemies.size() + " foes (seed " + seed + ") ===");
        while (!state.over() && state.turn() <= maxTurns) {
            describe(state, out);
            autoTurn(state, out);
