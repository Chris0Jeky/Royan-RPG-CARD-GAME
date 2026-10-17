package com.chris.cardgame.cli;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

import com.chris.cardgame.run.SaveData;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public final class SaveStore {
    public static final int VERSION = 2;

    private SaveStore() {
    }

    public static Path defaultPath() {
        return Path.of(".royan-save", "save.json");
    }

    public static synchronized void save(Path path, SaveData data) {
        save(path, data, SaveStore::writeForced);
    }

    @FunctionalInterface
    interface CheckpointWriter {
        void write(Path path, byte[] bytes) throws IOException;
    }

    static synchronized void save(Path path, SaveData data, CheckpointWriter writer) {
        try {
            path = path.toAbsolutePath();
            Files.createDirectories(path.getParent());
            ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
            validate(data);
            byte[] bytes = mapper.writeValueAsBytes(data);
            // Keep a verified checkpoint until the new file has been forced and installed.
            if (Files.exists(path)) {
                SaveData previous;
                try {
                    previous = load(path);
                } catch (UncheckedIOException | IllegalStateException e) {
                    previous = null;
                }
                if (previous != null) {
                    replace(backup(path), mapper.writeValueAsBytes(previous), writer);
                }
            }
            replace(path, bytes, writer);
        } catch (IOException e) {
            throw new UncheckedIOException("cannot save to " + path, e);
        }
    }

    public static synchronized SaveData load(Path path) {
        try {
            return read(path);
        } catch (IOException | IllegalStateException primary) {
            try {
                return read(backup(path));
            } catch (IOException | IllegalStateException recovery) {
                primary.addSuppressed(recovery);
            }
            if (primary instanceof IOException io) {
                throw new UncheckedIOException("cannot load " + path, io);
            }
            throw (IllegalStateException) primary;
        }
    }

    public static boolean exists(Path path) {
        return Files.exists(path) || Files.exists(backup(path));
    }

    private static SaveData read(Path path) throws IOException {
        try (InputStream in = Files.newInputStream(path)) {
            SaveData data = new ObjectMapper().readValue(in, SaveData.class);
            validate(data);
            return data;
        }
    }

    private static void validate(SaveData data) {
        if (data == null || data.version() < 1 || data.version() > VERSION) {
            throw new IllegalStateException("unsupported save version");
        }
        if (data.heroClass() == null || data.deck() == null || data.relics() == null
                || data.companionIds() == null || data.companionHp() == null
                || data.seenEvents() == null || (data.version() == 2 && data.rngState() == null)) {
            throw new IllegalStateException("incomplete save");
        }
    }

    private static Path backup(Path path) {
        return path.resolveSibling(path.getFileName() + ".bak");
    }

    private static void replace(Path path, byte[] bytes, CheckpointWriter writer) throws IOException {
        Path temporary = Files.createTempFile(path.getParent(), "royan-", ".tmp");
        try {
            writer.write(temporary, bytes);
            // Fail closed if atomic replacement is unsupported; never truncate the checkpoint.
            Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            syncDirectory(path.getParent());
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static void writeForced(Path path, byte[] bytes) throws IOException {
        try (FileChannel file = FileChannel.open(path, StandardOpenOption.WRITE)) {
            ByteBuffer buffer = ByteBuffer.wrap(bytes);
            while (buffer.hasRemaining()) {
                file.write(buffer);
            }
            file.force(true);
        }
    }

    private static void syncDirectory(Path directory) {
        try (FileChannel dir = FileChannel.open(directory, StandardOpenOption.READ)) {
            dir.force(true);
        } catch (IOException | UnsupportedOperationException e) {
            // Windows cannot open directories as channels; retain the forced recovery file.
        }
    }

    public static synchronized void delete(Path path) {
        try {
            Files.deleteIfExists(backup(path));
            Files.deleteIfExists(path);
        } catch (IOException e) {
            throw new UncheckedIOException("cannot delete " + path, e);
        }
    }
}
