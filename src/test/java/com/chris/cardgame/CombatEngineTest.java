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
