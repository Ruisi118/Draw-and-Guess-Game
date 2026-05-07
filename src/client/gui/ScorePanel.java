package client.gui;

import util.*;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Score panel — shows player list with scores and status indicators.
 */
public class ScorePanel extends JPanel {
    private JPanel playerList;

    public ScorePanel() {
        setLayout(new BorderLayout());
        setBackground(GameColors.BG_SECONDARY);
        setBorder(BorderFactory.createMatteBorder(0, 2, 2, 0, GameColors.BORDER));

        JLabel header = new JLabel("  Players");
        header.setFont(GameFonts.CAPTION);
        header.setForeground(GameColors.TEXT_SECONDARY);
        header.setPreferredSize(new Dimension(0, 28));
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, GameColors.BORDER));
        add(header, BorderLayout.NORTH);

        playerList = new JPanel();
        playerList.setLayout(new BoxLayout(playerList, BoxLayout.Y_AXIS));
        playerList.setBackground(GameColors.BG_SECONDARY);
        add(GameStyles.createScrollPane(playerList), BorderLayout.CENTER);
    }

    /**
     * Update the player list display.
     * @param players     map of username → score
     * @param drawer      current drawer username (null if not in game)
     * @param guessed     set of usernames who have guessed correctly
     * @param playerIndex map of username → index (for color assignment)
     */
    public void updatePlayers(Map<String, Integer> players, String drawer,
                              Set<String> guessed, Map<String, Integer> playerIndex) {
        playerList.removeAll();

        for (Map.Entry<String, Integer> entry : players.entrySet()) {
            String name = entry.getKey();
            int score = entry.getValue();
            int idx = playerIndex.getOrDefault(name, 0);

            JPanel row = new JPanel(new BorderLayout());
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
            row.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

            // Determine status
            String status;
            Color statusColor;
            if (drawer != null && name.equals(drawer)) {
                status = "Drawing";
                statusColor = GameColors.TEXT_PRIMARY;
                row.setBackground(new Color(196, 241, 53, 40));
            } else if (guessed != null && guessed.contains(name)) {
                status = "Guessed";
                statusColor = GameColors.SUCCESS;
                row.setBackground(GameColors.BG_SECONDARY);
            } else {
                status = drawer != null ? "..." : "";
                statusColor = GameColors.TEXT_MUTED;
                row.setBackground(GameColors.BG_SECONDARY);
            }
            row.setOpaque(true);

            // Color dot + name
            JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
            left.setOpaque(false);
            JLabel dot = new JLabel("\u25CF"); // ●
            dot.setForeground(GameColors.getPlayerColor(idx));
            dot.setFont(new Font("SansSerif", Font.PLAIN, 10));
            JLabel nameLabel = new JLabel(name);
            nameLabel.setFont(GameFonts.BODY);
            left.add(dot);
            left.add(nameLabel);

            // Status + score
            JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
            right.setOpaque(false);
            JLabel statusLabel = new JLabel(status);
            statusLabel.setFont(GameFonts.CAPTION);
            statusLabel.setForeground(statusColor);
            JLabel scoreLabel = new JLabel(String.valueOf(score));
            scoreLabel.setFont(GameFonts.CAPTION);
            scoreLabel.setForeground(GameColors.TEXT_SECONDARY);
            right.add(statusLabel);
            right.add(scoreLabel);

            row.add(left, BorderLayout.WEST);
            row.add(right, BorderLayout.EAST);
            playerList.add(row);
        }

        playerList.revalidate();
        playerList.repaint();
    }
}
