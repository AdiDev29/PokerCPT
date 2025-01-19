package com.adrianandsasha.texasholdem.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Evaluates the best 5-card hand out of up to 7 cards (Texas Hold'em).
 * Categories from best (ROYAL_FLUSH) to worst (HIGH_CARD).
 */
public class TexasHoldemHandEvaluator {

    /**
     * Compare two 7-card sets (2 hole + up to 5 community).
     * Return >0 if hand1 is better, 0 if tie, <0 if hand2 is better.
     */
    public static int compareHands(List<Card> hand1, List<Card> hand2) {
        HandRank best1 = getBestHandRank(hand1);
        HandRank best2 = getBestHandRank(hand2);

        // Compare category ordinals (lower => stronger).
        int c1 = best1.getCategory().ordinal();
        int c2 = best2.getCategory().ordinal();
        if (c1 != c2) {
            // If c1 < c2, it means hand1 has a stronger category => return positive
            return (c1 < c2) ? 1 : -1;
        }

        // Same category => compare tiebreakers
        return compareTiebreakers(best1, best2);
    }

    /**
     * From up to 7 cards, pick the best 5-card hand rank.
     */
    public static HandRank getBestHandRank(List<Card> sevenCards) {
        List<List<Card>> combos = generate5CardCombos(sevenCards);
        HandRank best = null;

        for (List<Card> five : combos) {
            HandRank current = evaluate5CardHand(five);
            if (best == null) {
                best = current;
            } else {
                // If current category is stronger (lower ordinal) => better
                int curCat = current.getCategory().ordinal();
                int bestCat = best.getCategory().ordinal();
                if (curCat < bestCat) {
                    best = current;
                } else if (curCat == bestCat) {
                    // Tie => compare tiebreakers
                    if (compareTiebreakers(current, best) > 0) {
                        best = current;
                    }
                }
            }
        }
        return best;
    }

    /**
     * Evaluate exactly 5 cards => return the category + tiebreaker info.
     */
    public static HandRank evaluate5CardHand(List<Card> fiveCards) {
        // Sort descending by rank value
        List<Card> sorted = new ArrayList<>(fiveCards);
        sorted.sort((a, b) -> b.getRank().getValue() - a.getRank().getValue());

        boolean isFlush = checkFlush(sorted);
        boolean isStraight = checkStraight(sorted);  // includes Ace-low detection
        boolean isAceLow = (isStraight && isAceLowStraight(sorted));  // new helper

        // Build a frequency array
        int[] rankCounts = new int[15];  // index 2..14 used
        for (Card c : sorted) {
            rankCounts[c.getRank().getValue()]++;
        }

        // 1) ROYAL_FLUSH => A-K-Q-J-10 flush
        if (isFlush && isStraight
                && sorted.get(0).getRank() == Rank.ACE
                && sorted.get(1).getRank() == Rank.KING) {
            // By definition, top is Ace, next is King, so we have A-K-Q-J-10
            return new HandRank(HandCategory.ROYAL_FLUSH, collectRanks(sorted));
        }

        // 2) STRAIGHT_FLUSH
        if (isFlush && isStraight) {
            // If it's Ace-low, set tie-break ranks to [5, 4, 3, 2, 1]
            if (isAceLow) {
                return new HandRank(HandCategory.STRAIGHT_FLUSH, Arrays.asList(5, 4, 3, 2, 1));
            } else {
                // Normal high straight flush
                return new HandRank(HandCategory.STRAIGHT_FLUSH, collectRanks(sorted));
            }
        }

        // 3) FOUR_OF_A_KIND
        if (containsCount(rankCounts, 4)) {
            return rank4Helper(sorted, rankCounts, HandCategory.FOUR_OF_A_KIND);
        }

        // 4) FULL_HOUSE
        if (containsCount(rankCounts, 3) && containsCount(rankCounts, 2)) {
            return fullHouseHelper(sorted, rankCounts);
        }

        // 5) FLUSH
        if (isFlush) {
            return new HandRank(HandCategory.FLUSH, collectRanks(sorted));
        }

        // 6) STRAIGHT
        if (isStraight) {
            if (isAceLow) {
                // Ace plays as '1' => 5-high
                return new HandRank(HandCategory.STRAIGHT, Arrays.asList(5, 4, 3, 2, 1));
            } else {
                return new HandRank(HandCategory.STRAIGHT, collectRanks(sorted));
            }
        }

        // 7) THREE_OF_A_KIND
        if (containsCount(rankCounts, 3)) {
            return threeOfAKindHelper(sorted, rankCounts);
        }

        // 8) TWO_PAIR
        if (twoPairCount(rankCounts) >= 2) {
            return twoPairHelper(sorted, rankCounts);
        }

        // 9) ONE_PAIR
        if (containsCount(rankCounts, 2)) {
            return onePairHelper(sorted, rankCounts);
        }

        // 10) HIGH_CARD
        return new HandRank(HandCategory.HIGH_CARD, collectRanks(sorted));
    }

    // ------------------------------------------------------------------
    // Compare tiebreaker lists
    // ------------------------------------------------------------------
    private static int compareTiebreakers(HandRank hr1, HandRank hr2) {
        List<Integer> r1 = hr1.getRanks();
        List<Integer> r2 = hr2.getRanks();
        for (int i = 0; i < Math.min(r1.size(), r2.size()); i++) {
            if (r1.get(i) > r2.get(i)) return 1;
            if (r1.get(i) < r2.get(i)) return -1;
        }
        return 0; // tie
    }

    // ------------------------------------------------------------------
    // Generate all 5-card combos (7 choose 5)
    // ------------------------------------------------------------------
    private static List<List<Card>> generate5CardCombos(List<Card> cards) {
        List<List<Card>> results = new ArrayList<>();
        comboHelper(cards, 0, new ArrayList<>(), results);
        return results;
    }

    private static void comboHelper(List<Card> cards, int start, List<Card> temp, List<List<Card>> results) {
        if (temp.size() == 5) {
            results.add(new ArrayList<>(temp));
            return;
        }
        for (int i = start; i < cards.size(); i++) {
            temp.add(cards.get(i));
            comboHelper(cards, i + 1, temp, results);
            temp.remove(temp.size() - 1);
        }
    }

    // ------------------------------------------------------------------
    // checkFlush => all same suit?
    // ------------------------------------------------------------------
    private static boolean checkFlush(List<Card> five) {
        Suit firstSuit = five.get(0).getSuit();
        for (int i = 1; i < five.size(); i++) {
            if (five.get(i).getSuit() != firstSuit) {
                return false;
            }
        }
        return true;
    }

    // ------------------------------------------------------------------
    // checkStraight => true if ranks are consecutive or Ace-low (A-2-3-4-5)
    // ------------------------------------------------------------------
    private static boolean checkStraight(List<Card> sorted) {
        // Build a distinct descending list of ranks
        List<Integer> distinct = new ArrayList<>();
        for (Card c : sorted) {
            int val = c.getRank().getValue();
            if (!distinct.contains(val)) {
                distinct.add(val);
            }
        }

        // Check for normal 5 consecutive descending
        int consecutive = 1;
        for (int i = 0; i < distinct.size() - 1; i++) {
            if (distinct.get(i) - distinct.get(i + 1) == 1) {
                consecutive++;
            } else {
                consecutive = 1;
            }
            if (consecutive == 5) {
                return true;
            }
        }

        // Ace-low check => if 14,5,4,3,2 appear in distinct
        if (distinct.contains(14) && distinct.contains(5)
                && distinct.contains(4) && distinct.contains(3)
                && distinct.contains(2)) {
            return true;
        }

        return false;
    }

    /**
     * Extra helper: is this specifically the A-2-3-4-5 straight?
     */
    private static boolean isAceLowStraight(List<Card> sorted) {
        // Build distinct ranks
        List<Integer> distinct = new ArrayList<>();
        for (Card c : sorted) {
            int val = c.getRank().getValue();
            if (!distinct.contains(val)) {
                distinct.add(val);
            }
        }
        // If it has 14(A),5,4,3,2 => that is Ace-low
        return (distinct.contains(14) && distinct.contains(5)
                && distinct.contains(4) && distinct.contains(3)
                && distinct.contains(2));
    }

    // ------------------------------------------------------------------
    // Utility: does rankCounts[] contain a specific count?
    // ------------------------------------------------------------------
    private static boolean containsCount(int[] rankCounts, int count) {
        for (int c : rankCounts) {
            if (c == count) return true;
        }
        return false;
    }

    private static int twoPairCount(int[] rankCounts) {
        int pairs = 0;
        for (int c : rankCounts) {
            if (c == 2) pairs++;
        }
        return pairs;
    }

    /**
     * Collect ranks (descending) for final tie-break usage.
     * Used by flush, non-Ace-low straight, high card, etc.
     */
    private static List<Integer> collectRanks(List<Card> five) {
        List<Integer> ranks = new ArrayList<>();
        for (Card c : five) {
            ranks.add(c.getRank().getValue());
        }
        return ranks;
    }

    // ------------------------------------------------------------------
    // FOUR_OF_A_KIND => [fourVal, kicker]
    // ------------------------------------------------------------------
    private static HandRank rank4Helper(List<Card> sorted, int[] rankCounts, HandCategory cat) {
        int fourVal = 0;
        int kicker = 0;
        for (int r = 14; r >= 2; r--) {
            if (rankCounts[r] == 4) {
                fourVal = r;
            } else if (rankCounts[r] == 1) {
                // highest single as kicker
                kicker = Math.max(kicker, r);
            }
        }
        List<Integer> rr = new ArrayList<>();
        rr.add(fourVal);
        rr.add(kicker);
        return new HandRank(cat, rr);
    }

    // ------------------------------------------------------------------
    // FULL_HOUSE => [tripleVal, pairVal]
    // ------------------------------------------------------------------
    private static HandRank fullHouseHelper(List<Card> sorted, int[] rankCounts) {
        int tripleVal = 0, pairVal = 0;
        // find top triple
        for (int r = 14; r >= 2; r--) {
            if (rankCounts[r] == 3 && tripleVal == 0) {
                tripleVal = r;
            }
        }
        // find top pair, ignoring the tripleVal
        for (int r = 14; r >= 2; r--) {
            if (r != tripleVal && rankCounts[r] == 2) {
                pairVal = r;
                break;
            }
        }
        List<Integer> rr = new ArrayList<>();
        rr.add(tripleVal);
        rr.add(pairVal);
        return new HandRank(HandCategory.FULL_HOUSE, rr);
    }

    // ------------------------------------------------------------------
    // THREE_OF_A_KIND => [tripleVal, kicker1, kicker2]
    // ------------------------------------------------------------------
    private static HandRank threeOfAKindHelper(List<Card> sorted, int[] rankCounts) {
        int tripleVal = 0;
        List<Integer> kickers = new ArrayList<>();
        for (int r = 14; r >= 2; r--) {
            if (rankCounts[r] == 3 && tripleVal == 0) {
                tripleVal = r;
            } else if (rankCounts[r] == 1) {
                kickers.add(r);
            }
        }
        // We only keep top 2 kickers
        Collections.sort(kickers, Collections.reverseOrder());
        while (kickers.size() > 2) {
            kickers.remove(kickers.size() - 1);
        }
        List<Integer> rr = new ArrayList<>();
        rr.add(tripleVal);
        rr.addAll(kickers);
        return new HandRank(HandCategory.THREE_OF_A_KIND, rr);
    }

    // ------------------------------------------------------------------
    // TWO_PAIR => [pairVal1, pairVal2, kicker]
    // ------------------------------------------------------------------
    private static HandRank twoPairHelper(List<Card> sorted, int[] rankCounts) {
        List<Integer> foundPairs = new ArrayList<>();
        int kicker = 0;
        for (int r = 14; r >= 2; r--) {
            if (rankCounts[r] == 2) {
                foundPairs.add(r);
            } else if (rankCounts[r] == 1 && kicker == 0) {
                kicker = r;
            }
        }
        // Sort pairs descending
        foundPairs.sort((a, b) -> b - a);
        if (foundPairs.size() > 2) {
            foundPairs = foundPairs.subList(0, 2);
        }
        List<Integer> rr = new ArrayList<>(foundPairs);
        rr.add(kicker);
        return new HandRank(HandCategory.TWO_PAIR, rr);
    }

    // ------------------------------------------------------------------
    // ONE_PAIR => [pairVal, kicker1, kicker2, kicker3]
    // ------------------------------------------------------------------
    private static HandRank onePairHelper(List<Card> sorted, int[] rankCounts) {
        int pairVal = 0;
        List<Integer> kickers = new ArrayList<>();
        for (int r = 14; r >= 2; r--) {
            if (rankCounts[r] == 2 && pairVal == 0) {
                pairVal = r; // only take the top pair
            } else if (rankCounts[r] == 1) {
                kickers.add(r);
            }
        }
        Collections.sort(kickers, Collections.reverseOrder());
        // only keep top 3
        while (kickers.size() > 3) {
            kickers.remove(kickers.size() - 1);
        }
        List<Integer> rr = new ArrayList<>();
        rr.add(pairVal);
        rr.addAll(kickers);
        return new HandRank(HandCategory.ONE_PAIR, rr);
    }
}