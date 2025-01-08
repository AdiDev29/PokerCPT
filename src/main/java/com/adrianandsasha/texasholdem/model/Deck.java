package com.adrianandsasha.texasholdem.model;
//lmao. I call this the trinity for better arrays.
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
// Class for the deck with deck related operations: shuffling and dealing a card.
public class Deck {
    private List<Card> cards;
    private int currentIndex;

    public Deck() {
        cards = new ArrayList<>();
        for (Suit suit : Suit.values()) {
            for (Rank rank : Rank.values()) {
                cards.add(new Card(suit, rank));
            }
        }
        shuffle();
    }

    public void shuffle() {
        Collections.shuffle(cards);
        currentIndex = 0;
    }

    public Card dealCard() {
        if (currentIndex >= cards.size()) {
            throw new IllegalStateException("No more cards in the deck!");
        }
        return cards.get(currentIndex++);
    }
}
