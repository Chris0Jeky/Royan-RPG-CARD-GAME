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
