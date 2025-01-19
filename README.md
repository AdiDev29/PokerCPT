# **Texas Hold'em Poker Project Documentation**

## **Table of Contents**
1. [Introduction](#introduction)
2. [Features](#features)
3. [Architecture Overview](#architecture-overview)
4. [Backend Components](#backend-components)
   - [Game Service](#game-service)
   - [Hand Evaluation](#hand-evaluation)
   - [WebSocket Communication](#websocket-communication)
5. [Frontend Components](#frontend-components)
   - [HTML/CSS](#htmlcss)
   - [JavaScript](#javascript)
6. [Game Flow](#game-flow)
7. [How to Run the Project](#how-to-run-the-project)
8. [Future Improvements](#future-improvements)

---

## **Introduction**
This project is a **real-time multiplayer Texas Hold'em poker game** built with a **Spring Boot backend** and a **vanilla HTML/CSS/JavaScript frontend**. It supports multiple players joining a shared table, making decisions (fold, call, raise), and competing for the pot using standard poker rules.

The game implements:
- Real-time updates using **WebSockets**.
- A robust poker hand evaluation algorithm.
- An interactive frontend showing player positions, actions, cards, and the game state.

---

## **Features**
- **Real-Time Gameplay**: All players see updates immediately (community cards, player turns, pot size).
- **Hand Evaluation**: The backend determines the winning hand using Texas Hold'em rules.
- **User-Friendly Interface**: A clean, responsive table design showing each player's name, stack, and actions.
- **Dynamic Game Flow**: Automatically progresses through betting rounds and deals the community cards.
- **Turn-Based Actions**: Players act one at a time, ensuring fairness.

---

## **Architecture Overview**

### **1. Backend**
The backend is built using **Java (Spring Boot)** and is responsible for:
- Managing the game state (players, bets, cards, turns).
- Broadcasting real-time updates via **WebSockets**.
- Evaluating hands using a **hand evaluator**.

### **2. Frontend**
The frontend is a **vanilla HTML/CSS/JavaScript** web client that:
- Displays the poker table, community cards, and player actions.
- Sends player actions (fold, call, raise) to the backend.
- Updates the UI in real time based on WebSocket messages.

---

## **Backend Components**

### **Game Service**
The `GameService` class handles the core game logic:
1. **Player Management**:
   - Adds players when they join the game.
   - Tracks their chip stacks, hole cards, and actions.
2. **Betting Rounds**:
   - Handles pre-flop, flop, turn, and river betting rounds.
   - Ensures all players match the highest bet before moving to the next round.
3. **Game Progression**:
   - Deals cards (hole cards and community cards).
   - Broadcasts updates (e.g., "new community card dealt").
   - Ends the game by evaluating hands and distributing the pot.

### **Hand Evaluation**
The `TexasHoldemHandEvaluator` is responsible for:
1. **Generating All Possible Hands**:
   - From a player's hole cards and the community cards, generate all 5-card combinations.
2. **Ranking Hands**:
   - Determines the best hand based on standard poker categories (Royal Flush, Straight, Pair, etc.).
3. **Tie-Breakers**:
   - Uses tiebreaker logic (e.g., highest pair, kicker) to determine the winner.

Hand categories:
- Royal Flush
- Straight Flush
- Four of a Kind
- Full House
- Flush
- Straight
- Three of a Kind
- Two Pair
- One Pair
- High Card

### **WebSocket Communication**
- The backend uses **Spring WebSocket** to send real-time updates to all clients.
- Endpoints:
  - `/topic/game-state`: Broadcasts the current game state to all players.
  - `/app/action`: Receives player actions (fold, call, raise) and processes them.

---

## **Frontend Components**

### **HTML/CSS**
- The main page has a **poker table** layout:
  - Six player seats positioned around the table.
  - Community cards displayed in the center.
  - A **pot information area** showing the total chips.
- Each seat shows:
  - Player's nickname.
  - Chip stack.
  - Hole cards (only visible to the current player).

### **JavaScript**
- Handles all **UI updates** and **WebSocket communication**:
  1. **Connecting to the Game**:
     - Players enter their nickname and join the table.
  2. **Real-Time Updates**:
     - Receives messages from `/topic/game-state` and updates the table.
  3. **Player Actions**:
     - Sends actions (fold, call, raise) to the backend via `/app/action`.

### **Frontend State Management**
- Keeps track of:
  - Which player is sitting in which seat.
  - The current turn (highlighted seat).
  - The cards on the table (hole cards and community cards).
  - The pot size.

---

## **Game Flow**
The game follows the standard Texas Hold'em flow:

1. **Joining the Game**:
   - Players enter their nickname and are seated at the table.
   - The game starts when there are at least 2 players.

2. **Pre-Flop**:
   - Each player is dealt 2 hole cards.
   - Players take turns acting (fold, call, raise).

3. **Flop**:
   - The first 3 community cards are dealt.
   - Another round of betting occurs.

4. **Turn**:
   - The 4th community card is dealt.
   - Another round of betting occurs.

5. **River**:
   - The 5th and final community card is dealt.
   - The last betting round occurs.

6. **Showdown**:
   - The backend evaluates all remaining players' hands and determines the winner.
   - Chips are distributed, and a new game can begin.

---

## **How to Run the Project**

### **Prerequisites**
1. Install **Java 17+**.
2. Install **Maven**.
3. Install **Node.js** (for serving static files if needed).

### **Steps**
1. Clone the repository:
   ```bash
   git clone https://github.com/your-repo/texas-holdem-poker.git
   cd texas-holdem-poker

2.	Build and run the backend:
    ```bash
    mvn spring-boot:run

The backend will start at http://localhost:8080.

3.	Open index.html in a browser to access the game.
4.	Open the game in multiple browser tabs to simulate multiple players.

Future Improvements
1.	Side Pots:
  Implement side pot logic for all-in scenarios.
2.	Authentication:
   Add user authentication to prevent duplicate nicknames.
3.	Better UI:
   Use a framework like React or Vue.js for a more dynamic frontend.
4.	Mobile Support:
   Optimize the UI for smaller screens.
5.	Spectator Mode:
   Allow players to watch the game without participating.

Conclusion

This Texas Hold’em Poker project demonstrates:
- Real-time multiplayer interaction.
- Complex backend logic for game state management and hand evaluation.
- A clean and intuitive frontend interface.

With additional features like side pots and authentication, this project could become a fully-fledged online poker platform.
