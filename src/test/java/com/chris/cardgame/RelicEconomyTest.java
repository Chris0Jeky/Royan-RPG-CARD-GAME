package com.chris.cardgame;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SplittableRandom;

import com.chris.cardgame.data.CardLoader;
import com.chris.cardgame.data.EnemyLoader;
import com.chris.cardgame.data.RelicLoader;
import com.chris.cardgame.map.MapNode;
import com.chris.cardgame.map.NodeType;
import com.chris.cardgame.model.CardDef;
import com.chris.cardgame.model.HeroClass;
import com.chris.cardgame.model.RelicDef;
import com.chris.cardgame.run.RunEngine;
import com.chris.cardgame.run.RunState;
import org.junit.jupiter.api.Test;

class RelicEconomyTest {
    private final PrintStream silent = new PrintStream(OutputStream.nullOutputStream());
    private final CardLoader cards = CardLoader.load();
    private final EnemyLoader enemies = EnemyLoader.load();
    private final RelicLoader relics = RelicLoader.load();

    private RunState state(long seed) {
        return new RunState("Captain", HeroClass.KNIGHT, cards.starterDeck(HeroClass.KNIGHT), seed);
    }

    @Test
    void relicsLoadDistinctWithPositiveValues() {
        assertThat(relics.all()).hasSize(20);
        assertThat(relics.all().stream().map(RelicDef::id).distinct().count()).isEqualTo(20);
        assertThat(relics.all()).allSatisfy(relic -> assertThat(relic.value()).isPositive());
    }

    @Test
    void offerSkipsOwnedAndExhausts() {
        Set<String> owned = new HashSet<>();
        SplittableRandom rng = new SplittableRandom(11L);

        for (int i = 0; i < 20; i++) {
            var offered = relics.offer(owned, false, rng);
            assertThat(offered).isPresent();
            assertThat(owned).doesNotContain(offered.get().id());
            owned.add(offered.get().id());
        }
        assertThat(relics.offer(owned, false, rng)).isEmpty();
    }

    @Test
    void relicPickupAppliesStats() {
        RunState state = state(1L);
        int maxHp = state.hero().maxHp();

