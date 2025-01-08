package com.adrianandsasha.texasholdem.model;
import java.util.List;
//ranks hands. actual conditions dont happen here
public class HandRank {
    private final HandCategory category;
    private final List<Integer> ranks; // for tiebreakers

    public HandRank(HandCategory category, List<Integer> ranks) {
        this.category = category;
        this.ranks = ranks;
    }

    public HandCategory getCategory() {
        return category;
    }

    public List<Integer> getRanks() {
        return ranks;
    }
}
