package com.adrianandsasha.texasholdem.dto;
//Players moving
public class PlayerAction {
    private String playerId; // which player is acting
    private String actionType; // "FOLD", "CALL", "RAISE", "CHECK", etc. (specified in ENUM)
    private int amount;       // used if actionType = "RAISE"

    public PlayerAction() {}

    public String getPlayerId() {
        return playerId;
    }
    public void setPlayerId(String playerId) {
        this.playerId = playerId;
    }

    public String getActionType() {
        return actionType;
    }
    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public int getAmount() {
        return amount;
    }
    public void setAmount(int amount) {
        this.amount = amount;
    }
}
