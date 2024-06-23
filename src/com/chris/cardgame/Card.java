package com.chris.cardgame;

public class Card {
    enum AdvantageType {
        A, B, C
    }

    private final String cardName;
    private final String type;
    private int attackVal;
    private int defenceVal;
    private AdvantageType advantageType;
    private int effect;
    private boolean oneTime;

    public Card(String cardName, String type, int attackVal, int defenceVal, AdvantageType advantageType) {
        this.cardName = cardName;
        this.type = type;
        this.attackVal = attackVal;
        this.defenceVal = defenceVal;
        this.advantageType = advantageType;
        this.effect = 0;
        this.oneTime = false;

    }

    public void displayCardFields() {
        System.out.println("Card Name: " + cardName);
        System.out.println("Card type: " + type);
        System.out.println("Attack value: " + attackVal);
        System.out.println("Defence value: " + defenceVal);
    }

    public String getCardName() {
        System.out.println("Card Name: " + cardName);
        return cardName;
    }

    public String getType() {
        System.out.println("Card type: " + type);
        return type;
    }

    public int getAttackVal() {
        System.out.println("Attack value: " + attackVal);
        return attackVal;
    }

    public void setAttackVal(int attackVal) {
        this.attackVal = attackVal;
    }

    public int getDefenceVal() {
        System.out.println("Defence value: " + defenceVal);
        return defenceVal;
    }

    public void setDefenceVal(int defenceVal) {
        this.defenceVal = defenceVal;
    }

    public AdvantageType getAdvantageType() {
        System.out.println("Advantage Type: " + advantageType);
        return advantageType;
    }

    public void addToDefenceVal(int defenceVal) {
        this.defenceVal = defenceVal;
    }
}