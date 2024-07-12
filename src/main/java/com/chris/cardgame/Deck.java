package com.chris.cardgame;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;

public class Deck {
    private final List<Card> cards = new ArrayList<>();

    public void addCard(Card card) {
        cards.add(Objects.requireNonNull(card, "card"));
    }

