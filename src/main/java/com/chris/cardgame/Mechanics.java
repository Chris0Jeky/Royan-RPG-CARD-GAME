package com.chris.cardgame;

public class Mechanics {
    public static Declaration start_game() {
        System.out.println("Game starting...");
        Declaration declaration = new Declaration();
        System.out.println("Players: " + declaration.getPlayers().size()
                + ", cards: " + declaration.getCards().size());
        return declaration;
    }
}
