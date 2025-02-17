package com.chris.cardgame.cli;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.chris.cardgame.run.SaveData;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public final class SaveStore {
    public static final int VERSION = 1;

    private SaveStore() {
    }

    public static Path defaultPath() {
        return Path.of(".royan-save", "save.json");
    }

    public static void save(Path path, SaveData data) {
        try {
            Files.createDirectories(path.getParent());
            ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
            mapper.writeValue(path.toFile(), data);
        } catch (IOException e) {
            throw new UncheckedIOException("cannot save to " + path, e);
        }
    }

    public static SaveData load(Path path) {
        try {
            SaveData data = new ObjectMapper().readValue(path.toFile(), SaveData.class);
            if (data.version() != VERSION) {
                throw new IllegalStateException("unsupported save version: " + data.version());
            }
            return data;
        } catch (IOException e) {
            throw new UncheckedIOException("cannot load " + path, e);
        }
    }

    public static void delete(Path path) {
        try {
            Files.deleteIfExists(path);
