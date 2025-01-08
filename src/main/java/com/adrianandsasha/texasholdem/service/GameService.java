package com.adrianandsasha.texasholdem.service;

import com.adrianandsasha.texasholdem.dto.PlayerAction;
import com.adrianandsasha.texasholdem.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages the entire game flow using a turn-based approach (version with no timers).
 * it goes from PRE_FLOP -> FLOP -> TURN -> RIVER -> SHOWDOWN
 * with a betting round between each stage, using a "last-raiser" approach
 * to determine when betting ends (no forced time-based progression).
 */
@Service
public class GameService {
//variables for game
    private final Map<String, Player> players = new ConcurrentHashMap<>();
    private Deck deck;
    private List<Card> communityCards = new ArrayList<>();
    private int pot = 0;
    private final int bigBlind = 100;
    private final int smallBlind = 50;

    // We'll rotate dealer each hand (by index in a seat-ordered list).
    private int dealerIndex = 0;

    private boolean gameInProgress = false;
    private GameRound currentRound = GameRound.PRE_FLOP;

    // Turn-based data
    private List<Player> activePlayersThisHand = new ArrayList<>();
    private int actionIndex = 0;          // which seat in activePlayersThisHand is acting
    private int highestBetThisRound = 0;  // the largest bet any player has put in for this round

    // For a robust approach, we track the last raiser seat + how many still need to act
    private int lastRaiserIndex = 0;
    private int playersToAct = 0;         // how many players still need to act since the last raise

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    /**
     * A new player joins. If 2+ players exist and no hand is running, start a new hand.
     */
    public String addNewPlayer(String nickname) {
        String playerId = UUID.randomUUID().toString();
        Player p = new Player(playerId, nickname, 2000);
        players.put(playerId, p);

        // If 2+ players are present and no game is running, start a new hand
        if (!gameInProgress && players.size() >= 2) {
            startNewHand();
        }

        broadcastGameState();
        return playerId;
    }

    /**
     * Initiate a brand new hand:
     * 1) Shuffle deck, clear community
     * 2) Seat players in an order that starts with 'dealerIndex'
     * 3) Deal hole cards
     * 4) Post blinds
     * 5) Start pre-flop betting
     */
    private void startNewHand() {
        List<Player> all = new ArrayList<>(players.values());
        if (all.size() < 2) {
            return;
        }

        gameInProgress = true;
        currentRound = GameRound.PRE_FLOP;
        deck = new Deck();
        communityCards.clear();
        pot = 0;

        // Reset each player's state for the new hand
        for (Player p : all) {
            p.resetForNewHand();
        }

        // Rotate so that dealerIndex is at front
        dealerIndex = dealerIndex % all.size();
        Collections.rotate(all, -dealerIndex);

        // Everyone is active at the start
        activePlayersThisHand = new ArrayList<>(all);

        // Deal 2 hole cards each
        for (Player p : activePlayersThisHand) {
            p.receiveCard(deck.dealCard());
            p.receiveCard(deck.dealCard());
        }

        // Post blinds if >= 2 players
        highestBetThisRound = 0;
        if (activePlayersThisHand.size() >= 2) {
            // seat[1] => small blind
            Player sb = activePlayersThisHand.get(1 % activePlayersThisHand.size());
            int sbAmt = Math.min(smallBlind, sb.getChipStack());
            sb.setCurrentBet(sbAmt);
            sb.setChipStack(sb.getChipStack() - sbAmt);
            pot += sbAmt;
            highestBetThisRound = sbAmt;

            // seat[2] => big blind
            if (activePlayersThisHand.size() >= 3) {
                Player bb = activePlayersThisHand.get(2 % activePlayersThisHand.size());
                int bbAmt = Math.min(bigBlind, bb.getChipStack());
                bb.setCurrentBet(bbAmt);
                bb.setChipStack(bb.getChipStack() - bbAmt);
                pot += bbAmt;
                highestBetThisRound = bbAmt;

                // big blind is the "last raiser"
                lastRaiserIndex = 2;
                playersToAct = activePlayersThisHand.size() - 1;
            } else {
                // Heads up scenario => seat[1] is big blind effectively
                lastRaiserIndex = 1;
                playersToAct = activePlayersThisHand.size() - 1;
            }
        }

        // If we have 3+ players, action starts at seat[3]. If 2 players, seat[0] is next to act.
        if (activePlayersThisHand.size() > 2) {
            actionIndex = 3 % activePlayersThisHand.size();
        } else {
            actionIndex = 0;
        }

        broadcastGameState();
        doBettingRound();
    }

