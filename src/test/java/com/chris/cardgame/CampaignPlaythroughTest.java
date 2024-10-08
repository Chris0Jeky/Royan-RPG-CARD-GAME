package com.chris.cardgame;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.List;

import com.chris.cardgame.cli.GameLoop;
import org.junit.jupiter.api.Test;

class CampaignPlaythroughTest {
    private final PrintStream silent = new PrintStream(OutputStream.nullOutputStream());

    @Test
    void campaignsTerminateWithSaneShape() {
        for (long seed : List.of(42L, 7L, 1L, 99L, 5L, 13L, 21L, 1234L, 3L, 11L)) {
            GameLoop.CampaignResult result = new GameLoop().runAutoCampaign(seed, silent);

            assertThat(result.nodesVisited()).isBetween(1, 30);
            assertThat(result.deckSize()).isBetween(12, 30);
