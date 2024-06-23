package com.chris.cardgame;

public class Card {
    enum AdvantageType {
        A, B, C
    }

    private final String cardName;
    private final String type;
    private int values;
    private int attackVal;
    private int defenceVal;
    private AdvantageType advantageType;
    private int effect;
    private boolean oneTime;


    public Card(String cardName, String type, int values, AdvantageType advantageType) {
        this.cardName = cardName;
        this.type = type;
        this.values = values;
        this.advantageType = advantageType;
    }

    public void displayCardFields() {
        System.out.println("Card Name: " + cardName);
        System.out.println("Card type: " + type);
        System.out.println("Card value: " + values);
    }

    public String getCardName() {
        System.out.println("Card Name: " + cardName);
        return cardName;
    }

    public String getType() {
        System.out.println("Card type: " + type);
        return type;
    }

    public int getValues() {
        System.out.println("Card value: " + values);
        return values;
    }

    public AdvantageType getAdvantageType() {
        System.out.println("Advantage Type: " + advantageType);
        return advantageType;
    }

    public updateCardValue()
}