package com.adrianandsasha.texasholdem.model;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
//the actual hand evaluator
public class TexasHoldemHandEvaluator {

    public static HandRank getBestHandRank(List<Card> sevenCards) {
        // Generate all 5-card combos and pick the best. Maybe theres a faster way to do this, but this is what i thought of
        List<List<Card>> combos = generate5CardCombos(sevenCards);
        HandRank best = null;

        for (List<Card> five : combos) {
            HandRank hr = evaluate5CardHand(five);
            if (best == null) {
                best = hr;
            } else {
                int catCompare = hr.getCategory().compareTo(best.getCategory());
                if (catCompare > 0) {
                    best = hr;
                } else if (catCompare == 0) {
                    if (compareTiebreakers(hr, best) > 0) {
                        best = hr;
                    }
                }
            }
        }
        return best;
    }

    private static List<List<Card>> generate5CardCombos(List<Card> cards) {
        List<List<Card>> result = new ArrayList<>();
        comboHelper(cards, 0, new ArrayList<>(), result);
        return result;
    }
//helper for combo generator
    private static void comboHelper(List<Card> cards, int start, List<Card> temp, List<List<Card>> result) {
        if (temp.size() == 5) {
            result.add(new ArrayList<>(temp));
            return;
        }
        for (int i = start; i < cards.size(); i++) {
            temp.add(cards.get(i));
            comboHelper(cards, i + 1, temp, result);
            temp.remove(temp.size() - 1);
        }
    }
//tie brakers like for example if 2 people have pairs, one would win by high card, or if theres another hand
    private static int compareTiebreakers(HandRank a, HandRank b) {
        List<Integer> ra = a.getRanks();
        List<Integer> rb = b.getRanks();
        for (int i = 0; i < Math.min(ra.size(), rb.size()); i++) {
            if (ra.get(i) > rb.get(i)) return 1;
            if (ra.get(i) < rb.get(i)) return -1;
        }
        return 0;
    }

//evaluate only exactly 5 cards
    public static HandRank evaluate5CardHand(List<Card> fiveCards) {
        // Sort descending
        List<Card> sorted = new ArrayList<>(fiveCards);
        sorted.sort((c1, c2) -> c2.getRank().getValue() - c1.getRank().getValue());

        boolean isFlush = checkFlush(sorted);
        boolean isStraight = checkStraight(sorted);

        int[] rankCounts = new int[15];
        for (Card c : sorted) {
            rankCounts[c.getRank().getValue()]++;
        }
// sets hand
        // 1. Royal Flush
        if (isFlush && isStraight && sorted.get(0).getRank() == Rank.ACE && sorted.get(1).getRank() == Rank.KING) {
            return new HandRank(HandCategory.ROYAL_FLUSH, collectRanks(sorted));
        }
        // 2. Straight Flush
        if (isFlush && isStraight) {
            return new HandRank(HandCategory.STRAIGHT_FLUSH, collectRanks(sorted));
        }
        // 3. Four of a Kind
        if (containsCount(rankCounts, 4)) {
            return rank4Helper(sorted, rankCounts, HandCategory.FOUR_OF_A_KIND);
        }
        // 4. Full House
        if (containsCount(rankCounts, 3) && containsCount(rankCounts, 2)) {
            return fullHouseHelper(sorted, rankCounts);
        }
        // 5. Flush
        if (isFlush) {
            return new HandRank(HandCategory.FLUSH, collectRanks(sorted));
        }
        // 6. Straight
        if (isStraight) {
            return new HandRank(HandCategory.STRAIGHT, collectRanks(sorted));
        }
        // 7. Three of a Kind
        if (containsCount(rankCounts, 3)) {
            return threeOfAKindHelper(sorted, rankCounts);
        }
        // 8. Two Pair
        if (twoPairCount(rankCounts) == 2) {
            return twoPairHelper(sorted, rankCounts);
        }
        // 9. One Pair
        if (containsCount(rankCounts, 2)) {
            return onePairHelper(sorted, rankCounts);
        }
        // 10. High card
        return new HandRank(HandCategory.HIGH_CARD, collectRanks(sorted));
    }
// these check for hands
    private static boolean checkFlush(List<Card> cards) {
        Suit s = cards.get(0).getSuit();
        for (Card c : cards) {
            if (c.getSuit() != s) return false;
        }
        return true;
    }

