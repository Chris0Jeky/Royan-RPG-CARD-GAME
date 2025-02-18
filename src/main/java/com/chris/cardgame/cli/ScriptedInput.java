package com.chris.cardgame.cli;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.NoSuchElementException;

public class ScriptedInput implements Input {
    private final Deque<String> lines;
