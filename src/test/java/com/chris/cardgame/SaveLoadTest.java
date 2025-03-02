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
