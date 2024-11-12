package com.chris.cardgame;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import com.chris.cardgame.combat.CombatEngine;
import com.chris.cardgame.combat.CombatState;
import com.chris.cardgame.data.CardLoader;
import com.chris.cardgame.data.EnemyLoader;
import com.chris.cardgame.model.CardDef;
import com.chris.cardgame.model.Combatant;
import com.chris.cardgame.model.HeroClass;
import org.junit.jupiter.api.Test;

class BattleArtsTest {
    private final CombatEngine engine = new CombatEngine();
    private final CardLoader cards = CardLoader.load();
    private final EnemyLoader enemies = EnemyLoader.load();

    private Combatant hero() {
        return Combatant.hero("Captain", HeroClass.KNIGHT, 60);
    }

    private CombatState battle(Combatant hero, List<String> deck, List<String> foes, long seed) {
        List<CardDef> cards = deck.stream().map(this.cards::get).toList();
        return engine.newBattle(hero, List.of(), cards,
                foes.stream().map(enemies::get).toList(), seed);
