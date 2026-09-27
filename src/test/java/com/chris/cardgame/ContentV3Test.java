package com.chris.cardgame;

import static org.assertj.core.api.Assertions.assertThat;

import com.chris.cardgame.data.CardLoader;
import com.chris.cardgame.data.CompanionLoader;
import com.chris.cardgame.data.EventLoader;
import com.chris.cardgame.data.RelicLoader;
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
}