    /**
     * The main betting round logic. If playersToAct == 0 or only 1 player remains, the round ends.
     */
    private void doBettingRound() {
        // If only 1 remains => end
        if (activePlayersThisHand.size() == 1) {
            finishHandEarly(activePlayersThisHand.get(0));
            return;
        }

        // If no one else needs to act, the round is done
        if (playersToAct <= 0) {
            advanceRound();
            return;
        }

        // Otherwise, broadcast state so the current player knows to act
        broadcastGameState();
        // We "pause" here until that player acts (handlePlayerAction calls continueBettingRound)
    }

    /**
     * The game continues after a player's action. We move to the next seat and re-check the round.
     */
    private void continueBettingRound() {
        if (activePlayersThisHand.size() == 1) {
            finishHandEarly(activePlayersThisHand.get(0));
            return;
        }

        actionIndex = (actionIndex + 1) % activePlayersThisHand.size();
        doBettingRound();
    }

    /**
     * Move from PRE_FLOP -> FLOP -> TURN -> RIVER -> SHOWDOWN
     * dealing community cards & starting a new betting round at each step.
     */
    private void advanceRound() {
        switch (currentRound) {
            case PRE_FLOP:
                // Deal FLOP
                communityCards.add(deck.dealCard());
                communityCards.add(deck.dealCard());
                communityCards.add(deck.dealCard());
                currentRound = GameRound.FLOP;
                break;
            case FLOP:
                // Deal TURN
                communityCards.add(deck.dealCard());
                currentRound = GameRound.TURN;
                break;
            case TURN:
                // Deal RIVER
                communityCards.add(deck.dealCard());
                currentRound = GameRound.RIVER;
                break;
            case RIVER:
                // Move to SHOWDOWN
                currentRound = GameRound.SHOWDOWN;
                doShowdown();
                return; // end
            default:
                return; // shouldn't happen
        }

        // Setup for next betting round
        highestBetThisRound = 0;
        for (Player p : activePlayersThisHand) {
            p.setCurrentBet(0);
        }

        // In real hold'em, post-flop action starts at seat[1] relative to the button
        actionIndex = 1 % activePlayersThisHand.size();

        // The first bet is effectively 0 => no raiser
        lastRaiserIndex = actionIndex;
        playersToAct = activePlayersThisHand.size();

        broadcastGameState();
        doBettingRound();
    }

    /**
     * Showdown: if 2+ not folded, evaluate; if 1 left, they auto-win.
     */
    private void doShowdown() {
        if (activePlayersThisHand.size() == 1) {
            finishHandEarly(activePlayersThisHand.get(0));
            return;
        }

        // Check how many remain not folded
        List<Player> notFolded = new ArrayList<>();
        for (Player p : activePlayersThisHand) {
            if (!p.isFolded()) notFolded.add(p);
        }
        if (notFolded.size() == 1) {
            finishHandEarly(notFolded.get(0));
            return;
        }
        // Evaluate
        List<ShowdownResult> results = new ArrayList<>();
        for (Player p : notFolded) {
            List<Card> combined = new ArrayList<>(communityCards);
            combined.addAll(p.getHoleCards());
            HandRank rank = TexasHoldemHandEvaluator.getBestHandRank(combined);
            results.add(new ShowdownResult(p, rank));
        }

        results.sort((r1, r2) -> {
            int catCompare = r2.getRank().getCategory().ordinal()
                    - r1.getRank().getCategory().ordinal();
            if (catCompare != 0) return catCompare;
            return tiebreakComparison(r1.getRank().getRanks(), r2.getRank().getRanks());
        });

        ShowdownResult best = results.get(0);
        HandCategory bestCat = best.getRank().getCategory();
        List<Player> winners = new ArrayList<>();
        winners.add(best.getPlayer());

        // Ties
        for (int i = 1; i < results.size(); i++) {
            ShowdownResult sr = results.get(i);
            if (sr.getRank().getCategory() == bestCat) {
                if (tiebreakComparison(best.getRank().getRanks(), sr.getRank().getRanks()) == 0) {
                    winners.add(sr.getPlayer());
                } else {
                    break;
                }
            } else {
                break;
            }
        }

        if (winners.size() == 1) {
            winners.get(0).setChipStack(winners.get(0).getChipStack() + pot);
        } else {
            int split = pot / winners.size();
            for (Player w : winners) {
                w.setChipStack(w.getChipStack() + split);
            }
            int remainder = pot % winners.size();
            if (remainder > 0) {
                winners.get(0).setChipStack(winners.get(0).getChipStack() + remainder);
            }
        }

        endHand();
    }

    /**
     * If only 1 remains mid-round, we finish the hand.
     */
    private void finishHandEarly(Player winner) {
        winner.setChipStack(winner.getChipStack() + pot);
        endHand();
    }

