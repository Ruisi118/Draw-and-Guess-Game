package server.db;

import java.sql.*;

/**
 * User data access — registration, login, score updates.
 * All methods are synchronized for thread safety.
 */
public class UserDAO {
    private final DatabaseManager db;

    public UserDAO(DatabaseManager db) {
        this.db = db;
    }

    public synchronized boolean register(String username, String passwordHash) {
        String sql = "INSERT INTO users (username, password_hash) VALUES (?, ?)";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, passwordHash);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            // Username already exists (UNIQUE constraint) or other error
            return false;
        }
    }

    public synchronized boolean validateLogin(String username, String passwordHash) {
        String sql = "SELECT password_hash FROM users WHERE username = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString("password_hash").equals(passwordHash);
            }
            return false;
        } catch (SQLException e) {
            System.err.println("Login validation error: " + e.getMessage());
            return false;
        }
    }

    public synchronized void updateScore(String username, int score) {
        String sql = "UPDATE users SET high_score = MAX(high_score, ?), " +
                     "games_played = games_played + 1 WHERE username = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, score);
            ps.setString(2, username);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Score update error: " + e.getMessage());
        }
    }
}
