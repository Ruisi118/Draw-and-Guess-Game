package server;

import java.util.List;

/**
 * Broadcasts messages to all clients in a room.
 */
public class Broadcaster {

    /** Send to all players in the room. */
    public void toAll(Room room, String message) {
        for (ClientHandler ch : room.getPlayers()) {
            ch.send(message);
        }
    }

    /** Send to all players except one (e.g. the sender). */
    public void toAllExcept(Room room, String message, String excludeUsername) {
        for (ClientHandler ch : room.getPlayers()) {
            if (!ch.getUsername().equals(excludeUsername)) {
                ch.send(message);
            }
        }
    }

    /** Send to drawer + all already-guessed players, excluding the sender (who already shows it locally). */
    public void toGuessedGroup(Room room, String message, String excludeUsername) {
        String drawer = room.getCurrentDrawer();
        java.util.Set<String> guessed = room.getGuessedPlayers();
        for (ClientHandler ch : room.getPlayers()) {
            String name = ch.getUsername();
            if (name.equals(excludeUsername)) continue;
            if (name.equals(drawer) || guessed.contains(name)) {
                ch.send(message);
            }
        }
    }

    /** Send to a specific player by username. */
    public void toPlayer(Room room, String message, String username) {
        for (ClientHandler ch : room.getPlayers()) {
            if (ch.getUsername().equals(username)) {
                ch.send(message);
                return;
            }
        }
    }
}
