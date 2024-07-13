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

    public List<Card> getCards() {
        return Collections.unmodifiableList(cards);
    }

    public int size() {
        return cards.size();
    }

    public boolean isEmpty() {
        return cards.isEmpty();
    }

    public void shuffle(Random rng) {
        Collections.shuffle(cards, rng);
    }

    public Optional<Card> draw() {
        if (cards.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(cards.remove(0));
    }
}