    private void endHand() {
        pot = 0;
        gameInProgress = false;

        // Move dealer for next hand
        dealerIndex = (dealerIndex + 1) % players.size();

        broadcastGameState();

        // Remove bankrupt players
        players.values().removeIf(pl -> pl.getChipStack() <= 0);

        // Start next if still >= 2
        if (players.size() >= 2) {
            startNewHand();
        }
    }

    /**
     * The main action handler. We reduce calls, raises, folds, or checks. Then continue the round.
     */
    public void handlePlayerAction(PlayerAction action) {
        Player p = players.get(action.getPlayerId());
        if (p == null) {
            broadcastGameState();
            return;
        }

        if (!gameInProgress) {
            broadcastGameState();
            return;
        }

        // Must match the current seat
        Player current = activePlayersThisHand.get(actionIndex);
        if (!current.getId().equals(p.getId())) {
            // Not your turn
            broadcastGameState();
            return;
        }

        switch (action.getActionType().toUpperCase()) {
            case "FOLD":
                p.fold();
                activePlayersThisHand.removeIf(Player::isFolded);
                if (activePlayersThisHand.size() == 1) {
                    finishHandEarly(activePlayersThisHand.get(0));
                    return;
                }
                // If we fold, we do playersToAct -= 1 so we don't block
                playersToAct -= 1;
                break;

            case "CALL": {
                int needed = highestBetThisRound - p.getCurrentBet();
                int callAmount = Math.min(needed, p.getChipStack());
                p.setCurrentBet(p.getCurrentBet() + callAmount);
                p.setChipStack(p.getChipStack() - callAmount);
                pot += callAmount;
                playersToAct -= 1;
                break;
            }

            case "RAISE": {
                int neededToCall = highestBetThisRound - p.getCurrentBet();
                int raiseAmount = action.getAmount();
                if (raiseAmount < neededToCall) {
                    // minimal fix: treat as call
                    raiseAmount = neededToCall;
                }
                if (raiseAmount > p.getChipStack()) {
                    // all in
                    raiseAmount = p.getChipStack();
                }

                int increment = raiseAmount;
                p.setCurrentBet(p.getCurrentBet() + increment);
                p.setChipStack(p.getChipStack() - increment);
                pot += increment;

                if (p.getCurrentBet() > highestBetThisRound) {
                    highestBetThisRound = p.getCurrentBet();

                    // new raise => reset playersToAct
                    playersToAct = activePlayersThisHand.size() - 1;
                    lastRaiserIndex = actionIndex;
                }
                break;
            }

            case "CHECK": {
                // valid only if p.getCurrentBet() == highestBetThisRound
                if (p.getCurrentBet() < highestBetThisRound) {
                    // not valid => fold or something else
                    p.fold();
                    activePlayersThisHand.removeIf(Player::isFolded);
                    if (activePlayersThisHand.size() == 1) {
                        finishHandEarly(activePlayersThisHand.get(0));
                        return;
                    }
                } else {
                    // matched the bet with 0 extra
                    playersToAct -= 1;
                }
                break;
            }
        }

        broadcastGameState();
        continueBettingRound();
    }

    /**
     * Utility for comparing tiebreaker ranks.
     */
    private int tiebreakComparison(List<Integer> r1, List<Integer> r2) {
        for (int i = 0; i < Math.min(r1.size(), r2.size()); i++) {
            if (r1.get(i) > r2.get(i)) return 1;
            if (r1.get(i) < r2.get(i)) return -1;
        }
        return 0;
    }

    /**
     * Return a JSON-friendly snapshot of the entire game state.
     */
    public Map<String, Object> getGameState() {
        Map<String, Object> state = new HashMap<>();
        state.put("communityCards", communityCards);
        state.put("pot", pot);
        state.put("players", players.values());
        state.put("gameInProgress", gameInProgress);
        state.put("currentRound", currentRound);

        // We also provide the seat order for this hand
        List<String> activeIds = new ArrayList<>();
        for (Player p : activePlayersThisHand) {
            activeIds.add(p.getId());
        }
        state.put("activePlayersThisHand", activeIds);
        state.put("actionIndex", actionIndex);

        return state;
    }

    private void broadcastGameState() {
        messagingTemplate.convertAndSend("/topic/game-state", getGameState());
    }

    // For showdown
    private static class ShowdownResult {
        private final Player player;
        private final HandRank rank;

        public ShowdownResult(Player p, HandRank r) {
            player = p;
            rank = r;
        }

        public Player getPlayer() { return player; }
        public HandRank getRank() { return rank; }
    }
}