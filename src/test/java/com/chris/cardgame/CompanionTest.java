package com.chris.cardgame;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.List;

import com.chris.cardgame.combat.CombatEngine;
import com.chris.cardgame.combat.CombatState;
import com.chris.cardgame.data.CardLoader;
import com.chris.cardgame.data.CompanionLoader;
import com.chris.cardgame.data.EnemyLoader;
import com.chris.cardgame.data.EventLoader;
import com.chris.cardgame.data.RelicLoader;
import com.chris.cardgame.map.MapNode;
import com.chris.cardgame.map.NodeType;
import com.chris.cardgame.model.CardDef;
import com.chris.cardgame.model.Combatant;
import com.chris.cardgame.model.CompanionRole;
import com.chris.cardgame.model.HeroClass;
import com.chris.cardgame.run.Companion;
import com.chris.cardgame.run.RunEngine;
import com.chris.cardgame.run.RunState;
import org.junit.jupiter.api.Test;

class CompanionTest {
    private final PrintStream silent = new PrintStream(OutputStream.nullOutputStream());
    private final CombatEngine engine = new CombatEngine();
    private final CardLoader cards = CardLoader.load();
    private final EnemyLoader enemies = EnemyLoader.load();
    private final RelicLoader relics = RelicLoader.load();
    private final CompanionLoader companions = CompanionLoader.load();
    private final EventLoader events = EventLoader.load();

    private CombatState battleWith(String companionId, String foeId) {
        Companion ally = new Companion(companions.get(companionId));
        List<CardDef> deck = List.of(
                cards.get("knight-strike"), cards.get("knight-strike"),
                cards.get("knight-strike"), cards.get("knight-strike"));
        return engine.newBattle(
                Combatant.hero("Captain", HeroClass.KNIGHT, 60),
                List.of(ally.toCombatant()),
                List.of(ally.def().role()),
                List.of(ally.def().power()),
                deck, List.of(enemies.get(foeId)), 1L);
    }

    @Test
    void strikerDamagesFirstFoe() {
        CombatState state = battleWith("pip", "rat");

        engine.endTurn(state);

        assertThat(state.enemies().get(0).hp()).isEqualTo(6);
    }

    @Test
    void guardianSoaksEnemyHit() {
        CombatState state = battleWith("bruma", "rat");

        engine.endTurn(state);

        assertThat(state.hero().hp()).isEqualTo(60);
    }

    @Test
    void medicHealsBeforeEnemyActs() {
        CombatState state = battleWith("wren", "rat");
        state.hero().takeDamage(20);

        engine.endTurn(state);

        assertThat(state.hero().hp()).isIn(42, 46);
    }

    @Test
    void tavernRecruitsUpToTwo() {
        RunEngine engine = new RunEngine(cards, enemies, relics, companions, events);
        RunState state = new RunState("Captain", HeroClass.KNIGHT,
                cards.starterDeck(HeroClass.KNIGHT), 20L);
        state.addGold(500);
        state.addDust(500);

        MapNode tavern = new MapNode("tavern", 1, 2, NodeType.TAVERN, List.of());
        engine.resolve(state, tavern, silent);
        engine.resolve(state, tavern, silent);
        engine.resolve(state, tavern, silent);

        assertThat(state.companions()).hasSize(2);
    }

    @Test
    void companionRestsBetweenBattles() {
        Companion pip = new Companion(companions.get("pip"));
        pip.setHp(10);

        pip.rest();

        assertThat(pip.hp()).isEqualTo(17);
    }

    @Test
    void runCapsCompanionsAtTwo() {
        RunState state = new RunState("Captain", HeroClass.KNIGHT,
                cards.starterDeck(HeroClass.KNIGHT), 21L);

        assertThat(state.recruit(new Companion(companions.get("pip")))).isTrue();
        assertThat(state.recruit(new Companion(companions.get("wren")))).isTrue();
        assertThat(state.recruit(new Companion(companions.get("tess")))).isFalse();
