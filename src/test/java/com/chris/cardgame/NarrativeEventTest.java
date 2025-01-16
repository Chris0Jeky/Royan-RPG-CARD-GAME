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
    void twentyFourEventsLoadWithChoices() {
        assertThat(events.all()).hasSize(24);
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

        for (int i = 0; i < 24; i++) {
            engine.pick(state);
        }
        assertThat(state.seenEvents()).hasSize(24);
        engine.pick(state);
        assertThat(state.seenEvents()).hasSize(1);
    }

    @Test
    void chooseRespectsCosts() {
        Events engine = engine();
        EventDef toll = events.get("toll");
