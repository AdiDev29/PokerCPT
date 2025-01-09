package com.adrianandsasha.texasholdem.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Evaluates the best 5-card hand from a 7-card set (2 hole + 5 community).
 * Then compares hands by standard Texas Hold'em ranking:
 *
 *  1) Royal Flush
 *  2) Straight Flush
 *  3) Four of a Kind
 *  4) Full House
 *  5) Flush
 *  6) Straight
 *  7) Three of a Kind
 *  8) Two Pair
 *  9) One Pair
 * 10) High Card
 *
 * Tiebreakers: hand category first, then relevant rank lists in descending order.
 */
public class TexasHoldemHandEvaluator {

    /**
     * Compare two sets of up to 7 cards (2 hole + 5 community).
     * Returns < 0 if hand1 is worse, 0 if tie, > 0 if hand1 is better.
     */
    public static int compareHands(List<Card> hand1, List<Card> hand2) {
        HandRank best1 = getBestHandRank(hand1);
        HandRank best2 = getBestHandRank(hand2);

        // Compare categories
        int catCompare = best1.getCategory().compareTo(best2.getCategory());
        if (catCompare != 0) {
            return catCompare; // positive => best1 category is higher
        }
        // Same category => compare tiebreakers
        return compareTiebreakers(best1, best2);
    }

