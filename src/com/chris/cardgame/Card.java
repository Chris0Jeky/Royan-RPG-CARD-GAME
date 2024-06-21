package com.chris.cardgame;

public class Card {
    enum AdvantageType {
        A, B, C
    }

    private final String cardName;
    private final String type;
    private int value;
    private AdvantageType advantageType;

    public Card(String cardName, String type, int value, AdvantageType advantageType) {
        this.cardName = cardName;
        this.type = type;
        this.value = value;
        this.advantageType = advantageType;
    }

    public void displayCardFields() {
        System.out.println("Card Name: " + cardName);
        System.out.println("Card type: " + type);
        System.out.println("Card value: " + value);
    }

    public String getCardName() {
        System.out.println("Card Name: " + cardName);
        return cardName;
    }

    public String getType() {
        System.out.println("Card type: " + type);
        return type;
    }

    public int getValue() {
        System.out.println("Card value: " + value);
        return value;
    }

    public AdvantageType getAdvantageType() {
        System.out.println("Advantage Type: " + advantageType);
        return advantageType;
    }
}