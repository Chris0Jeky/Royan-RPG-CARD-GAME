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
                List.of(enemies.get("golem")), battleScript(12), silent);

        assertThat(victory).isFalse();
    }

    @Test
    void manualShopBuysAndLeaves() {
        RunState state = state(HeroClass.KNIGHT, 13L);
        state.addGold(500);
        ScriptedInput in = new ScriptedInput(List.of("card 0", "heal", "leave"));

        new InteractiveLoop().manualShop(state, in, silent);

        assertThat(state.deck()).hasSize(13);
        assertThat(state.gold()).isLessThan(550);
    }

    @Test
    void manualTavernRecruitRemoveMeal() {
        RunState state = state(HeroClass.KNIGHT, 14L);
        state.addGold(500);
        state.addDust(200);
        state.hero().takeDamage(40);
        int before = state.hero().hp();
        ScriptedInput in = new ScriptedInput(List.of("recruit", "remove", "meal", "leave"));

        new InteractiveLoop().manualTavern(state, in, silent);

        assertThat(state.companions()).hasSize(1);
        assertThat(state.deck()).hasSize(11);
