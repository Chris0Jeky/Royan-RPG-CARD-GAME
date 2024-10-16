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
