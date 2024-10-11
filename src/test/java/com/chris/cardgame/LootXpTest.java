package com.chris.cardgame;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;

import com.chris.cardgame.data.CardLoader;
import com.chris.cardgame.loot.Boon;
import com.chris.cardgame.loot.LootGen;
import com.chris.cardgame.loot.XpCurve;
import com.chris.cardgame.model.CardDef;
import com.chris.cardgame.model.CardType;
import com.chris.cardgame.model.HeroClass;
import com.chris.cardgame.run.RunState;
import org.junit.jupiter.api.Test;

class LootXpTest {
    private final CardLoader cards = CardLoader.load();

    private RunState state(long seed) {
        return new RunState("Captain", HeroClass.KNIGHT, cards.starterDeck(HeroClass.KNIGHT), seed);
    }

    @Test
    void xpCurveThresholds() {
        assertThat(XpCurve.xpForNext(1)).isEqualTo(32);
        assertThat(XpCurve.xpForNext(9)).isEqualTo(128);
        assertThat(XpCurve.xpForNext(10)).isEqualTo(Integer.MAX_VALUE);
    }

    @Test
    void bigXpGrantsMultipleLevelsAndBoons() {
        RunState state = state(1L);

        List<Boon> boons = state.addXp(200);

        assertThat(state.level()).isEqualTo(5);
        assertThat(state.xp()).isEqualTo(0);
        assertThat(boons).hasSize(4);
    }

    @Test
    void cardOptionsAreDistinctAndPlayable() {
        LootGen loot = new LootGen(cards);
        RunState state = state(5L);
