package com.adrianandsasha.texasholdem.controller;

import com.adrianandsasha.texasholdem.dto.PlayerAction;
import com.adrianandsasha.texasholdem.service.GameService;
import com.adrianandsasha.texasholdem.model.Player;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
public class GameController {

    @Autowired
    private GameService gameService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    //REST endpoint: a new player with nickname joins the table to spend their life savings in poker.
    //Returns the playerId that is assigned  (UUID or something).
    @PostMapping("/api/join")
    public String join(@RequestParam String nickname) {
        String playerId = gameService.addNewPlayer(nickname);
        return playerId;
    }

//WebSocket listener: handle a player's actions (fold (if coward), call (wants to be like their friends), raise (real gambler, makes betMGM proud), etc.).
// Because we might send actions from the client to /app/action.
    @MessageMapping("/action")
    public void playerAction(PlayerAction action) {
        gameService.handlePlayerAction(action);
        // Broadcast the updated game state to everyone
        messagingTemplate.convertAndSend("/topic/game-state", gameService.getGameState());
    }

// optional but ill probably use it for debugging to get the game state
    @GetMapping("/api/state")
    public Object getState() {
        return gameService.getGameState();
    }
}