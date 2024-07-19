package com.chris.cardgame;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;

class SmokeTest {

    @Test
    void declarationBuildsTwoPlayersWithDecks() {
        Declaration declaration = new Declaration();

        assertThat(declaration.getPlayers()).hasSize(2);
        assertThat(declaration.getCards()).hasSizeGreaterThanOrEqualTo(2);
        assertThat(declaration.getPlayers())
                .allSatisfy(player -> assertThat(player.getDeck().size()).isEqualTo(2));
    }

    @Test
