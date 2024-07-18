package com.chris.cardgame;

public class Player {
    private final String playerID;
    private final String playerName;
    private int health;
    private final Deck deck;

    public Player(String playerID, String playerName, int health) {
        this.playerID = playerID;
        this.playerName = playerName;
        this.health = health;
        this.deck = new Deck();
    }

    public Player(String playerName) {
        this("player-" + playerName.toLowerCase(), playerName, 100);
    }
