package server;

import shared.GameState;
import java.util.List;
import java.util.concurrent.*;

/**
 * Core game logic — round management, timer, scoring, word matching.
 * All state-mutating methods synchronize on the Room object.
 */
public class GameController {
    private static final int ROUND_SECONDS = 80;
    private static final int WORD_PICK_SECONDS = 10;
    private static final int ROUND_END_SECONDS = 5;

    private final Broadcaster broadcaster;
    private final WordBank wordBank;
    private final server.db.UserDAO userDAO;
    private ScheduledExecutorService timerExecutor;
    private ScheduledFuture<?> timerTask;     // recurring timer (round countdown / word-pick auto)
    private ScheduledFuture<?> delayedTask;   // one-shot delayed task (round-end → next round, game-end → game-over)

    private int drawerIndex = -1;

    public GameController(Broadcaster broadcaster, WordBank wordBank, server.db.UserDAO userDAO) {
        this.broadcaster = broadcaster;
        this.wordBank = wordBank;
        this.userDAO = userDAO;
        this.timerExecutor = Executors.newSingleThreadScheduledExecutor();
    }

    /** Release the timer thread when the room is disposed (prevents thread leak). */
    public void shutdown() {
        cancelTimer();
        cancelDelayedTask();
        timerExecutor.shutdownNow();
    }

    // ═══ Game Start ═══

    public void startGame(Room room) {
        synchronized (room) {
            wordBank.reset();
            drawerIndex = -1;
            room.setTotalRounds(0); // Infinite — increments each round
            room.setCurrentRound(0);
            startNextRound(room);
        }
    }

    // ═══ Round Flow ═══

    private void startNextRound(Room room) {
        synchronized (room) {
            if (room.getPlayers().size() < 2) {
                endGame(room);
                return;
            }

            drawerIndex++;
            int round = drawerIndex + 1;
            room.setCurrentRound(round);
            room.setTotalRounds(round); // Update display total to current

            // Cycle through players
            ClientHandler drawer = room.getPlayers().get(drawerIndex % room.getPlayers().size());
            String drawerName = drawer.getUsername();

            // Pick 3 word choices and remember them so we can validate the drawer's pick
            List<String> choices = wordBank.getThreeChoices();
            room.setCurrentChoices(choices);
            room.setCurrentDrawer(drawerName); // authoritative drawer for this round
            room.setState(GameState.WORD_SELECTION);

            // Tell everyone a new round started
            broadcaster.toAll(room, String.format("NEW_ROUND|%d|%d|%s|0",
                round, room.getTotalRounds(), drawerName));

            // Send word choices only to drawer
            broadcaster.toPlayer(room, String.format("WORD_CHOICES|%s|%s|%s",
                choices.get(0), choices.get(1), choices.get(2)), drawerName);

            // Auto-pick if drawer doesn't choose in 10s
            startTimer(room, WORD_PICK_SECONDS, () -> {
                synchronized (room) {
                    if (room.getState() == GameState.WORD_SELECTION) {
                        handleWordChosen(room, drawerName, choices.get(0)); // Auto-pick first word
                    }
                }
            });
        }
    }

    public void handleWordChosen(Room room, String requesterUsername, String word) {
        synchronized (room) {
            if (room.getState() != GameState.WORD_SELECTION) return;
            // Authority check: only the assigned drawer can pick the word
            if (!requesterUsername.equals(room.getCurrentDrawer())) return;
            // Validity check: word must be one of the offered choices
            if (!room.getCurrentChoices().contains(word)) return;
            cancelTimer();

            room.startNewRound(requesterUsername, word);
            room.setSecondsLeft(ROUND_SECONDS);

            // Send word hint to guessers (underscores)
            String hint = word.chars()
                .mapToObj(c -> c == ' ' ? "  " : "_ ")
                .reduce("", String::concat).trim();
            broadcaster.toAllExcept(room, "WORD_HINT|" + hint, requesterUsername);

            // Start round timer
            startTimer(room, 1, new Runnable() {
                public void run() {
                    synchronized (room) {
                        if (room.getState() != GameState.DRAWING_ACTIVE) return;
                        int left = room.getSecondsLeft() - 1;
                        room.setSecondsLeft(left);
                        broadcaster.toAll(room, "TIMER_UPDATE|" + left);

                        if (left <= 0) {
                            cancelTimer();
                            endRound(room);
                        }
                    }
                }
            });
        }
    }

