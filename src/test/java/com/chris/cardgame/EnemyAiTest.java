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
