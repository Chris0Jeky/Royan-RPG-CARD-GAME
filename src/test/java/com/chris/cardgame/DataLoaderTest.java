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

        assertThat(cards.all()).hasSize(90);
        assertThat(cards.all()).allSatisfy(card ->
                assertThat(card.cost()).isBetween(0, 3));
        assertThat(cards.all().stream().map(CardDef::id).distinct().count()).isEqualTo(90);
    }

    @Test
    void collectionCountsPerClass() {
        CardLoader cards = CardLoader.load();

        assertThat(cards.all().stream().filter(c -> c.heroClass() == HeroClass.KNIGHT).count()).isEqualTo(24);
        assertThat(cards.all().stream().filter(c -> c.heroClass() == HeroClass.RANGER).count()).isEqualTo(24);
        assertThat(cards.all().stream().filter(c -> c.heroClass() == HeroClass.RUNEMAGE).count()).isEqualTo(24);
        assertThat(cards.all().stream().filter(c -> c.heroClass() == HeroClass.NEUTRAL).count()).isEqualTo(18);
    }

    @Test
    void starterDecksAreTwelveCards() {
        CardLoader cards = CardLoader.load();

        List<CardDef> knight = cards.starterDeck(HeroClass.KNIGHT);
        assertThat(knight).hasSize(12);
        assertThat(knight.stream().filter(c -> c.id().equals("knight-strike")).count()).isEqualTo(3);
        assertThat(knight.stream().filter(c -> c.id().equals("knight-guard")).count()).isEqualTo(4);

        List<CardDef> ranger = cards.starterDeck(HeroClass.RANGER);
        assertThat(ranger).hasSize(12);
        assertThat(ranger.stream().filter(c -> c.id().equals("ranger-dodge")).count()).isEqualTo(4);

        List<CardDef> runemage = cards.starterDeck(HeroClass.RUNEMAGE);
        assertThat(runemage).hasSize(12);
        assertThat(runemage.stream().filter(c -> c.id().equals("runemage-ward")).count()).isEqualTo(4);
    }

    @Test
    void unknownCardThrows() {
        CardLoader cards = CardLoader.load();

        assertThatThrownBy(() -> cards.get("nope"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> cards.starterDeck(HeroClass.NEUTRAL))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void enemiesLoad() {
        EnemyLoader enemies = EnemyLoader.load();

        assertThat(enemies.all()).hasSize(10);
        assertThat(enemies.get("golem").hp()).isEqualTo(36);
        assertThat(enemies.all().stream().filter(e -> e.boss()).count()).isEqualTo(3);
        assertThatThrownBy(() -> enemies.get("nope"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
