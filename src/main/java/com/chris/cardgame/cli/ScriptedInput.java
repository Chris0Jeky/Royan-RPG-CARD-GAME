package com.chris.cardgame.cli;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.NoSuchElementException;

public class ScriptedInput implements Input {
    private final Deque<String> lines;

    public ScriptedInput(List<String> lines) {
        this.lines = new ArrayDeque<>(lines);
    }

    public static ScriptedInput fuzz(int cycles) {
        List<String> cycle = List.of("0", "play 0 0", "end", "leave");
        ArrayDeque<String> lines = new ArrayDeque<>();
        for (int i = 0; i < cycles; i++) {
            lines.addAll(cycle);
        }
        return new ScriptedInput(List.copyOf(lines));
    }

    @Override
    public String readLine(String prompt) {
        if (lines.isEmpty()) {
            throw new NoSuchElementException("script exhausted at: " + prompt);
        }
        return lines.removeFirst();
    }

    public int remaining() {
        return lines.size();
    }
}