    // ═══ Guess Handling ═══

    public void handleGuess(Room room, String username, String guess, Broadcaster bc) {
        synchronized (room) {
            if (room.getState() != GameState.DRAWING_ACTIVE) {
                bc.toPlayer(room, "SYSTEM_MSG|Please wait for the drawer to pick a word.", username);
                return;
            }
            if (username.equals(room.getCurrentDrawer())) {
                bc.toPlayer(room, "SYSTEM_MSG|You are drawing, you can't guess!", username);
                return;
            }
            // Already guessed correctly — route as private chat (visible to drawer + winners only, excluding sender)
            if (room.getGuessedPlayers().contains(username)) {
                bc.toGuessedGroup(room, "GUESS_CHAT|" + username + "|" + guess, username);
                return;
            }

            if (guess.equalsIgnoreCase(room.getCurrentWord())) {
                // Correct guess
                int points = calculateGuesserScore(room.getSecondsLeft(), ROUND_SECONDS);
                room.addCorrectGuess(username);
                room.addScore(username, points);

                // Drawer earns 30% of guesser score (skribbl-style: faster guess = more drawer points)
                int drawerBonus = (int) Math.round(points * 0.3);
                room.addScore(room.getCurrentDrawer(), drawerBonus);

                broadcaster.toAll(room, "GUESS_CORRECT|" + username + "|" + points);

                if (room.hasEveryoneGuessed()) {
                    cancelTimer();
                    endRound(room);
                }
            } else if (isCloseGuess(guess, room.getCurrentWord())) {
                broadcaster.toPlayer(room, "GUESS_CLOSE", username);
                broadcaster.toAll(room, "GUESS_WRONG|" + username + "|" + guess);
            } else {
                broadcaster.toAll(room, "GUESS_WRONG|" + username + "|" + guess);
            }
        }
    }

    // ═══ Round / Game End ═══

    private void endRound(Room room) {
        synchronized (room) {
            // Guard: if game already ended (or this round was already ended), don't fire again
            if (room.getState() == GameState.GAME_ENDING
                || room.getState() == GameState.GAME_OVER
                || room.getState() == GameState.ROUND_ENDING) return;

            room.setState(GameState.ROUND_ENDING);
            broadcaster.toAll(room, "ROUND_END|" + room.getCurrentWord() + "|" + room.getScoreSummary());

            // Wait then start next round — track the future so we can cancel if game ends early
            cancelDelayedTask();
            delayedTask = timerExecutor.schedule(() -> startNextRound(room), ROUND_END_SECONDS, TimeUnit.SECONDS);
        }
    }

    private void endGame(Room room) {
        synchronized (room) {
            // Guard: prevent double game-end (e.g. timer + disconnect both triggering)
            if (room.getState() == GameState.GAME_ENDING || room.getState() == GameState.GAME_OVER) return;

            // Cancel any pending delayed transitions (e.g. queued startNextRound from endRound)
            cancelTimer();
            cancelDelayedTask();

            room.setState(GameState.GAME_ENDING);
            broadcaster.toAll(room, "GAME_END|" + room.getScoreSummary());

            // Persist each player's final score (updates high_score, increments games_played)
            for (ClientHandler p : room.getPlayers()) {
                userDAO.updateScore(p.getUsername(), room.getScore(p.getUsername()));
            }

            // After a delay, transition to GAME_OVER
            delayedTask = timerExecutor.schedule(() -> {
                synchronized (room) {
                    room.setState(GameState.GAME_OVER);
                }
            }, ROUND_END_SECONDS, TimeUnit.SECONDS);
        }
    }

