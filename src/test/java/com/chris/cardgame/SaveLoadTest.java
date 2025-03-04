package com.chris.cardgame;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.chris.cardgame.cli.SaveStore;
import com.chris.cardgame.data.CardLoader;
import com.chris.cardgame.data.CompanionLoader;
import com.chris.cardgame.data.RelicLoader;
import com.chris.cardgame.model.HeroClass;
import com.chris.cardgame.run.Companion;
import com.chris.cardgame.run.RunState;
import com.chris.cardgame.run.SaveData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SaveLoadTest {
    private final CardLoader cards = CardLoader.load();
    private final RelicLoader relics = RelicLoader.load();
    private final CompanionLoader companions = CompanionLoader.load();

    @Test
    void roundtripPreservesEverything(@TempDir Path temp) {
        RunState state = new RunState("Captain", HeroClass.RUNEMAGE,
                cards.starterDeck(HeroClass.RUNEMAGE), 77L);
        state.setAct(2);
        state.addGold(120);
        state.addDust(35);
        state.addShards(2);
        state.addXp(100);
        state.addRelic(relics.get("whetstone"));
        state.addRelic(relics.get("plank-shield"));
        Companion pip = new Companion(companions.get("pip"));
        pip.setHp(17);
        state.recruit(pip);
        state.hero().takeDamage(20);
        state.seenEvents().add("cache");
        Path save = temp.resolve("save.json");

        SaveStore.save(save, state.toSave("a2-L3-1"));
        assertThat(Files.exists(save)).isTrue();

        SaveData loaded = SaveStore.load(save);
        RunState restored = RunState.fromSave(loaded, cards, relics, companions);

        assertThat(restored.toSave("a2-L3-1")).isEqualTo(state.toSave("a2-L3-1"));
        assertThat(restored.companions()).hasSize(1);
        assertThat(restored.companions().get(0).hp()).isEqualTo(17);
