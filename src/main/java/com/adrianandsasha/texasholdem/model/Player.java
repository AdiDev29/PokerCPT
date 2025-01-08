package com.adrianandsasha.texasholdem.model;

import java.util.ArrayList;
import java.util.List;
//instance for player
public class Player {

    private String id;         // Unique ID
    private String nickname;   // The name they entered
    private int chipStack;
    private boolean folded;
    private int currentBet;
    //I also searched this name up. did you know the 2 cards dealt to you are called hole cards?
    private List<Card> holeCards = new ArrayList<>();
//values assigned to player initially
    public Player(String id, String nickname, int initialChips) {
        this.id = id;
        this.nickname = nickname;
        this.chipStack = initialChips;
        this.folded = false;
        this.currentBet = 0;
    }
//after round, when new hand is given. chips, nickname, id and others dont get resetted.
    public void resetForNewHand() {
        folded = false;
        currentBet = 0;
        holeCards.clear();
    }
//when cards are added
    public void receiveCard(Card c) {
        holeCards.add(c);
    }
//getter methods
    public String getId() {
        return id;
    }

    public String getNickname() {
        return nickname;
    }

    public int getChipStack() {
        return chipStack;
    }
//setter method also im getting a suggestion from intellij to use lombok but i dont care

    public void setChipStack(int chipStack) {
        this.chipStack = chipStack;
    }

    public boolean isFolded() {
        return folded;
    }

    public void fold() {
        folded = true;
    }

    public int getCurrentBet() {
        return currentBet;
    }

    public void setCurrentBet(int currentBet) {
        this.currentBet = currentBet;
    }

    public List<Card> getHoleCards() {
        return holeCards;
    }
}
