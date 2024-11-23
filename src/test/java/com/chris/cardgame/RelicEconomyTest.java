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

        state.addRelic(relics.get("whetstone"));
        state.addRelic(relics.get("plank-shield"));
        state.addRelic(relics.get("iron-rations"));
        state.addRelic(relics.get("lucky-coin"));
        state.addRelic(relics.get("blood-vial"));
        state.addRelic(relics.get("war-drums"));
        state.addRelic(relics.get("swift-boots"));

        assertThat(state.hero().strength()).isEqualTo(1);
        assertThat(state.hero().plating()).isEqualTo(3);
        assertThat(state.hero().maxHp()).isEqualTo(maxHp + 6);
        assertThat(state.goldPctBonus()).isEqualTo(15);
        assertThat(state.healAfterCombat()).isEqualTo(3);
        assertThat(state.hero().firstTurnEnergy()).isEqualTo(1);
        assertThat(state.hero().firstTurnDraw()).isEqualTo(1);
    }

    @Test
    void dustAndShardWallets() {
        RunState state = state(1L);

        state.addDust(60);
        state.addShards(2);
        assertThat(state.spendDust(50)).isTrue();
        assertThat(state.spendDust(50)).isFalse();
        assertThat(state.spendShards(1)).isTrue();
        assertThat(state.spendShards(5)).isFalse();
    }

    @Test
    void removeBasicThinsStarter() {
        RunState state = state(1L);

        assertThat(state.removeBasic()).isTrue();
        assertThat(state.deck()).hasSize(11);
    }

    @Test
    void eliteVictoryGrantsRelicAndShard() {
        RunEngine engine = new RunEngine(cards, enemies, relics);
        boolean won = false;
        for (long seed = 1; seed <= 40 && !won; seed++) {
            RunState state = state(seed);
            won = engine.resolve(state, new MapNode("elite", 1, 2, NodeType.ELITE, List.of()), silent);
            if (won) {
                assertThat(state.relics()).hasSize(1);
                assertThat(state.shards()).isGreaterThanOrEqualTo(1);
            }
        }
        assertThat(won).isTrue();
    }

    @Test
    void honedDeckSalvagesDraftForDust() {
        RunEngine engine = new RunEngine(cards, enemies, relics);
        boolean won = false;
        for (long seed = 1; seed <= 40 && !won; seed++) {
            RunState state = state(seed);
            List<CardDef> extras = cards.all().stream()
                    .filter(card -> card.heroClass() == HeroClass.KNIGHT
                            || card.heroClass() == HeroClass.NEUTRAL)
                    .filter(card -> !card.unplayable())
                    .limit(12).toList();
            extras.forEach(card -> state.deck().add(card));
            won = engine.resolve(state, new MapNode("fight", 1, 0, NodeType.COMBAT, List.of()), silent);
            if (won) {
                assertThat(state.dust()).isGreaterThanOrEqualTo(RunEngine.SALVAGE_DUST);
            }
        }
