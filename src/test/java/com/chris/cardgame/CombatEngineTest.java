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

