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

    public String getPlayerID() {
        return playerID;
    }

    public String getPlayerName() {
        return playerName;
    }

    public int getHealth() {
        return health;
    }

    public void setHealth(int health) {
        this.health = Math.max(0, health);
    }

    public Deck getDeck() {
