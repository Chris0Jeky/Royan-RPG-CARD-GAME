package com.chris.cardgame;

public class Flow {
    public enum Phase {
        DRAW, PLAY, COMBAT, END
    }

    private Phase current = Phase.DRAW;

    public Phase current() {
        return current;
    }

    public Phase next() {
        switch (current) {
            case DRAW:
