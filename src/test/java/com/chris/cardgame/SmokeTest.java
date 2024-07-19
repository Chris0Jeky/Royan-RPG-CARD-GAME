package com.chris.cardgame;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;

class SmokeTest {

    @Test
    void declarationBuildsTwoPlayersWithDecks() {
        Declaration declaration = new Declaration();

        assertThat(declaration.getPlayers()).hasSize(2);
