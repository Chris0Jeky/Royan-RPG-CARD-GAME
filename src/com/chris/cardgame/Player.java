package com.chris.cardgame;

import java.util.LinkedList;

public class Player {
    private String playerID;
    private String playerName;
    private int health;
    private 

    public Player(String playerID, String playerName, int health) {
        this.playerID = playerID;
        this.playerName = playerName;
        this.health = health;
    }

    public String getPlayerID() {
        System.out.println("Player id: " + playerID);
        return playerID;
    }

    public String getPlayerName() {
        System.out.println("Player name: " + playerName);
        return playerName;
    }

    public int getHealth() {
        System.out.println("Player health: " + health);
        return health;
    }
}
