package com.chris.cardgame;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import com.chris.cardgame.combat.CombatEngine;
import com.chris.cardgame.combat.CombatState;
import com.chris.cardgame.data.CardLoader;
import com.chris.cardgame.data.EnemyLoader;
import com.chris.cardgame.model.Behavior;
import com.chris.cardgame.model.CardDef;
import com.chris.cardgame.model.Combatant;
import com.chris.cardgame.model.HeroClass;
import org.junit.jupiter.api.Test;

class BossPhaseTest {
    private final CombatEngine engine = new CombatEngine();
    private final CardLoader cards = CardLoader.load();
    private final EnemyLoader enemies = EnemyLoader.load();

    private CombatState battle(String bossId, int heroHp) {
        List<CardDef> deck = List.of(
                cards.get("knight-execute"), cards.get("knight-execute"),
                cards.get("knight-execute"), cards.get("knight-execute"));
        return engine.newBattle(Combatant.hero("Captain", HeroClass.KNIGHT, heroHp), List.of(),
                deck, List.of(enemies.get(bossId)), 1L);
    }

    private void playExecute(CombatState state) {
        for (int i = 0; i < state.hand().size(); i++) {
            if (state.hand().get(i).id().equals("knight-execute") && state.energy() >= 3) {
                engine.playCard(state, i, 0);
                return;
            }
        }
        throw new IllegalStateException("no playable execute");
    }

    @Test
    void maraTransformsAtHalfHp() {
        CombatState state = battle("mara", 200);

        playExecute(state);
        assertThat(state.transitioned()).isEmpty();
        engine.endTurn(state);
        playExecute(state);

        assertThat(state.transitioned()).containsExactly(0);
        assertThat(state.currentAtk().get(0)).isEqualTo(12);
        assertThat(state.currentBehavior().get(0)).isEqualTo(Behavior.AGGRO);
        assertThat(state.enemies().get(0).hp()).isEqualTo(28);
        assertThat(state.enemies().get(0).strength()).isEqualTo(2);
