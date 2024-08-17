package com.chris.cardgame.combat;

public record Intent(IntentKind kind, int preview) {

    @Override
    public String toString() {
