package com.chris.cardgame;

import static org.assertj.core.api.Assertions.assertThat;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.SplittableRandom;

import com.chris.cardgame.data.CardLoader;
import com.chris.cardgame.data.CompanionLoader;
import com.chris.cardgame.data.EnemyLoader;
import com.chris.cardgame.data.EventLoader;
import com.chris.cardgame.data.RelicLoader;
import com.chris.cardgame.model.EnemyDef;
import com.chris.cardgame.run.Skirmish;
import com.chris.cardgame.model.EventDef;
import com.chris.cardgame.model.HeroClass;
import com.chris.cardgame.run.Events;
import com.chris.cardgame.run.RunState;
import org.junit.jupiter.api.Test;

class ContentV3Test {
    private final CardLoader cards = CardLoader.load();
    private final EventLoader events = EventLoader.load();
    private final RelicLoader relics = RelicLoader.load();
    private final CompanionLoader companions = CompanionLoader.load();

    private RunState state(long seed) {
        return new RunState("Captain", HeroClass.KNIGHT, cards.starterDeck(HeroClass.KNIGHT), seed);
    }

    private Events engine() {
        return new Events(events, cards, relics, companions);
    }

    @Test
    void chainsThreeAndFourLoadWithGating() {
        assertThat(events.all()).hasSize(32);
        assertThat(events.get("ledger-heir").requiresSeen()).isEqualTo("tollkeepers-price");
        assertThat(events.get("ledger-settled").requiresSeen()).isEqualTo("ledger-heir");
        assertThat(events.get("kid-captain").requiresSeen()).isEqualTo("stowaway-farewell");
        assertThat(events.get("kid-flag").requiresSeen()).isEqualTo("kid-captain");
        for (String id : new String[]{"ledger-heir", "ledger-settled", "kid-captain", "kid-flag"}) {
            EventDef event = events.get(id);
            assertThat(event.title()).isNotBlank();
            assertThat(event.text()).isNotBlank();
            assertThat(event.choices().size()).isGreaterThanOrEqualTo(2);
        }
    }

    @Test
    void chainThreeUnlocksOnlyAfterPricePaid() {
        Events engine = engine();

        RunState noPrice = state(11L);
        events.all().stream()
                .filter(event -> event.requiresSeen() == null)
                .map(EventDef::id)
                .forEach(id -> noPrice.seenEvents().add(id));
        noPrice.seenEvents().add("tollkeepers-ledger");
        assertThat(engine.pick(noPrice).id()).isIn("stowaway-returns", "tollkeepers-price");

        RunState primed = state(11L);
        events.all().stream()
                .filter(event -> event.requiresSeen() == null)
                .map(EventDef::id)
                .forEach(id -> primed.seenEvents().add(id));
        primed.seenEvents().add("tollkeepers-ledger");
        primed.seenEvents().add("tollkeepers-price");
        primed.seenEvents().add("stowaway-returns");
        primed.seenEvents().add("stowaway-farewell");
        primed.seenEvents().add("kid-captain");
        assertThat(engine.pick(primed).id()).isIn("ledger-heir", "kid-flag");
    }

    @Test
    void chainFourUnlocksOnlyAfterFarewell() {
        Events engine = engine();
        RunState primed = state(12L);
        events.all().stream()
                .filter(event -> event.requiresSeen() == null)
                .map(EventDef::id)
                .forEach(id -> primed.seenEvents().add(id));
        primed.seenEvents().add("stowaway-returns");
        primed.seenEvents().add("stowaway-farewell");
        primed.seenEvents().add("tollkeepers-ledger");
        primed.seenEvents().add("tollkeepers-price");
        primed.seenEvents().add("ledger-heir");
        assertThat(engine.pick(primed).id()).isIn("kid-captain", "ledger-settled");
    }

    @Test
    void fiveSkirmishTiersLoadWithPools() {
        Skirmish skirmish = Skirmish.load();

        assertThat(skirmish.tierCount()).isEqualTo(5);
        assertThat(skirmish.tiers().stream().map(t -> t.tier()).toList())
                .containsExactly(1, 2, 3, 4, 5);
        assertThat(skirmish.tiers()).allSatisfy(tier -> {
            assertThat(tier.name()).isNotBlank();
            assertThat(tier.pools().size()).isGreaterThanOrEqualTo(3);
            assertThat(tier.pools()).allSatisfy(pool ->
                    assertThat(pool.size()).isBetween(1, 3));
        });
    }

    @Test
    void skirmishPoolsResolveAndExcludeBosses() {
        Skirmish skirmish = Skirmish.load();
        EnemyLoader enemies = EnemyLoader.load();

        for (int tier = 1; tier <= 5; tier++) {
            for (int roll = 0; roll < 20; roll++) {
                List<EnemyDef> foes = skirmish.encounterFor(
                        tier, enemies, new SplittableRandom(roll * 31L + tier));
                assertThat(foes).isNotEmpty();
                assertThat(foes).allSatisfy(foe ->
                        assertThat(foe.boss()).isFalse());
            }
        }
    }

    @Test
    void skirmishTiersEscalateInHpAndNumbers() {
        Skirmish skirmish = Skirmish.load();
        EnemyLoader enemies = EnemyLoader.load();

        int prevMax = -1;
        for (int tier = 1; tier <= 5; tier++) {
            List<Integer> poolHp = skirmish.tier(tier).pools().stream()
                    .map(pool -> pool.stream().mapToInt(id -> enemies.get(id).hp()).sum())
                    .sorted().toList();
            assertThat(poolHp.get(0)).isGreaterThan(prevMax);
            prevMax = poolHp.get(poolHp.size() - 1);
        }
        assertThat(skirmish.tier(1).pools()).allSatisfy(pool ->
                assertThat(pool.size()).isLessThanOrEqualTo(2));
        assertThat(skirmish.tier(5).pools()).allSatisfy(pool ->
                assertThat(pool.size()).isEqualTo(3));
    }

    @Test
    void skirmishEncounterIsDeterministicAndValidated() {
        Skirmish skirmish = Skirmish.load();
        EnemyLoader enemies = EnemyLoader.load();

        List<String> first = skirmish.encounterFor(3, enemies, new SplittableRandom(7L))
                .stream().map(EnemyDef::id).toList();
        List<String> second = skirmish.encounterFor(3, enemies, new SplittableRandom(7L))
                .stream().map(EnemyDef::id).toList();
        assertThat(first).isEqualTo(second);
        assertThat(skirmish.tier(3).pools()).contains(first);
        assertThatThrownBy(() -> skirmish.encounterFor(0, enemies, new SplittableRandom(1L)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> skirmish.encounterFor(6, enemies, new SplittableRandom(1L)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
