package com.chris.cardgame;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import com.chris.cardgame.data.CardLoader;
import com.chris.cardgame.data.EnemyLoader;
import com.chris.cardgame.model.CardDef;
import com.chris.cardgame.model.HeroClass;
import org.junit.jupiter.api.Test;

class DataLoaderTest {

    @Test
    void cardsLoadWithValidCosts() {
        CardLoader cards = CardLoader.load();

        assertThat(cards.all()).hasSize(18);
        assertThat(cards.all()).allSatisfy(card ->
                assertThat(card.cost()).isBetween(0, 3));
    }

    @Test
    void knightStarterDeckIsTwelveCards() {
        CardLoader cards = CardLoader.load();

        List<CardDef> starter = cards.starterDeck(HeroClass.KNIGHT);

        assertThat(starter).hasSize(12);
        assertThat(starter.stream().filter(c -> c.id().equals("knight-strike")).count()).isEqualTo(4);
        assertThat(starter.stream().filter(c -> c.id().equals("knight-guard")).count()).isEqualTo(3);
        assertThat(starter.stream().filter(c -> c.id().equals("knight-heavy")).count()).isEqualTo(2);
    }

    @Test
    void unknownCardThrows() {
        CardLoader cards = CardLoader.load();

        assertThatThrownBy(() -> cards.get("nope"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> cards.starterDeck(HeroClass.RANGER))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void enemiesLoad() {
        EnemyLoader enemies = EnemyLoader.load();
