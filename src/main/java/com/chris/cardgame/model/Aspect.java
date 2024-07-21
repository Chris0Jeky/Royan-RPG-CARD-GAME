package com.chris.cardgame.model;

public enum Aspect {
    MIGHT, GUILE, FOCUS;

    public boolean beats(Aspect other) {
        return (this == MIGHT && other == GUILE)
                || (this == GUILE && other == FOCUS)
                || (this == FOCUS && other == MIGHT);
    }
}
