package com.chris.cardgame;

public class Card {
    enum AdvantageType {
        A, B, C
    }
    enum Rarity {
        A, B, C, D
    }

    private final String cardName;
    private final String type;
    private int attackVal;
    private int defenceVal;
