package com.chris.cardgame;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

import com.chris.cardgame.map.ActMap;
import com.chris.cardgame.map.MapGen;
import com.chris.cardgame.map.MapNode;
import com.chris.cardgame.map.NodeType;
import org.junit.jupiter.api.Test;

class MapGenTest {

    @Test
    void actsHaveCorrectNodeCounts() {
        MapGen gen = new MapGen();

        assertThat(gen.generate(1, 42L).nodes()).hasSize(15);
        assertThat(gen.generate(2, 42L).nodes()).hasSize(18);
        assertThat(gen.generate(3, 42L).nodes()).hasSize(22);
    }

    @Test
    void entriesAreCombatAndBossIsLast() {
        for (int act = 1; act <= 3; act++) {
            ActMap map = new MapGen().generate(act, 7L);

            assertThat(map.entries()).isNotEmpty();
            map.entries().forEach(id ->
                    assertThat(map.node(id).type()).isEqualTo(NodeType.COMBAT));
            MapNode boss = map.node(map.bossId());
            assertThat(boss.type()).isEqualTo(NodeType.BOSS);
            assertThat(boss.children()).isEmpty();
            assertThat(boss.layer()).isEqualTo(map.layers() - 1);
        }
    }

    @Test
    void everyNodeReachableAndLinked() {
        for (int act = 1; act <= 3; act++) {
            ActMap map = new MapGen().generate(act, 99L);
            Set<String> seen = new HashSet<>();
            Deque<String> queue = new ArrayDeque<>(map.entries());
            while (!queue.isEmpty()) {
                String id = queue.removeFirst();
                if (seen.add(id)) {
                    queue.addAll(map.node(id).children());
                }
