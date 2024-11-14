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
    }

    private int indexOf(CombatState state, String id) {
        for (int i = 0; i < state.hand().size(); i++) {
            if (state.hand().get(i).id().equals(id)) {
                return i;
            }
        }
        throw new IllegalStateException("card not in hand: " + id);
    }

    @Test
    void aoeHitsEveryFoeWithCover() {
        CombatState state = battle(hero(),
                List.of("knight-cleave", "knight-cleave", "knight-cleave", "knight-cleave"),
                List.of("rat", "imp"), 1L);

        engine.playCard(state, indexOf(state, "knight-cleave"), 0);

        assertThat(state.enemies().get(0).hp()).isEqualTo(3);
        assertThat(state.enemies().get(1).hp()).isEqualTo(8);
    }

    @Test
    void cardAspectDrivesAdvantage() {
        CombatState state = battle(hero(),
                List.of("neutral-shiv", "neutral-shiv", "knight-strike", "knight-strike"),
                List.of("imp"), 1L);

        engine.playCard(state, indexOf(state, "neutral-shiv"), 0);

        assertThat(state.enemies().get(0).hp()).isEqualTo(9);
    }

    @Test
    void multiHitSplitsAcrossHits() {
        CombatState state = battle(hero(),
                List.of("ranger-double-tap", "ranger-double-tap", "knight-strike", "knight-strike"),
                List.of("pirate"), 1L);

        engine.playCard(state, indexOf(state, "ranger-double-tap"), 0);

        assertThat(state.enemies().get(0).hp()).isEqualTo(20);
    }

    @Test
    void strengthAppliesPerHit() {
        Combatant hero = hero();
        hero.gainBaseStrength(2);
        CombatState state = battle(hero,
                List.of("ranger-double-tap", "knight-strike", "knight-strike", "knight-strike"),
                List.of("rat"), 1L);

        engine.playCard(state, indexOf(state, "ranger-double-tap"), 0);

        assertThat(state.enemies().get(0).hp()).isEqualTo(4);
    }

    @Test
    void energyCardsRefundEnergy() {
        CombatState state = battle(hero(),
                List.of("runemage-channel", "knight-strike", "knight-strike", "knight-strike",
                        "knight-guard"),
                List.of("rat"), 1L);

        engine.playCard(state, indexOf(state, "runemage-channel"), 0);

        assertThat(state.energy()).isEqualTo(3);
        assertThat(state.hand()).hasSize(4);
    }

    @Test
    void overchargeSpikesEnergy() {
        CombatState state = battle(hero(),
                List.of("runemage-overcharge", "knight-strike", "knight-strike", "knight-strike"),
                List.of("rat"), 1L);

        engine.playCard(state, indexOf(state, "runemage-overcharge"), 0);

        assertThat(state.energy()).isEqualTo(5);
    }

    @Test
    void platingBlocksTurnOne() {
        Combatant hero = hero();
        hero.gainPlating(5);
        CombatState state = battle(hero,
                List.of("knight-strike", "knight-strike", "knight-strike", "knight-strike"),
                List.of("rat"), 1L);

        assertThat(state.hero().block()).isEqualTo(5);
    }

    @Test
    void firstTurnRelicStatsApply() {
        Combatant hero = hero();
        hero.gainFirstTurnEnergy(1);
        hero.gainFirstTurnDraw(2);
        CombatState state = battle(hero,
                List.of("knight-strike", "knight-strike", "knight-strike", "knight-strike",
                        "knight-strike", "knight-strike"),
                List.of("rat"), 1L);
