let playerId = null;
let stompClient = null;

// We'll store up to 6 seats in exact turn order from the server
let seatAssignments = new Array(6).fill(null);

document.addEventListener("DOMContentLoaded", () => {
    document.getElementById("joinBtn").addEventListener("click", joinGame);
});


//Helper to map rank/suit strings to filenames, e.g. "ACE" + "SPADES" => "AS.png"
function getCardFilename(rank, suit) {
    const rankMap = {
        "TWO": "2",
        "THREE": "3",
        "FOUR": "4",
        "FIVE": "5",
        "SIX": "6",
        "SEVEN": "7",
        "EIGHT": "8",
        "NINE": "9",
        "TEN": "10",
        "JACK": "J",
        "QUEEN": "Q",
        "KING": "K",
        "ACE": "A"
    };
    const suitMap = {
        "HEARTS": "H",
        "DIAMONDS": "D",
        "CLUBS": "C",
        "SPADES": "S"
    };

    const r = rankMap[rank] || "X";
    const s = suitMap[suit] || "X";
    return r + s + ".png"; // e.g. "AS.png"
}

function joinGame() {
    const nickname = document.getElementById("nickname").value.trim();
    if (!nickname) {
        alert("Please enter a nickname.");
        return;
    }

    fetch(`/api/join?nickname=${encodeURIComponent(nickname)}`, { method: "POST" })
        .then(response => response.text())
        .then(id => {
            playerId = id;

            // Hide join, show game
            document.getElementById("join-section").style.display = "none";
            document.getElementById("game-section").style.display = "block";

            connectWebSocket();
        })
        .catch(err => console.error("Join error:", err));
}

function connectWebSocket() {
    const socket = new SockJS('/ws');
    stompClient = Stomp.over(socket);
    stompClient.connect({}, function(frame) {
        console.log("Connected:", frame);

        // Subscribe to game state broadcasts
        stompClient.subscribe("/topic/game-state", function(message) {
            if (message.body) {
                const gameState = JSON.parse(message.body);
                updateGameUI(gameState);
            }
        });

        // Fetch current state immediately
        fetchState();
    });
}

function fetchState() {
    fetch("/api/state")
        .then(res => res.json())
        .then(state => {
            updateGameUI(state);
        })
        .catch(err => console.log("State fetch error", err));
}

/**
 * Renders the entire game UI based on the given gameState object
 * from the server. We seat players in the order found in
 * gameState.activePlayersThisHand, i.e. seat 0 => first, seat 1 => second, etc.
 * actionIndex determines whose turn it is.
 */
function updateGameUI(gameState) {
    // 1. Seat players in turn order from activePlayersThisHand
    seatAssignments.fill(null);
    const activeIds = gameState.activePlayersThisHand || [];

    // Example: if activePlayersThisHand = ["pA","pB","pC"], seat 0 => pA, seat 1 => pB, seat 2 => pC
    for (let i = 0; i < activeIds.length && i < 6; i++) {
        seatAssignments[i] = activeIds[i];
    }

    // 2. Render seats
    const allPlayers = gameState.players || [];

    for (let i = 0; i < 6; i++) {
        const seatDiv = document.getElementById(`seat-${i}`);
        const infoDiv = document.getElementById(`player-info-${i}`);
        const cardsDiv = document.getElementById(`player-cards-${i}`);

        // Clear old content/borders
        seatDiv.style.removeProperty("border");
        infoDiv.innerHTML = "";
        cardsDiv.innerHTML = "";

        const playerIdInSeat = seatAssignments[i];
        if (!playerIdInSeat) continue;

        // Find the corresponding player object
        const thisPlayer = allPlayers.find(pl => pl.id === playerIdInSeat);
        if (!thisPlayer) continue;

        // Build info text
        let infoText = `${thisPlayer.nickname} | ${thisPlayer.chipStack} chips`;
        if (thisPlayer.folded) {
            infoText += " (FOLDED)";
        }
        infoDiv.textContent = infoText;

        // Hole cards if it's me
        if (thisPlayer.id === playerId) {
            for (let c of thisPlayer.holeCards) {
                const cardDiv = document.createElement("div");
                cardDiv.className = "card";

                const filename = getCardFilename(c.rank, c.suit);
                const img = document.createElement("img");
                img.src = `/cards/${filename}`;

                cardDiv.appendChild(img);
                cardsDiv.appendChild(cardDiv);
            }
        }
    }

    // 3. Pot + Community Cards
    const potDiv = document.getElementById("potInfo");
    potDiv.innerHTML = `<h3>Pot: ${gameState.pot || 0}</h3>`;

    const ccDiv = document.getElementById("communityCards");
    ccDiv.innerHTML = "";
    if (gameState.communityCards) {
        for (let card of gameState.communityCards) {
            const cardDiv = document.createElement("div");
            cardDiv.className = "card";

            const filename = getCardFilename(card.rank, card.suit);
            const img = document.createElement("img");
            img.src = `/cards/${filename}`;

            cardDiv.appendChild(img);
            ccDiv.appendChild(cardDiv);
        }
    }

    // 4. Highlight current turn
    // If the server sets: actionIndex => which seat is up
    // e.g. actionIndex=0 => seat 0 is current turn
    if (gameState.actionIndex != null) {
        const actingSeat = gameState.actionIndex;
        if (actingSeat >= 0 && actingSeat < 6) {
            const seatDiv = document.getElementById(`seat-${actingSeat}`);
            if (seatDiv) {
                seatDiv.style.border = "2px solid yellow";
            }
        }
    }
}

/** Actions */
function fold() {
    sendAction("FOLD", 0);
}
function call() {
    sendAction("CALL", 0);
}
function raise() {
    const amt = prompt("Enter raise amount:");
    const num = parseInt(amt);
    if (!isNaN(num) && num > 0) {
        sendAction("RAISE", num);
    }
}
function check() {
    sendAction("CHECK", 0);
}

function sendAction(actionType, amount) {
    if (!stompClient) return;
    stompClient.send("/app/action", {}, JSON.stringify({
        playerId: playerId,
        actionType: actionType,
        amount: amount
    }));
}