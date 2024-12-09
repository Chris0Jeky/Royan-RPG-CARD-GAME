package com.chris.cardgame.ai;

import java.util.Map;
import java.util.SplittableRandom;

import com.chris.cardgame.combat.IntentKind;
import com.chris.cardgame.model.Behavior;

public class EnemyAi {

    public IntentKind roll(Behavior behavior, int enemyIndex, Map<Integer, Integer> memory,
            SplittableRandom rng) {
        return switch (behavior) {
            case AGGRO -> weighted(rng, 8, 1, 1, 0);
            case TURTLE -> weighted(rng, 4, 4, 2, 0);
