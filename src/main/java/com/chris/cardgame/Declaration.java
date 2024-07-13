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