    private static boolean checkStraight(List<Card> sorted) {
        int consecutive = 1;
        for (int i = 0; i < sorted.size()-1; i++) {
            if (sorted.get(i).getRank().getValue() - sorted.get(i+1).getRank().getValue() == 1) {
                consecutive++;
            } else {
                break;
            }
        }
        if (consecutive == 5) return true;
        // Ace-low
        List<Integer> ranks = sorted.stream().map(c -> c.getRank().getValue()).collect(Collectors.toList());
        if (ranks.contains(14) && ranks.contains(5) && ranks.contains(4)
                && ranks.contains(3) && ranks.contains(2)) {
            return true;
        }
        return false;
    }

    private static boolean containsCount(int[] rankCounts, int c) {
        for (int count : rankCounts) {
            if (count == c) return true;
        }
        return false;
    }

    private static int twoPairCount(int[] rankCounts) {
        int pairs = 0;
        for (int count : rankCounts) {
            if (count == 2) pairs++;
        }
        return pairs;
    }

    private static List<Integer> collectRanks(List<Card> cards) {
        List<Integer> r = new ArrayList<>();
        for (Card c : cards) {
            r.add(c.getRank().getValue());
        }
        return r;
    }

    private static HandRank rank4Helper(List<Card> sorted, int[] rankCounts, HandCategory cat) {
        int fourVal = 0, kickerVal = 0;
        for (int r = 14; r >= 2; r--) {
            if (rankCounts[r] == 4) fourVal = r;
            else if (rankCounts[r] == 1) kickerVal = r;
        }
        List<Integer> ranks = new ArrayList<>();
        ranks.add(fourVal);
        ranks.add(kickerVal);
        return new HandRank(cat, ranks);
    }

    private static HandRank fullHouseHelper(List<Card> sorted, int[] rankCounts) {
        int tripleVal = 0, pairVal = 0;
        for (int r = 14; r >= 2; r--) {
            if (rankCounts[r] == 3) tripleVal = r;
            if (rankCounts[r] == 2) pairVal = r;
        }
        List<Integer> ranks = new ArrayList<>();
        ranks.add(tripleVal);
        ranks.add(pairVal);
        return new HandRank(HandCategory.FULL_HOUSE, ranks);
    }

    private static HandRank threeOfAKindHelper(List<Card> sorted, int[] rankCounts) {
        int tripleVal = 0;
        List<Integer> kickers = new ArrayList<>();
        for (int r = 14; r >= 2; r--) {
            if (rankCounts[r] == 3) tripleVal = r;
            else if (rankCounts[r] == 1) kickers.add(r);
        }
        kickers.sort((a,b)->b-a);
        List<Integer> ranks = new ArrayList<>();
        ranks.add(tripleVal);
        ranks.addAll(kickers);
        return new HandRank(HandCategory.THREE_OF_A_KIND, ranks);
    }

    private static HandRank twoPairHelper(List<Card> sorted, int[] rankCounts) {
        List<Integer> pairs = new ArrayList<>();
        int kicker = 0;
        for (int r = 14; r >= 2; r--) {
            if (rankCounts[r] == 2) pairs.add(r);
            else if (rankCounts[r] == 1 && kicker == 0) kicker = r;
        }
        pairs.sort((a,b)->b-a);
        List<Integer> ranks = new ArrayList<>();
        ranks.addAll(pairs);
        ranks.add(kicker);
        return new HandRank(HandCategory.TWO_PAIR, ranks);
    }

    private static HandRank onePairHelper(List<Card> sorted, int[] rankCounts) {
        int pairVal = 0;
        List<Integer> kickers = new ArrayList<>();
        for (int r = 14; r >= 2; r--) {
            if (rankCounts[r] == 2) pairVal = r;
            else if (rankCounts[r] == 1) kickers.add(r);
        }
        kickers.sort((a,b)->b-a);
        List<Integer> ranks = new ArrayList<>();
        ranks.add(pairVal);
        ranks.addAll(kickers);
        return new HandRank(HandCategory.ONE_PAIR, ranks);
    }
}
