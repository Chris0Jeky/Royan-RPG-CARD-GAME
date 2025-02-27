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

    private ScriptedInput battleScript(int turns) {
        List<String> lines = new ArrayList<>();
        for (int i = 0; i < turns; i++) {
            lines.add("play 0 0");
            lines.add("play 0 0");
            lines.add("play 0 0");
            lines.add("play 0 0");
            lines.add("end");
        }
        return new ScriptedInput(lines);
    }

    @Test
    void scriptedShortBattleVictory(@TempDir Path temp) {
        RunState state = state(HeroClass.KNIGHT, 11L);

        boolean victory = new InteractiveLoop().manualBattle(state,
                List.of(enemies.get("rat")), battleScript(8), silent);

        assertThat(victory).isTrue();
        assertThat(state.hero().hp()).isPositive();
    }

    @Test
    void scriptedBattleDefeat() {
        RunState state = state(HeroClass.KNIGHT, 12L);
        state.hero().takeDamage(state.hero().maxHp() - 1);

        boolean victory = new InteractiveLoop().manualBattle(state,
