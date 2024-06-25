package com.chris.cardgame;

import java.util.ArrayList;
import java.util.List;

public class Declaration {
    private List<Card> cards;
    private List<Player> players;

    public Declaration() {
        this.cards = new ArrayList<>();
        this.players = new ArrayList<>();
        initializeCards();
        initializePlayers();
    }

    private void initializeCards() {
        // Initialize 22 cards
        cards.add(new Card("Dragon", "Monster", 50, 30, Card.AdvantageType.A, 1, Card.Rarity.B));
        cards.add(new Card("Knight", "Warrior", 30, 50, Card.AdvantageType.B, 1, Card.Rarity.A));
        // Add more cards as needed...
    }

    private void initializePlayers() {
        // Initialize 2 players
        Player player1 = new Player("Alice");
        Player player2 = new Player("Bob");

        // Add cards to players' decks
        player1.getDeck().addCard(cards.get(0));
        player1.getDeck().addCard(cards.get(1));
        player2.getDeck().addCard(cards.get(0));
        player2.getDeck().addCard(cards.get(1));
        // Add more cards to players' decks as needed...

        players.add(player1);
        players.add(player2);
    }

    public List<Card> getCards() {
        return cards;
    }

    public List<Player> getPlayers() {
        return players;
    }

    Card card1 = new Card("Card1", "Type1", 10, 20, Card.AdvantageType.A, 1, Card.Rarity.A);
    Card card2 = new Card("Card2", "Type2", 15, 25, Card.AdvantageType.B, 2, Card.Rarity.B);
    Card card3 = new Card("Card3", "Type3", 20, 30, Card.AdvantageType.C, 3, Card.Rarity.C);
    Card card4 = new Card("Card4", "Type1", 25, 35, Card.AdvantageType.A, 1, Card.Rarity.A);
    Card card5 = new Card("Card5", "Type2", 30, 40, Card.AdvantageType.B, 2, Card.Rarity.B);
    Card card6 = new Card("Card6", "Type3", 35, 45, Card.AdvantageType.C, 3, Card.Rarity.C);
    Card card7 = new Card("Card7", "Type1", 40, 50, Card.AdvantageType.A, 1, Card.Rarity.A);
    Card card8 = new Card("Card8", "Type2", 45, 55, Card.AdvantageType.B, 2, Card.Rarity.B);
    Card card9 = new Card("Card9", "Type3", 50, 60, Card.AdvantageType.C, 3, Card.Rarity.C);
    Card card10 = new Card("Card10", "Type1", 55, 65, Card.AdvantageType.A, 1, Card.Rarity.A);
    Card card11 = new Card("Card11", "Type2", 60, 70, Card.AdvantageType.B, 2, Card.Rarity.B);
    Card card12 = new Card("Card12", "Type3", 65, 75, Card.AdvantageType.C, 3, Card.Rarity.C);
    Card card13 = new Card("Card13", "Type1", 70, 80, Card.AdvantageType.A, 1, Card.Rarity.A);
    Card card14 = new Card("Card14", "Type2", 75, 85, Card.AdvantageType.B, 2, Card.Rarity.B);
    Card card15 = new Card("Card15", "Type3", 80, 90, Card.AdvantageType.C, 3, Card.Rarity.C);
    Card card16 = new Card("Card16", "Type1", 85, 95, Card.AdvantageType.A, 1, Card.Rarity.A);
    Card card17 = new Card("Card17", "Type2", 90, 100, Card.AdvantageType.B, 2, Card.Rarity.B);
    Card card18 = new Card("Card18", "Type3", 95, 105, Card.AdvantageType.C, 3, Card.Rarity.C);

    Player player1 = new Player("0012f", "Player1", 100);
    Player player2 = new Player("0013f", "Player2", 100);
}