    /**
     * Generate the best 5-card hand rank from a 7-card set.
     */
    public static HandRank getBestHandRank(List<Card> sevenCards) {
        // Generate all 5-card combos
        List<List<Card>> combos = generate5CardCombos(sevenCards);
        HandRank best = null;

        for (List<Card> five : combos) {
            HandRank hr = evaluate5CardHand(five);
            if (best == null) {
                best = hr;
            } else {
                int catCompare = hr.getCategory().compareTo(best.getCategory());
                if (catCompare > 0) {
                    // Found a strictly better category
                    best = hr;
                } else if (catCompare == 0) {
                    // Same category => check tiebreakers
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

    /**
     * Recursively build all 5-card combinations from the given list.
     */
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

    /**
     * Evaluate exactly 5 cards. Return a HandRank object.
     */
    public static HandRank evaluate5CardHand(List<Card> fiveCards) {
        // Sort descending by rank
        List<Card> sorted = new ArrayList<>(fiveCards);
        sorted.sort((a, b) -> b.getRank().getValue() - a.getRank().getValue());

        boolean isFlush = checkFlush(sorted);
        boolean isStraight = checkStraight(sorted);
        int[] rankCounts = new int[15]; // index 2..14 used

        for (Card c : sorted) {
            rankCounts[c.getRank().getValue()]++;
        }

        // 1) Royal Flush
        //    => flush + straight + top card is Ace
        if (isFlush && isStraight && sorted.get(0).getRank() == Rank.ACE
                && sorted.get(1).getRank() == Rank.KING) {
            return new HandRank(HandCategory.ROYAL_FLUSH, collectRanks(sorted));
        }
        // 2) Straight Flush
        if (isFlush && isStraight) {
            return new HandRank(HandCategory.STRAIGHT_FLUSH, collectRanks(sorted));
        }
        // 3) Four of a Kind
        if (containsCount(rankCounts, 4)) {
            return rank4Helper(sorted, rankCounts, HandCategory.FOUR_OF_A_KIND);
        }
        // 4) Full House
        if (containsCount(rankCounts, 3) && containsCount(rankCounts, 2)) {
            return fullHouseHelper(sorted, rankCounts);
        }
        // 5) Flush
        if (isFlush) {
            return new HandRank(HandCategory.FLUSH, collectRanks(sorted));
        }
        // 6) Straight
        if (isStraight) {
            return new HandRank(HandCategory.STRAIGHT, collectRanks(sorted));
        }
        // 7) Three of a Kind
        if (containsCount(rankCounts, 3)) {
            return threeOfAKindHelper(sorted, rankCounts);
        }
        // 8) Two Pair
        if (twoPairCount(rankCounts) >= 2) {
            return twoPairHelper(sorted, rankCounts);
        }
        // 9) One Pair
        if (containsCount(rankCounts, 2)) {
            return onePairHelper(sorted, rankCounts);
        }
        // 10) High Card
        return new HandRank(HandCategory.HIGH_CARD, collectRanks(sorted));
    }

    /**
     * Compare the tiebreaker ranks of two HandRank objects.
     * Return > 0 if hr1 is better, < 0 if hr2 is better, 0 if tie.
     */
    private static int compareTiebreakers(HandRank hr1, HandRank hr2) {
        List<Integer> r1 = hr1.getRanks();
        List<Integer> r2 = hr2.getRanks();
        for (int i = 0; i < Math.min(r1.size(), r2.size()); i++) {
            if (r1.get(i) > r2.get(i)) return 1;
            if (r1.get(i) < r2.get(i)) return -1;
        }
        return 0;
    }

    /**
     * Check if all cards share the same suit => flush.
     */
    private static boolean checkFlush(List<Card> cards) {
        Suit s = cards.get(0).getSuit();
        for (Card c : cards) {
            if (c.getSuit() != s) return false;
        }
        return true;
    }

    /**
     * Check for a 5-card straight (including Ace-low).
     */
    private static boolean checkStraight(List<Card> sorted) {
        // Basic descending check
        int consecutive = 1;
        for (int i = 0; i < sorted.size() - 1; i++) {
            if (sorted.get(i).getRank().getValue()
                    - sorted.get(i + 1).getRank().getValue() == 1) {
                consecutive++;
            } else {
                break;
            }
        }
        if (consecutive == 5) return true;

        // Ace-low check => A,5,4,3,2
        List<Integer> vals = sorted.stream()
                .map(c -> c.getRank().getValue())
                .collect(Collectors.toList());
        // If we see (14,5,4,3,2), that's an Ace-low straight
        if (vals.contains(14) && vals.contains(5)
                && vals.contains(4) && vals.contains(3) && vals.contains(2)) {
            return true;
        }
        return false;
    }

    private static boolean containsCount(int[] rankCounts, int count) {
        for (int c : rankCounts) {
            if (c == count) return true;
        }
        return false;
    }

    private static int twoPairCount(int[] rankCounts) {
        int pairs = 0;
        for (int c : rankCounts) {
            if (c == 2) {
                pairs++;
            }
        }
        return pairs;
    }

    /**
     * Collect ranks in descending order from the sorted 5-card set
     * for final tiebreak usage (flush, straight, etc.).
     */
    private static List<Integer> collectRanks(List<Card> cards) {
        List<Integer> res = new ArrayList<>();
        for (Card c : cards) {
            res.add(c.getRank().getValue());
        }
        return res;
    }

    /**
     * 4-of-a-kind
     */
    private static HandRank rank4Helper(List<Card> sorted,
                                        int[] rankCounts,
                                        HandCategory category) {
        int fourVal = 0, kickerVal = 0;
        // We search top-down so we find the highest 4-of-kind rank
        // and the highest single kicker
        for (int r = 14; r >= 2; r--) {
            if (rankCounts[r] == 4) {
                fourVal = r;
            } else if (rankCounts[r] == 1) {
                kickerVal = Math.max(kickerVal, r);
            }
        }
        List<Integer> ranks = new ArrayList<>();
        ranks.add(fourVal);
        ranks.add(kickerVal);
        return new HandRank(category, ranks);
    }

    /**
     * Full house => pick top triple, then top pair
     */
    private static HandRank fullHouseHelper(List<Card> sorted, int[] rankCounts) {
        int tripleVal = 0, pairVal = 0;
        // We find the highest triple, then the highest pair
        for (int r = 14; r >= 2; r--) {
            if (rankCounts[r] == 3 && tripleVal == 0) {
                tripleVal = r;
            }
        }
        for (int r = 14; r >= 2; r--) {
            if (rankCounts[r] == 2 && r != tripleVal) {
                pairVal = r;
                break;
            }
        }
        List<Integer> ranks = new ArrayList<>();
        ranks.add(tripleVal);
        ranks.add(pairVal);
        return new HandRank(HandCategory.FULL_HOUSE, ranks);
    }

    /**
     * Three of a kind => pick top triple, then top 2 kickers
     */
    private static HandRank threeOfAKindHelper(List<Card> sorted, int[] rankCounts) {
        int tripleVal = 0;
        List<Integer> kickers = new ArrayList<>();
        for (int r = 14; r >= 2; r--) {
            if (rankCounts[r] == 3 && tripleVal == 0) {
                tripleVal = r;
            } else if (rankCounts[r] == 1) {
                // potential kicker
                kickers.add(r);
            }
        }
        Collections.sort(kickers, Collections.reverseOrder());
        // triple + two best kickers
        List<Integer> ranks = new ArrayList<>();
        ranks.add(tripleVal);
        for (int i = 0; i < Math.min(2, kickers.size()); i++) {
            ranks.add(kickers.get(i));
        }
        return new HandRank(HandCategory.THREE_OF_A_KIND, ranks);
    }

    /**
     * Two pair => find top 2 pairs in descending order, then best kicker
     */
    private static HandRank twoPairHelper(List<Card> sorted, int[] rankCounts) {
        List<Integer> foundPairs = new ArrayList<>();
        int kicker = 0;
        // find all pairs top-down
        for (int r = 14; r >= 2; r--) {
            if (rankCounts[r] == 2) {
                foundPairs.add(r);
            } else if (rankCounts[r] == 1 && kicker == 0) {
                kicker = r; // first single rank we find from top => kicker
            }
        }
        // We only need the top 2 pairs if more are present
        foundPairs.sort((a, b) -> b - a);
        if (foundPairs.size() > 2) {
            foundPairs = foundPairs.subList(0, 2);
        }
        List<Integer> ranks = new ArrayList<>(foundPairs);
        ranks.add(kicker);
        return new HandRank(HandCategory.TWO_PAIR, ranks);
    }

    /**
     * One pair => find the highest pair once, then 3 kickers
     */
    private static HandRank onePairHelper(List<Card> sorted, int[] rankCounts) {
        int pairVal = 0;
        List<Integer> kickers = new ArrayList<>();
        for (int r = 14; r >= 2; r--) {
            if (rankCounts[r] == 2 && pairVal == 0) {
                // take only the first (highest) pair
                pairVal = r;
            } else if (rankCounts[r] == 1) {
                kickers.add(r);
            }
        }
        // sort kickers descending
        Collections.sort(kickers, Collections.reverseOrder());
        // pick top 3 kickers
        while (kickers.size() > 3) {
            kickers.remove(kickers.size() - 1);
        }
        List<Integer> ranks = new ArrayList<>();
        ranks.add(pairVal);
        ranks.addAll(kickers);
        return new HandRank(HandCategory.ONE_PAIR, ranks);
    }
}