package com.chris.cardgame;

public class Mechanics {
    public static void start_game(){
        System.out.println("Game starting...");
        Card card1 = new Card("Card1", "Attack", 5);
        Player player1 = new Player("0012f", "Player1", 100);

        card1.displayCardFields();
        card1.getCardName();
        player1.getPlayerID();
        player1.getPlayerName();
    }
}
