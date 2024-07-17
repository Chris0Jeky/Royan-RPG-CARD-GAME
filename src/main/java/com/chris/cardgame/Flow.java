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
                current = Phase.PLAY;
                break;
            case PLAY:
                current = Phase.COMBAT;
                break;
            case COMBAT:
                current = Phase.END;
                break;
            case END:
            default:
                break;
        }
