let playerId = null;
let stompClient = null;

// We'll keep track of which player is in which seat
let seatAssignments = new Array(6).fill(null);

document.addEventListener("DOMContentLoaded", () => {
    const joinBtn = document.getElementById("joinBtn");
    joinBtn.addEventListener("click", joinGame);
});

// 1) A helper function to build the correct image filename, e.g. "AS.png" for Ace of Spades
function getCardFilename(rank, suit) {
    // Convert rank from "TWO", "THREE", "ACE", etc. to the typical short code
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

    // Convert suit from "HEARTS", "DIAMONDS", "CLUBS", "SPADES" to a single letter
    const suitMap = {
        "HEARTS": "H",
        "DIAMONDS": "D",
        "CLUBS": "C",
        "SPADES": "S"
    };

    const r = rankMap[rank] || "X";
    const s = suitMap[suit] || "X";
    // returns something like "AS.png"
    return r + s + ".png";
}

function joinGame() {
    const nickname = document.getElementById("nickname").value.trim();
    if (!nickname) {
        alert("Please enter a nickname.");
        return;
    }
    fetch(`/api/join?nickname=${encodeURIComponent(nickname)}`, {
        method: "POST"
    })
        .then(response => response.text())
        .then(id => {
            playerId = id;

            seatAssignments[0] = playerId;

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

        stompClient.subscribe("/topic/game-state", function(message) {
            if (message.body) {
                const gameState = JSON.parse(message.body);
                updateGameUI(gameState);
            }
        });

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

function updateGameUI(gameState) {
    // Clear seat assignments except seat 0
    const localId = seatAssignments[0];
    seatAssignments.fill(null);
    seatAssignments[0] = localId;

    const allPlayers = gameState.players || [];
    const localPlayerObj = allPlayers.find(p => p.id === localId);
    const otherPlayers = allPlayers.filter(p => p.id !== localId);

    // seat 1..5 for others
    let seatIndex = 1;
    for (let p of otherPlayers) {
        if (seatIndex < 6) {
            seatAssignments[seatIndex] = p.id;
            seatIndex++;
        }
    }

    // Render seats
    for (let i = 0; i < 6; i++) {
        const pid = seatAssignments[i];
        const infoDiv = document.getElementById(`player-info-${i}`);
        const cardsDiv = document.getElementById(`player-cards-${i}`);
        infoDiv.innerHTML = "";
        cardsDiv.innerHTML = "";

        if (!pid) continue;
        const thisPlayer = allPlayers.find(pl => pl.id === pid);
        if (!thisPlayer) continue;

        let infoText = `${thisPlayer.nickname} | ${thisPlayer.chipStack} chips`;
        if (thisPlayer.folded) {
            infoText += " (FOLDED)";
        }
        infoDiv.textContent = infoText;

        // Show hole cards if local
        if (thisPlayer.id === localId) {
            for (let c of thisPlayer.holeCards) {
                const cardDiv = document.createElement("div");
                cardDiv.className = "card";

                const filename = getCardFilename(c.rank, c.suit);
                // e.g. "AH.png", "10D.png" => /cards/AH.png
                const img = document.createElement("img");
                img.src = `/cards/${filename}`;

                cardDiv.appendChild(img);
                cardsDiv.appendChild(cardDiv);
            }
        }
    }

    // Community cards
    const potDiv = document.getElementById("potInfo");
    potDiv.innerHTML = `<h3>Pot: ${gameState.pot || 0}</h3>`;

    const ccDiv = document.getElementById("communityCards");
    ccDiv.innerHTML = "";
    if (gameState.communityCards) {
        gameState.communityCards.forEach(card => {
            const cardDiv = document.createElement("div");
            cardDiv.className = "card";

            const filename = getCardFilename(card.rank, card.suit);
            const img = document.createElement("img");
            img.src = `/cards/${filename}`;

            cardDiv.appendChild(img);
            ccDiv.appendChild(cardDiv);
        });
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