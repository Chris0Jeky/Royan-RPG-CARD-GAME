package com.chris.cardgame;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;
import java.util.SplittableRandom;

import com.chris.cardgame.ai.EnemyAi;
import com.chris.cardgame.combat.IntentKind;
import com.chris.cardgame.model.Behavior;
import org.junit.jupiter.api.Test;

class EnemyAiTest {

    @Test
    void burstCyclesTwoAttacksThenBuff() {
        EnemyAi ai = new EnemyAi();
        Map<Integer, Integer> memory = new HashMap<>();
        SplittableRandom rng = new SplittableRandom(1L);

        assertThat(ai.roll(Behavior.BURST, 0, memory, rng)).isEqualTo(IntentKind.ATTACK);
