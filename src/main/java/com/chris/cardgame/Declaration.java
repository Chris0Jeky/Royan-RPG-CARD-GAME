package com.chris.cardgame;

import java.util.ArrayList;
import java.util.List;

public class Declaration {
    private final List<Card> cards;
    private final List<Player> players;

    public Declaration() {
        this.cards = new ArrayList<>();
        this.players = new ArrayList<>();
        initializeCards();
        initializePlayers();
    }

    private void initializeCards() {
        cards.add(new Card("Dragon", "Monster", 50, 30, Card.AdvantageType.A, 1, Card.Rarity.B));
        cards.add(new Card("Knight", "Warrior", 30, 50, Card.AdvantageType.B, 1, Card.Rarity.A));
    }

    private void initializePlayers() {
        Player player1 = new Player("Alice");
        Player player2 = new Player("Bob");

        player1.getDeck().addCard(cards.get(0));
        player1.getDeck().addCard(cards.get(1));
        player2.getDeck().addCard(cards.get(0));
        player2.getDeck().addCard(cards.get(1));

