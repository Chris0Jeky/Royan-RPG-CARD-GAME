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
    void deckDrawRemovesTopCard() {
        Deck deck = new Deck();
        Card first = new Card("Card1", "Type1", 10, 20, Card.AdvantageType.A, 1, Card.Rarity.A);
        Card second = new Card("Card2", "Type2", 15, 25, Card.AdvantageType.B, 2, Card.Rarity.B);
        deck.addCard(first);
        deck.addCard(second);

        assertThat(deck.draw()).contains(first);
        assertThat(deck.size()).isEqualTo(1);
    }

    @Test
    void mechanicsStartsGame() {
        assertDoesNotThrow(Mechanics::start_game);
    }

    @Test
    void flowAdvancesThroughPhases() {
        Flow flow = new Flow();

        assertThat(flow.current()).isEqualTo(Flow.Phase.DRAW);
        assertThat(flow.next()).isEqualTo(Flow.Phase.PLAY);
        assertThat(flow.next()).isEqualTo(Flow.Phase.COMBAT);
        assertThat(flow.next()).isEqualTo(Flow.Phase.END);
        assertThat(flow.next()).isEqualTo(Flow.Phase.END);
    }
}
