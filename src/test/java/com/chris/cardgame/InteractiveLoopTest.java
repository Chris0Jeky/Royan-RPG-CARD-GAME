package com.chris.cardgame;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.chris.cardgame.cli.InteractiveLoop;
import com.chris.cardgame.cli.ScriptedInput;
import com.chris.cardgame.data.CardLoader;
import com.chris.cardgame.data.EnemyLoader;
import com.chris.cardgame.model.HeroClass;
import com.chris.cardgame.run.RunState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class InteractiveLoopTest {
    private final PrintStream silent = new PrintStream(OutputStream.nullOutputStream());
    private final CardLoader cards = CardLoader.load();
    private final EnemyLoader enemies = EnemyLoader.load();

    private RunState state(HeroClass heroClass, long seed) {
        return new RunState("Captain", heroClass, cards.starterDeck(heroClass), seed);
    }
