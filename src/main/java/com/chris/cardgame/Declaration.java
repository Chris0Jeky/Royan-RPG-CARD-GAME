package com.chris.cardgame;

import java.util.ArrayList;
import java.util.List;

public class Declaration {
    private final List<Card> cards;
    private final List<Player> players;

    public Declaration() {
        this.cards = new ArrayList<>();
        this.players = new ArrayList<>();
