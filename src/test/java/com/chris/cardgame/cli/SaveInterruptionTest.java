package com.chris.cardgame.cli;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.chris.cardgame.data.CardLoader;
import com.chris.cardgame.model.HeroClass;
import com.chris.cardgame.run.RunState;
import com.chris.cardgame.run.SaveData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SaveInterruptionTest {
    @Test
    void interruptedTemporaryWritesNeverReplaceTheCheckpoint(@TempDir Path temp) throws Exception {
        Path path = temp.resolve("save.json");
        RunState state = new RunState("Captain", HeroClass.KNIGHT,
                CardLoader.load().starterDeck(HeroClass.KNIGHT), 42);
        SaveData previous = state.toSave(null);
        SaveStore.save(path, previous);
        state.addGold(25);
        for (int fraction : new int[] {0, 1, 2}) {
            AtomicInteger writes = new AtomicInteger();
            assertThatThrownBy(() -> SaveStore.save(path, state.toSave(null), (scratch, bytes) -> {
                if (writes.incrementAndGet() == 1) {
                    Files.write(scratch, bytes); // Let the backup commit finish first.
                    return;
                }
                Files.write(scratch, Arrays.copyOf(bytes, bytes.length * fraction / 2));
                throw new IOException("injected interruption before install");
            })).isInstanceOf(UncheckedIOException.class);
            assertThat(SaveStore.load(path)).isEqualTo(previous);
            try (var files = Files.list(temp)) {
                assertThat(files.noneMatch(p -> p.toString().endsWith(".tmp"))).isTrue();
            }
        }
    }

    @Test
    void concurrentReadersSeeOnlyCompleteCheckpoints(@TempDir Path temp) throws Exception {
        Path path = temp.resolve("save.json");
        RunState state = new RunState("Captain", HeroClass.KNIGHT,
                CardLoader.load().starterDeck(HeroClass.KNIGHT), 42);
        SaveStore.save(path, state.toSave(null));
        AtomicBoolean writing = new AtomicBoolean(true);
        CountDownLatch firstRead = new CountDownLatch(1);
        var reader = Executors.newSingleThreadExecutor();
        var result = reader.submit(() -> {
            int reads = 0;
            while (writing.get()) {
                SaveData loaded = SaveStore.load(path);
                assertThat(loaded.gold()).isBetween(50, 54);
                assertThat(loaded.deck()).hasSize(12);
                reads++;
                firstRead.countDown();
            }
            return reads;
        });
        try {
            assertThat(firstRead.await(2, TimeUnit.SECONDS)).isTrue();
            for (int i = 0; i < 4; i++) {
                state.addGold(1);
                SaveStore.save(path, state.toSave(null));
            }
        } finally {
            writing.set(false);
            reader.shutdownNow();
        }
        assertThat(result.get()).isGreaterThan(0);
        assertThat(SaveStore.load(path).gold()).isEqualTo(54);
    }
}
