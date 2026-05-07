package server;

import server.db.DatabaseManager;
import java.sql.*;
import java.util.*;

/**
 * Manages word selection from the database.
 * Returns 3 choices (easy/medium/hard) for the drawer to pick from.
 */
public class WordBank {
    private final DatabaseManager db;
    private final Set<String> usedWords = new HashSet<>();

    public WordBank(DatabaseManager db) {
        this.db = db;
    }

    /**
     * Get 3 word choices — one from each difficulty level.
     * Avoids repeating words already used this game.
     */
    public List<String> getThreeChoices() {
        List<String> choices = new ArrayList<>();
        choices.add(getRandomWord(1)); // Easy
        choices.add(getRandomWord(2)); // Medium
        choices.add(getRandomWord(3)); // Hard
        return choices;
    }

    private String getRandomWord(int difficulty) {
        String sql = "SELECT word FROM words WHERE difficulty = ? ORDER BY RANDOM() LIMIT 10";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, difficulty);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String word = rs.getString("word");
                if (!usedWords.contains(word)) {
                    usedWords.add(word);
                    return word;
                }
            }
        } catch (SQLException e) {
            System.err.println("Word bank error: " + e.getMessage());
        }
        // Fallback if all words used or error
        return "drawing";
    }

    /** Reset used words for a new game. */
    public void reset() {
        usedWords.clear();
    }
}
