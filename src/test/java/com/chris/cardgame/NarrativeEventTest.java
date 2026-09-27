package com.chris.cardgame;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.List;

import com.chris.cardgame.data.CardLoader;
import com.chris.cardgame.data.CompanionLoader;
import com.chris.cardgame.data.EventLoader;
import com.chris.cardgame.data.RelicLoader;
import com.chris.cardgame.model.EventDef;
import com.chris.cardgame.model.EventDef.EventChoice;
import com.chris.cardgame.model.EventDef.EventCost;
import com.chris.cardgame.model.EventDef.EventEffect;
import com.chris.cardgame.model.HeroClass;
import com.chris.cardgame.run.Events;
import com.chris.cardgame.run.RunState;
import org.junit.jupiter.api.Test;

class NarrativeEventTest {
    private final PrintStream silent = new PrintStream(OutputStream.nullOutputStream());
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
    void twentyEightEventsLoadWithChoices() {
        assertThat(events.all()).hasSize(28);
        assertThat(events.all()).allSatisfy(event -> {
            assertThat(event.title()).isNotBlank();
            assertThat(event.choices().size()).isGreaterThanOrEqualTo(2);
            assertThat(event.choices()).allSatisfy(choice ->
                    assertThat(choice.text()).isNotBlank());
        });
    }

    @Test
    void pickAvoidsRepeatsThenResets() {
        Events engine = engine();
        RunState state = state(1L);

        for (int i = 0; i < 28; i++) {
            engine.pick(state);
        }
        assertThat(state.seenEvents()).hasSize(28);
        engine.pick(state);
        assertThat(state.seenEvents()).hasSize(1);
    }

    @Test
    void chainedEventsUnlockAfterPrerequisite() {
        Events engine = engine();
        RunState fresh = state(9L);

        assertThat(engine.pick(fresh).requiresSeen()).isNull();

        RunState primed = state(9L);
        events.all().stream()
                .filter(event -> event.requiresSeen() == null)
                .map(EventDef::id)
                .forEach(id -> primed.seenEvents().add(id));
        assertThat(engine.pick(primed).id())
                .isIn("stowaway-returns", "tollkeepers-ledger");
    }

    @Test
    void chooseRespectsCosts() {
        Events engine = engine();
        EventDef toll = events.get("toll");

        RunState broke = state(2L);
        broke.spendGold(50);
        assertThat(engine.choose(toll, broke).id()).isEqualTo("fight");

        RunState rich = state(2L);
        rich.addGold(200);
        assertThat(engine.choose(toll, rich).id()).isEqualTo("pay");
    }

    @Test
    void chooseAvoidsLethal() {
        Events engine = engine();
        EventDef gauntlet = new EventDef("gauntlet", "Gauntlet", "Pain or nothing.",
                List.of(
                        new EventChoice("pain", "Suffer.",
                                new EventCost(0, 0, 0, 0),
                                new EventEffect(0, 0, 0, 0, 50, 0, 0,
                                        false, false, false, false, false)),
                        new EventChoice("pass", "Decline.",
                                new EventCost(0, 0, 0, 0),
                                new EventEffect(0, 0, 0, 5, 0, 0, 0,
                                        false, false, false, false, false))), null);
        RunState frail = state(3L);
        frail.hero().takeDamage(50);

        assertThat(engine.choose(gauntlet, frail).id()).isEqualTo("pass");
    }

    @Test
    void smithDraftTradesGoldForCard() {
        Events engine = engine();
        EventDef smith = events.get("smith");
        EventChoice buy = smith.choices().stream()
                .filter(choice -> choice.id().equals("buy")).findFirst().orElseThrow();
        RunState state = state(4L);

        engine.apply(state, buy, silent);

        assertThat(state.gold()).isEqualTo(10);
        assertThat(state.deck()).hasSize(13);
    }

    @Test
    void curseSlipsIntoDeck() {
        Events engine = engine();
        EventDef memorial = events.get("memorial");
        EventChoice loot = memorial.choices().stream()
                .filter(choice -> choice.id().equals("loot")).findFirst().orElseThrow();
        RunState state = state(5L);

        engine.apply(state, loot, silent);

        assertThat(state.gold()).isEqualTo(100);
        assertThat(state.deck().stream().map(c -> c.id()))
                .containsAnyOf("curse-doubt", "curse-sloth");
    }

    @Test
    void stowawayRecruitsCompanion() {
        Events engine = engine();
        EventDef stowaway = events.get("stowaway");
        EventChoice keep = stowaway.choices().stream()
                .filter(choice -> choice.id().equals("keep")).findFirst().orElseThrow();
        RunState state = state(6L);

        engine.apply(state, keep, silent);

        assertThat(state.companions()).hasSize(1);
    }
}
