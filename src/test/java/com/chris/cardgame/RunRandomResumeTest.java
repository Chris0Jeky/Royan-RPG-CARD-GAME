package com.chris.cardgame;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.nio.file.Files;
import java.util.Set;
import java.util.SplittableRandom;

import com.chris.cardgame.cli.SaveStore;
import com.chris.cardgame.data.CardLoader;
import com.chris.cardgame.data.RelicLoader;
import com.chris.cardgame.data.CompanionLoader;
import com.chris.cardgame.data.EnemyLoader;
import com.chris.cardgame.loot.EncounterGen;
import com.chris.cardgame.loot.LootGen;
import com.chris.cardgame.loot.Boon;
import com.chris.cardgame.model.HeroClass;
import com.chris.cardgame.run.RunState;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RunRandomResumeTest {
    @Test
    void legacySchemaStillLoadsWithItsOriginalSeedBehavior(@TempDir Path temp) throws Exception {
        CardLoader cards = CardLoader.load();
        RunState state = new RunState("Captain", HeroClass.KNIGHT, cards.starterDeck(HeroClass.KNIGHT), 77);
        ObjectMapper json = new ObjectMapper();
        var legacy = json.valueToTree(state.toSave(null));
        ((com.fasterxml.jackson.databind.node.ObjectNode) legacy).put("version", 1).remove("rngState");
        Path path = temp.resolve("save.json");
        Files.writeString(path, json.writeValueAsString(legacy));
        RunState restored = RunState.fromSave(SaveStore.load(path), cards,
                RelicLoader.load(), CompanionLoader.load());
        assertThat(restored.gold()).isEqualTo(state.gold());
        assertThat(restored.rng().nextLong()).isEqualTo(new SplittableRandom(77).nextLong());
    }

    @Test
    void checkpointsPreserveFutureRollsAndUninterruptedSequence(@TempDir Path temp) {
        CardLoader cards = CardLoader.load();
        RelicLoader relics = RelicLoader.load();
        CompanionLoader companions = CompanionLoader.load();
        EncounterGen encounters = new EncounterGen(EnemyLoader.load());
        LootGen loot = new LootGen(cards);
        RunState control = new RunState("Captain", HeroClass.KNIGHT, cards.starterDeck(HeroClass.KNIGHT), 77);
        SplittableRandom original = new SplittableRandom(77);
        for (int draw = 0; draw < 100; draw++) {
            assertThat(control.rng().nextInt(1073741825)).isEqualTo(original.nextInt(1073741825));
            assertThat(control.rng().nextLong()).isEqualTo(original.nextLong());
            assertThat(control.rng().nextBoolean()).isEqualTo(original.nextBoolean());
        }
        for (int checkpoint = 0; checkpoint < 8; checkpoint++) {
            SaveStore.save(temp.resolve("save.json"), control.toSave(null));
            RunState restored = RunState.fromSave(SaveStore.load(temp.resolve("save.json")), cards, relics, companions);
            assertThat(restored.rng().nextLong()).isEqualTo(control.rng().nextLong());
            assertThat(encounters.combat(2, restored.rng())).isEqualTo(encounters.combat(2, control.rng()));
            assertThat(loot.cardOptions(control.heroClass(), control.deck(), true, restored.rng()))
                    .isEqualTo(loot.cardOptions(control.heroClass(), control.deck(), true, control.rng()));
            assertThat(relics.offer(Set.of(), true, restored.rng())).isEqualTo(relics.offer(Set.of(), true, control.rng()));
            assertThat(Boon.offer(restored.rng())).isEqualTo(Boon.offer(control.rng()));
        }
    }
}