    private void cancelDelayedTask() {
        if (delayedTask != null && !delayedTask.isDone()) {
            delayedTask.cancel(false);
        }
        delayedTask = null;
    }

    // ═══ Player Disconnect ═══

    public void handleDisconnect(Room room, ClientHandler handler) {
        synchronized (room) {
            String username = handler.getUsername();
            boolean wasDrawer = username.equals(room.getCurrentDrawer());
            boolean wasHost = username.equals(room.getHostUsername());

            room.removePlayer(handler);
            broadcaster.toAll(room, "PLAYER_LEFT|" + username);

            if (wasHost && !room.isEmpty()) {
                broadcaster.toAll(room, "HOST_CHANGED|" + room.getHostUsername());
            }

            if (room.getPlayers().size() < 2 && room.getState() != GameState.LOBBY) {
                cancelTimer();
                endGame(room);
                return;
            }

            // Drawer disconnects during WORD_SELECTION → cancel auto-pick timer and skip to next round
            // (without this, the queued auto-pick would call handleWordChosen for an absent drawer
            //  and the round would hang for 80s with nobody actually drawing)
            if (wasDrawer && room.getState() == GameState.WORD_SELECTION) {
                cancelTimer();
                broadcaster.toAll(room, "SYSTEM_MSG|" + username + " disconnected before picking. Skipping round.");
                // Use ROUND_ENDING (not LOBBY) during the brief skip transition
                // so the room is NOT joinable by new players in the meantime.
                // startNextRound() will set the state back to WORD_SELECTION when it runs.
                room.setState(GameState.ROUND_ENDING);
                cancelDelayedTask();
                delayedTask = timerExecutor.schedule(() -> startNextRound(room), 1, TimeUnit.SECONDS);
                return;
            }

            if (wasDrawer && room.getState() == GameState.DRAWING_ACTIVE) {
                cancelTimer();
                broadcaster.toAll(room, "SYSTEM_MSG|" + username + " disconnected. Round skipped.");
                endRound(room);
            }
        }
    }

    // ═══ Scoring ═══

    /**
     * Skribbl-style scoring (Option B):
     *   guesserScore = 50 + (secondsLeft / totalSeconds) × 250
     *   Range: 50 (last second) to 300 (instant guess)
     */
    public static int calculateGuesserScore(int secondsLeft, int totalSeconds) {
        double timeFraction = (double) secondsLeft / totalSeconds;
        return (int) Math.round(50 + timeFraction * 250);
    }

    // ═══ Close Guess Detection ═══

    public static boolean isCloseGuess(String guess, String answer) {
        guess = guess.toLowerCase().trim();
        answer = answer.toLowerCase().trim();
        if (guess.equals(answer)) return false;
        return levenshteinDistance(guess, answer) <= 2;
    }

    public static int levenshteinDistance(String a, String b) {
        int[][] dp = new int[a.length() + 1][b.length() + 1];
        for (int i = 0; i <= a.length(); i++) dp[i][0] = i;
        for (int j = 0; j <= b.length(); j++) dp[0][j] = j;
        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(Math.min(
                    dp[i - 1][j] + 1,
                    dp[i][j - 1] + 1),
                    dp[i - 1][j - 1] + cost
                );
            }
        }
        return dp[a.length()][b.length()];
    }

    // ═══ Timer Helpers ═══

    private void startTimer(Room room, int intervalSeconds, Runnable task) {
        cancelTimer();
        timerTask = timerExecutor.scheduleAtFixedRate(task, intervalSeconds, intervalSeconds, TimeUnit.SECONDS);
    }

    private void cancelTimer() {
        if (timerTask != null && !timerTask.isCancelled()) {
            timerTask.cancel(false);
        }
    }
}
