package server.db;

import java.sql.*;

/**
 * SQLite database manager — initializes schema and provides connections.
 * Only the server process accesses the database.
 */
public class DatabaseManager {
    private static final String DB_FILE = "drawandguess.db";
    private static final String URL = "jdbc:sqlite:" + DB_FILE;

    public DatabaseManager() {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            System.err.println("SQLite JDBC driver not found. Add sqlite-jdbc.jar to classpath.");
        }
        initSchema();
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL);
    }

    private void initSchema() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS users (" +
                "  id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "  username TEXT UNIQUE NOT NULL," +
                "  password_hash TEXT NOT NULL," +
                "  high_score INTEGER DEFAULT 0," +
                "  games_played INTEGER DEFAULT 0" +
                ")"
            );
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS words (" +
                "  id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "  word TEXT NOT NULL," +
                "  category TEXT," +
                "  difficulty INTEGER DEFAULT 1" +
                ")"
            );

            // Seed word bank if empty
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM words");
            if (rs.next() && rs.getInt(1) == 0) {
                seedWords(conn);
            }
        } catch (SQLException e) {
            System.err.println("Database init failed: " + e.getMessage());
        }
    }

    private void seedWords(Connection conn) throws SQLException {
        String[][] words = {
            // {word, category, difficulty}
            // Animals
            {"cat", "animals", "1"}, {"dog", "animals", "1"}, {"fish", "animals", "1"},
            {"bird", "animals", "1"}, {"snake", "animals", "1"}, {"horse", "animals", "1"},
            {"elephant", "animals", "2"}, {"giraffe", "animals", "2"}, {"penguin", "animals", "2"},
            {"dolphin", "animals", "2"}, {"butterfly", "animals", "2"}, {"octopus", "animals", "3"},
            // Food
            {"apple", "food", "1"}, {"pizza", "food", "1"}, {"cake", "food", "1"},
            {"banana", "food", "1"}, {"burger", "food", "1"}, {"ice cream", "food", "1"},
            {"sushi", "food", "2"}, {"pancake", "food", "2"}, {"popcorn", "food", "2"},
            {"watermelon", "food", "2"}, {"broccoli", "food", "2"}, {"spaghetti", "food", "3"},
            // Objects
            {"house", "objects", "1"}, {"car", "objects", "1"}, {"tree", "objects", "1"},
            {"book", "objects", "1"}, {"phone", "objects", "1"}, {"chair", "objects", "1"},
            {"umbrella", "objects", "2"}, {"guitar", "objects", "2"}, {"bicycle", "objects", "2"},
            {"telescope", "objects", "2"}, {"lighthouse", "objects", "3"}, {"chandelier", "objects", "3"},
            // Actions
            {"run", "actions", "1"}, {"swim", "actions", "1"}, {"dance", "actions", "1"},
            {"sleep", "actions", "1"}, {"cook", "actions", "1"}, {"sing", "actions", "1"},
            {"fishing", "actions", "2"}, {"surfing", "actions", "2"}, {"painting", "actions", "2"},
            {"skydiving", "actions", "3"}, {"juggling", "actions", "3"},
            // Places
            {"beach", "places", "1"}, {"school", "places", "1"}, {"hospital", "places", "1"},
            {"mountain", "places", "2"}, {"castle", "places", "2"}, {"volcano", "places", "2"},
            {"pyramid", "places", "2"}, {"island", "places", "2"},
            // People
            {"doctor", "people", "1"}, {"teacher", "people", "1"}, {"pirate", "people", "2"},
            {"astronaut", "people", "2"}, {"wizard", "people", "2"}, {"ninja", "people", "2"},
            {"detective", "people", "3"},
            // Nature
            {"sun", "nature", "1"}, {"moon", "nature", "1"}, {"rainbow", "nature", "1"},
            {"snowflake", "nature", "2"}, {"tornado", "nature", "2"}, {"earthquake", "nature", "3"},
            // Sports
            {"soccer", "sports", "1"}, {"basketball", "sports", "1"}, {"tennis", "sports", "1"},
            {"bowling", "sports", "2"}, {"fencing", "sports", "2"}, {"archery", "sports", "2"},
            // Fantasy
            {"dragon", "fantasy", "2"}, {"unicorn", "fantasy", "2"}, {"robot", "fantasy", "2"},
            {"alien", "fantasy", "2"}, {"mermaid", "fantasy", "2"}, {"zombie", "fantasy", "2"},
            {"spaceship", "fantasy", "3"},
            // Everyday
            {"clock", "everyday", "1"}, {"key", "everyday", "1"}, {"lamp", "everyday", "1"},
            {"mirror", "everyday", "2"}, {"scissors", "everyday", "2"}, {"toothbrush", "everyday", "2"},
            {"headphones", "everyday", "2"},
        };

        String sql = "INSERT INTO words (word, category, difficulty) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (String[] w : words) {
                ps.setString(1, w[0]);
                ps.setString(2, w[1]);
                ps.setInt(3, Integer.parseInt(w[2]));
                ps.addBatch();
            }
            ps.executeBatch();
        }
        System.out.println("Seeded " + words.length + " words into database.");
    }
}
