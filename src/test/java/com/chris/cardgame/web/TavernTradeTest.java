package com.chris.cardgame.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.io.OutputStream;
import java.io.PrintStream;

import com.chris.cardgame.data.RelicLoader;
import com.chris.cardgame.data.CardLoader;
import com.chris.cardgame.data.CompanionLoader;
import com.chris.cardgame.run.Companion;
import com.chris.cardgame.run.RunState;
import com.chris.cardgame.run.Tavern;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TavernTradeTest {
    @Test
    void relicTradeChargesOnlyWhenRelicGranted(@TempDir Path temp) throws Exception {
        for (int owned : new int[] {0, 19, 20}) {
            for (int gold : new int[] {99, 100}) {
                for (int shards : new int[] {0, 1}) {
                    WebRun run = WebRun.start("KNIGHT", 42L, temp.resolve("save.json"));
                    Field stateField = WebRun.class.getDeclaredField("state");
                    stateField.setAccessible(true);
                    RunState state = (RunState) stateField.get(run);
                    Field screen = WebRun.class.getDeclaredField("screen");
                    screen.setAccessible(true);
                    screen.set(run, "tavern");
                    RelicLoader.load().all().stream().limit(owned).forEach(state::addRelic);
                    state.addGold(gold - state.gold());
                    state.addShards(shards);
                    CompanionLoader companions = CompanionLoader.load();
                    companions.all().stream().limit(2).map(Companion::new).forEach(state::recruit);
                    RunState cli = RunState.fromSave(state.toSave(null), CardLoader.load(),
                            RelicLoader.load(), companions);
                    long randomState = state.toSave(null).rngState();
                    boolean purchase = owned < 20 && gold == 100 && shards == 1;
                    if (gold < 100 || shards < 1) {
                        assertThatThrownBy(() -> run.tavern("relic")).isInstanceOf(IllegalStateException.class);
                    } else {
                        run.tavern("relic");
                    }
                    assertThat(state.gold()).as("owned=%s gold=%s shards=%s", owned, gold, shards)
                            .isEqualTo(gold - (purchase ? 100 : 0));
                    assertThat(state.shards()).isEqualTo(shards - (purchase ? 1 : 0));
                    assertThat(state.relics()).hasSize(owned + (purchase ? 1 : 0));
                    new Tavern(RelicLoader.load(), companions).visit(cli,
                            new PrintStream(OutputStream.nullOutputStream()));
                    assertThat(state.toSave(null)).isEqualTo(cli.toSave(null));
                    if (!purchase) {
                        assertThat(state.toSave(null).rngState()).isEqualTo(randomState);
                    }
                }
            }
        }
    }
}
