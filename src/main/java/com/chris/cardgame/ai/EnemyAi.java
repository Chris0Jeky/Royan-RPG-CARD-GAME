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
            case TRICKSTER -> weighted(rng, 5, 2, 0, 3);
            case BURST -> {
                int count = memory.getOrDefault(enemyIndex, 0);
                if (count >= 2) {
                    memory.put(enemyIndex, 0);
                    yield IntentKind.BUFF;
                }
                memory.put(enemyIndex, count + 1);
                yield IntentKind.ATTACK;
            }
        };
    }

    private IntentKind weighted(SplittableRandom rng, int attack, int defend, int buff, int debuff) {
        int total = attack + defend + buff + debuff;
        int roll = rng.nextInt(total);
        if (roll < attack) {
            return IntentKind.ATTACK;
