package com.chris.cardgame;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;

import com.chris.cardgame.cli.SaveStore;
import com.chris.cardgame.data.CardLoader;
import com.chris.cardgame.model.HeroClass;
import com.chris.cardgame.run.RunState;
import com.chris.cardgame.run.SaveData;
import com.chris.cardgame.web.GameSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SaveDurabilityTest {
    @Test
    void damagedLatestCheckpointRecoversVerifiedPreviousSave(@TempDir Path temp) throws Exception {
        Path path = temp.resolve("save.json");
        RunState state = new RunState("Captain", HeroClass.KNIGHT,
                CardLoader.load().starterDeck(HeroClass.KNIGHT), 42);
        SaveData previous = state.toSave(null);
        SaveStore.save(path, previous);
        state.addGold(25);
        SaveStore.save(path, state.toSave(null));
        Files.writeString(path, "{interrupted");
        assertThat(SaveStore.load(path)).isEqualTo(previous);
        Files.delete(path);
        GameSession session = new GameSession(path);
        assertThat(session.hasSave()).isTrue();
        assertThat(session.continueRun().get("screen")).isEqualTo("map");
        SaveStore.delete(path);
        assertThat(SaveStore.exists(path)).isFalse();
        assertThat(Files.exists(path.resolveSibling("save.json.bak"))).isFalse();
    }
}
