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
    private final AdvantageType advantageType;
    private int effect = 0;
    private boolean oneTime = false;
    private int level = 1;
    private final Rarity rarity;


    public Card(String cardName, String type, int attackVal, int defenceVal, AdvantageType advantageType, int level, Rarity rarity) {
        this.cardName = cardName;
        this.type = type;
        this.attackVal = attackVal;
        this.defenceVal = defenceVal;
        this.advantageType = advantageType;
        this.level = level;
        this.rarity = rarity;

    }

    public void displayCardFields() {
        System.out.println("Card Name: " + cardName);
        System.out.println("Card type: " + type);
        System.out.println("Attack value: " + attackVal);
        System.out.println("Defence value: " + defenceVal);
    }
