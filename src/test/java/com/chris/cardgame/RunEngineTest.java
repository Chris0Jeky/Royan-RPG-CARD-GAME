package com.chris.cardgame;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Map;

import com.chris.cardgame.data.CardLoader;
import com.chris.cardgame.data.EnemyLoader;
import com.chris.cardgame.data.RelicLoader;
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
    private final RelicLoader relics = RelicLoader.load();

    private RunState state(long seed) {
        return new RunState("Captain", HeroClass.KNIGHT, cards.starterDeck(HeroClass.KNIGHT), seed);
    }

    private RunEngine engine() {
        return new RunEngine(cards, enemies, relics);
    }

    @Test
    void restHealsThirtyFivePercent() {
        RunEngine engine = engine();
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
        RunEngine engine = engine();
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
        RunEngine engine = engine();
        RunState rich = state(2L);
        rich.addGold(500);
        int deckBefore = rich.deck().size();

        assertThat(engine.resolve(rich, new MapNode("shop", 1, 2, NodeType.SHOP, List.of()), silent)).isTrue();
        assertThat(rich.deck().size()).isGreaterThanOrEqualTo(deckBefore);

        RunState broke = state(3L);
        broke.spendGold(50);
        assertThat(engine.resolve(broke, new MapNode("shop", 1, 2, NodeType.SHOP, List.of()), silent)).isTrue();
        assertThat(broke.deck().size()).isEqualTo(deckBefore);

        RunState curious = state(4L);
        assertThat(engine.resolve(curious, new MapNode("event", 1, 2, NodeType.EVENT, List.of()), silent)).isTrue();
    }

    @Test
    void tavernTradesAndHeals() {
        RunEngine engine = engine();
        RunState guest = state(9L);
        guest.addGold(500);
        guest.addDust(200);
        guest.addShards(2);
        guest.hero().takeDamage(40);
        int hpBefore = guest.hero().hp();

        assertThat(engine.resolve(guest, new MapNode("tavern", 1, 2, NodeType.TAVERN, List.of()), silent)).isTrue();

        assertThat(guest.relics()).hasSize(1);
        assertThat(guest.hero().hp()).isGreaterThan(hpBefore);
    }

    @Test
    void pathPrefersEliteAndRestWhenHurt() {
        RunEngine engine = engine();
        MapNode node = new MapNode("n", 1, 1, NodeType.COMBAT, List.of("c", "e"));
        ActMap map = new ActMap(1, Map.of(
                "n", node,
                "c", new MapNode("c", 1, 2, NodeType.COMBAT, List.of("x")),
                "e", new MapNode("e", 1, 2, NodeType.ELITE, List.of("x")),
                "x", new MapNode("x", 1, 3, NodeType.BOSS, List.of())), List.of("n"), "x");

        assertThat(engine.chooseNext(state(1L), map, node).id()).isEqualTo("e");

        MapNode restChoice = new MapNode("m", 1, 1, NodeType.COMBAT, List.of("c2", "r"));
        ActMap restMap = new ActMap(1, Map.of(
                "m", restChoice,
                "c2", new MapNode("c2", 1, 2, NodeType.COMBAT, List.of("x")),
                "r", new MapNode("r", 1, 2, NodeType.REST, List.of("x")),
                "x", new MapNode("x", 1, 3, NodeType.BOSS, List.of())), List.of("m"), "x");
        RunState hurt = state(1L);
        hurt.hero().takeDamage(70);
        assertThat(engine.chooseNext(hurt, restMap, restChoice).id()).isEqualTo("r");
    }
}
