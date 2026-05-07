package server;

import shared.GameState;
import java.util.List;
import java.util.Set;
import java.util.Collections;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Represents a game room with players, state, and scores.
 * Thread-safe: accessed by multiple ClientHandler threads + timer thread.
 */
public class Room {
    private final String roomCode;
    private final List<ClientHandler> players = new CopyOnWriteArrayList<>();
    private final ConcurrentHashMap<String, AtomicInteger> scores = new ConcurrentHashMap<>();
    private final Set<String> guessedPlayers = Collections.synchronizedSet(new HashSet<>());
    private final Set<String> readyPlayers = Collections.synchronizedSet(new HashSet<>());

    private GameController gameController; // shared by all players in this room

    private volatile String hostUsername;
    private volatile GameState state = GameState.LOBBY;
    private volatile String currentWord;
    private volatile String currentDrawer;
    private volatile java.util.List<String> currentChoices = java.util.Collections.emptyList();
    private volatile int currentRound = 0;
    private volatile int totalRounds = 0;
    private volatile int secondsLeft = 0;

    public Room(String roomCode) {
        this.roomCode = roomCode;
    }

    // ═══ Player Management ═══

    public void addPlayer(ClientHandler player) {
        players.add(player);
        scores.putIfAbsent(player.getUsername(), new AtomicInteger(0));
        if (players.size() == 1) {
            hostUsername = player.getUsername();
        }
    }

    public void removePlayer(ClientHandler player) {
        players.remove(player);
        readyPlayers.remove(player.getUsername());
        if (player.getUsername().equals(hostUsername) && !players.isEmpty()) {
            hostUsername = players.get(0).getUsername();
        }
    }

    /** Toggle a player's ready status. Returns the new state. */
    public boolean toggleReady(String username) {
        if (readyPlayers.contains(username)) {
            readyPlayers.remove(username);
            return false;
        } else {
            readyPlayers.add(username);
            return true;
        }
    }

    public boolean isReady(String username) {
        return readyPlayers.contains(username);
    }

    /** All non-host players must be ready (host is implicitly ready as the one starting). */
    public boolean allNonHostReady() {
        for (ClientHandler p : players) {
            String name = p.getUsername();
            if (!name.equals(hostUsername) && !readyPlayers.contains(name)) {
                return false;
            }
        }
        return players.size() >= 2;
    }

    public void clearReady() {
        readyPlayers.clear();
    }

    /** Build "user1:1,user2:0,user3:1" status string. */
    public String getReadyStatus() {
        StringBuilder sb = new StringBuilder();
        for (ClientHandler p : players) {
            if (sb.length() > 0) sb.append(",");
            String name = p.getUsername();
            // Host is always shown as ready
            int state = (name.equals(hostUsername) || readyPlayers.contains(name)) ? 1 : 0;
            sb.append(name).append(":").append(state);
        }
        return sb.toString();
    }

    public int getGuesserCount() {
        // Everyone except the drawer
        return (int) players.stream()
            .filter(p -> !p.getUsername().equals(currentDrawer))
            .count();
    }

    // ═══ Round Management ═══

    public void startNewRound(String drawer, String word) {
        this.currentDrawer = drawer;
        this.currentWord = word;
        this.guessedPlayers.clear();
        this.state = GameState.DRAWING_ACTIVE;
    }

    public void addCorrectGuess(String username) {
        guessedPlayers.add(username);
    }

    public boolean hasEveryoneGuessed() {
        return guessedPlayers.size() >= getGuesserCount();
    }

    /** Build score summary string: "alice:120,bob:85,charlie:50" */
    public String getScoreSummary() {
        StringBuilder sb = new StringBuilder();
        for (ClientHandler p : players) {
            if (sb.length() > 0) sb.append(",");
            sb.append(p.getUsername()).append(":").append(getScore(p.getUsername()));
        }
        return sb.toString();
    }

    // ═══ Getters / Setters ═══

    public GameController getGameController()          { return gameController; }
    public void setGameController(GameController gc)   { this.gameController = gc; }

    /** Release timer thread + game state when the room is being disposed. */
    public void shutdown() {
        if (gameController != null) gameController.shutdown();
    }

    public java.util.List<String> getCurrentChoices()  { return currentChoices; }
    public void setCurrentChoices(java.util.List<String> choices) { this.currentChoices = choices; }

    public void setCurrentDrawer(String drawer) { this.currentDrawer = drawer; }

    public String getRoomCode()       { return roomCode; }
    public List<ClientHandler> getPlayers() { return players; }
    public String getHostUsername()    { return hostUsername; }
    public GameState getState()       { return state; }
    public String getCurrentWord()    { return currentWord; }
    public String getCurrentDrawer()  { return currentDrawer; }
    public int getCurrentRound()      { return currentRound; }
    public int getTotalRounds()       { return totalRounds; }
    public int getSecondsLeft()       { return secondsLeft; }
    public Set<String> getGuessedPlayers() { return guessedPlayers; }

    public void setState(GameState state)       { this.state = state; }
    public void setCurrentRound(int round)      { this.currentRound = round; }
    public void setTotalRounds(int total)       { this.totalRounds = total; }
    public void setSecondsLeft(int seconds)     { this.secondsLeft = seconds; }

    public int getScore(String username) {
        AtomicInteger s = scores.get(username);
        return s != null ? s.get() : 0;
    }

    public void addScore(String username, int points) {
        scores.computeIfAbsent(username, k -> new AtomicInteger(0)).addAndGet(points);
    }

    public String getPlayerList() {
        StringBuilder sb = new StringBuilder();
        for (ClientHandler p : players) {
            if (sb.length() > 0) sb.append(",");
            sb.append(p.getUsername());
        }
        return sb.toString();
    }

    public boolean isEmpty() {
        return players.isEmpty();
    }
}
