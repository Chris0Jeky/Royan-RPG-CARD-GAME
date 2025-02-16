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
