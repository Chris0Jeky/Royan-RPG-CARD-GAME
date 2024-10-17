package com.chris.cardgame;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Map;

import com.chris.cardgame.data.CardLoader;
import com.chris.cardgame.data.EnemyLoader;
import com.chris.cardgame.map.ActMap;
import com.chris.cardgame.map.MapNode;
import com.chris.cardgame.map.NodeType;
import com.chris.cardgame.model.HeroClass;
import com.chris.cardgame.run.RunEngine;
import com.chris.cardgame.run.RunState;
import org.junit.jupiter.api.Test;

class RunEngineTest {
    private final PrintStream silent = new PrintStream(OutputStream.nullOutputStream());
    private final CardLoader cards = CardLoader.load();
    private final EnemyLoader enemies = EnemyLoader.load();

    private RunState state(long seed) {
        return new RunState("Captain", HeroClass.KNIGHT, cards.starterDeck(HeroClass.KNIGHT), seed);
    }

    @Test
    void restHealsThirtyFivePercent() {
        RunEngine engine = new RunEngine(cards, enemies);
        RunState state = state(1L);
        state.hero().takeDamage(50);
        int before = state.hero().hp();

        boolean survived = engine.resolve(state,
                new MapNode("rest", 1, 2, NodeType.REST, List.of()), silent);

        assertThat(survived).isTrue();
        assertThat(state.hero().hp() - before).isEqualTo(state.hero().maxHp() * 35 / 100);
    }

    @Test
    void combatVictoryGrantsSpoilsAndDraft() {
        RunEngine engine = new RunEngine(cards, enemies);
        boolean won = false;
        for (long seed = 1; seed <= 30 && !won; seed++) {
            RunState state = state(seed);
            won = engine.resolve(state, new MapNode("fight", 1, 0, NodeType.COMBAT, List.of()), silent);
            if (won) {
                assertThat(state.gold()).isGreaterThan(50);
                assertThat(state.xp() + (state.level() - 1) * 1000).isGreaterThan(0);
                assertThat(state.deck()).hasSize(13);
            }
        }
        assertThat(won).isTrue();
    }

    @Test
    void shopAndEventResolveSanely() {
        RunEngine engine = new RunEngine(cards, enemies);
        RunState rich = state(2L);
        rich.addGold(500);
        int deckBefore = rich.deck().size();

        assertThat(engine.resolve(rich, new MapNode("shop", 1, 2, NodeType.SHOP, List.of()), silent)).isTrue();
        assertThat(rich.deck().size()).isGreaterThanOrEqualTo(deckBefore);

        RunState broke = state(3L);
        broke.spendGold(50);
        assertThat(engine.resolve(broke, new MapNode("shop", 1, 2, NodeType.SHOP, List.of()), silent)).isTrue();
        assertThat(broke.deck().size()).isEqualTo(deckBefore);
