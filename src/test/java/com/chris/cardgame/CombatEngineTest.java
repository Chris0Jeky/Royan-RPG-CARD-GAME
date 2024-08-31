package com.chris.cardgame;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import com.chris.cardgame.combat.CombatEngine;
import com.chris.cardgame.combat.CombatState;
import com.chris.cardgame.combat.DamageCalc;
import com.chris.cardgame.data.CardLoader;
import com.chris.cardgame.data.EnemyLoader;
import com.chris.cardgame.model.Aspect;
import com.chris.cardgame.model.CardDef;
import com.chris.cardgame.model.Combatant;
import com.chris.cardgame.model.EnemyDef;
import com.chris.cardgame.model.HeroClass;
import com.chris.cardgame.model.Row;
import org.junit.jupiter.api.Test;

class CombatEngineTest {
    private final CombatEngine engine = new CombatEngine();
    private final CardLoader cards = CardLoader.load();
    private final EnemyLoader enemies = EnemyLoader.load();

    private Combatant hero() {
        return Combatant.hero("Captain", HeroClass.KNIGHT, 60);
    }

    private List<CardDef> deckOf(String... ids) {
        return java.util.Arrays.stream(ids).map(cards::get).toList();
    }

    @Test
    void newBattleDrawsFourAndRollsIntents() {
        CombatState state = engine.newBattle(hero(), List.of(),
                deckOf("knight-strike", "knight-strike", "knight-guard", "knight-guard"),
                List.of(enemies.get("rat")), 1L);

        assertThat(state.energy()).isEqualTo(3);
        assertThat(state.hand()).hasSize(4);
        assertThat(state.intents()).hasSize(1);
        assertThat(state.intents().get(0)).isNotNull();
    }

    @Test
    void strikeUsesAspectAdvantage() {
        CombatState state = engine.newBattle(hero(), List.of(),
                deckOf("knight-strike", "knight-strike", "knight-strike", "knight-strike"),
                List.of(enemies.get("rat")), 1L);

        engine.playCard(state, 0, 0);

        assertThat(state.enemies().get(0).hp()).isEqualTo(9);
        engine.playCard(state, 0, 0);
        assertThat(state.enemies().get(0).alive()).isFalse();
        assertThat(state.over()).isTrue();
        assertThat(state.victory()).isTrue();
    }

    @Test
    void backRowTakesCoverPenalty() {
        CombatState state = engine.newBattle(hero(), List.of(),
                deckOf("knight-strike", "knight-strike", "knight-strike", "knight-strike"),
                List.of(enemies.get("rat"), enemies.get("imp")), 1L);

        engine.playCard(state, 0, 1);

        assertThat(state.enemies().get(1).hp()).isEqualTo(11);
    }

    @Test
    void guardBlocksAndClearsNextTurn() {
        CombatState state = engine.newBattle(hero(), List.of(),
                deckOf("knight-guard", "knight-guard", "knight-guard", "knight-guard"),
                List.of(enemies.get("rat")), 1L);

        engine.playCard(state, 0, 0);

        assertThat(state.hero().block()).isEqualTo(5);
        engine.endTurn(state);
        assertThat(state.hero().hp()).isEqualTo(60);
        assertThat(state.hero().block()).isEqualTo(0);
    }

    @Test
    void tricksApplyWeakAndVulnerable() {
        CombatState state = engine.newBattle(hero(), List.of(),
                deckOf("neutral-cripple", "neutral-expose", "knight-strike", "knight-strike"),
                List.of(enemies.get("pirate")), 1L);

        int cripple = indexOf(state, "neutral-cripple");
        engine.playCard(state, cripple, 0);
        assertThat(state.enemies().get(0).weak()).isEqualTo(2);
        int expose = indexOf(state, "neutral-expose");
        engine.playCard(state, expose, 0);
        assertThat(state.enemies().get(0).vulnerable()).isEqualTo(2);
    }

    @Test
    void damageCalcMultipliers() {
        Combatant mighty = new Combatant("a", Aspect.MIGHT, Row.FRONT, 50);
        Combatant guile = new Combatant("b", Aspect.GUILE, Row.FRONT, 50);
        Combatant focusBack = new Combatant("c", Aspect.FOCUS, Row.BACK, 50);

        assertThat(DamageCalc.attackDamage(mighty, guile, 10, false)).isEqualTo(15);
        assertThat(DamageCalc.attackDamage(mighty, focusBack, 10, false)).isEqualTo(8);
        assertThat(DamageCalc.attackDamage(mighty, mighty, 10, false)).isEqualTo(10);
        assertThat(DamageCalc.attackDamage(mighty, focusBack, 10, true)).isEqualTo(6);

        mighty.applyWeak(1);
        assertThat(DamageCalc.attackDamage(mighty, guile, 10, false)).isEqualTo(11);
        guile.applyVulnerable(1);
        Combatant fresh = new Combatant("d", Aspect.MIGHT, Row.FRONT, 50);
        assertThat(DamageCalc.attackDamage(fresh, guile, 10, false)).isEqualTo(19);
        fresh.gainStrength(3);
        assertThat(DamageCalc.attackDamage(fresh, new Combatant("e", Aspect.MIGHT, Row.FRONT, 50), 10, false))
                .isEqualTo(13);
    }

    @Test
    void unplayableAndEnergyRules() {
        CombatState state = engine.newBattle(hero(), List.of(),
                deckOf("curse-doubt", "knight-execute", "knight-execute", "knight-strike"),
                List.of(enemies.get("golem")), 1L);

        int curse = indexOf(state, "curse-doubt");
        assertThatThrownBy(() -> engine.playCard(state, curse, 0))
                .isInstanceOf(IllegalStateException.class);
        int execute = indexOf(state, "knight-execute");
        engine.playCard(state, execute, 0);
        int second = indexOf(state, "knight-execute");
        assertThatThrownBy(() -> engine.playCard(state, second, 0))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
