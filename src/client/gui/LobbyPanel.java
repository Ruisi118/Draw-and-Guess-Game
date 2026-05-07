package client.gui;

import util.*;

import javax.swing.*;
import java.awt.*;

/**
 * Lobby screen — create/join room, player list, start game.
 * Uses CardLayout to switch between join view and room view.
 */
public class LobbyPanel extends JPanel {
    private CardLayout cardLayout;
    private JPanel joinView, roomView;

    // Join view
    private JTextField roomCodeField;
    private JButton createBtn, joinBtn;

    // Room view
    private JLabel roomCodeLabel;
    private JPanel playerListPanel;
    private JButton startBtn, leaveBtn, readyBtn;
    private boolean myReadyState = false;

    private LobbyListener listener;

    public interface LobbyListener {
        void onCreateRoom();
        void onJoinRoom(String code);
        void onStartGame();
        void onLeaveRoom();
        void onToggleReady();
    }

    public LobbyPanel() {
        cardLayout = new CardLayout();
        setLayout(cardLayout);
        setBackground(GameColors.PAPER);

        buildJoinView();
        buildRoomView();

        add(joinView, "join");
        add(roomView, "room");
        cardLayout.show(this, "join");
    }

    private void buildJoinView() {
        joinView = new JPanel(new GridBagLayout());
        joinView.setBackground(GameColors.PAPER);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Title
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        JLabel title = GameStyles.createHeaderLabel("Draw & Guess");
        title.setHorizontalAlignment(SwingConstants.CENTER);
        joinView.add(title, gbc);

        // Subtitle
        gbc.gridy = 1;
        JLabel sub = GameStyles.createCaptionLabel("Sketch, guess, and have fun");
        sub.setHorizontalAlignment(SwingConstants.CENTER);
        joinView.add(sub, gbc);

        gbc.gridy = 2;
        joinView.add(Box.createVerticalStrut(16), gbc);

        // Create room button
        gbc.gridy = 3; gbc.gridwidth = 2;
        createBtn = GameStyles.createPrimaryButton("Create Room");
        createBtn.addActionListener(e -> { if (listener != null) listener.onCreateRoom(); });
        joinView.add(createBtn, gbc);

        // Separator
        gbc.gridy = 4;
        JLabel orLabel = GameStyles.createCaptionLabel("— or join an existing room —");
        orLabel.setHorizontalAlignment(SwingConstants.CENTER);
        joinView.add(orLabel, gbc);

        // Room code input
        gbc.gridy = 5; gbc.gridwidth = 1;
        roomCodeField = new JTextField();
        roomCodeField.setFont(GameFonts.SUBTITLE);
        roomCodeField.setHorizontalAlignment(JTextField.CENTER);
        roomCodeField.setColumns(8);
        roomCodeField.setBackground(GameColors.BG_SECONDARY);
        roomCodeField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(GameColors.BORDER, 1),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        // Focus highlight = lime accent
        roomCodeField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent e) {
                roomCodeField.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(GameColors.BORDER_FOCUS, 2),
                    BorderFactory.createEmptyBorder(7, 11, 7, 11)
                ));
            }
            public void focusLost(java.awt.event.FocusEvent e) {
                roomCodeField.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(GameColors.BORDER, 1),
                    BorderFactory.createEmptyBorder(8, 12, 8, 12)
                ));
            }
        });
        // Placeholder via prompt text
        roomCodeField.putClientProperty("JTextField.placeholderText", "Room Code");
        joinView.add(roomCodeField, gbc);

        gbc.gridx = 1;
        joinBtn = GameStyles.createSecondaryButton("Join");
        joinBtn.addActionListener(e -> {
            String code = roomCodeField.getText().trim();
            if (!code.isEmpty() && listener != null) listener.onJoinRoom(code.toUpperCase());
        });
        joinView.add(joinBtn, gbc);
    }

    private void buildRoomView() {
        roomView = new JPanel(new BorderLayout(0, 16));
        roomView.setBackground(GameColors.PAPER);
        roomView.setBorder(BorderFactory.createEmptyBorder(24, 40, 24, 40));

        // Top: room code
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        topPanel.setOpaque(false);
        JLabel codeTitle = GameStyles.createCaptionLabel("ROOM CODE");
        roomCodeLabel = new JLabel("----");
        roomCodeLabel.setFont(GameFonts.ROOM_CODE);
        topPanel.add(codeTitle);
        topPanel.add(Box.createHorizontalStrut(12));
        topPanel.add(roomCodeLabel);
        roomView.add(topPanel, BorderLayout.NORTH);

        // Center: player list
        playerListPanel = new JPanel();
        playerListPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 12, 8));
        playerListPanel.setOpaque(false);
        roomView.add(playerListPanel, BorderLayout.CENTER);

        // Bottom: buttons
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        btnPanel.setOpaque(false);
        startBtn = GameStyles.createPrimaryButton("Start Game");
        startBtn.addActionListener(e -> { if (listener != null) listener.onStartGame(); });
        readyBtn = GameStyles.createSecondaryButton("Ready");
        readyBtn.addActionListener(e -> { if (listener != null) listener.onToggleReady(); });
        leaveBtn = GameStyles.createDangerButton("Leave");
        leaveBtn.addActionListener(e -> { if (listener != null) listener.onLeaveRoom(); });
        btnPanel.add(startBtn);
        btnPanel.add(readyBtn);
        btnPanel.add(leaveBtn);
        roomView.add(btnPanel, BorderLayout.SOUTH);
    }

    // ═══ Public Methods ═══

    public void showJoinView() {
        cardLayout.show(this, "join");
    }

    public void showRoomView(String roomCode) {
        roomCodeLabel.setText(roomCode);
        cardLayout.show(this, "room");
    }

    public void updatePlayerList(String[] players, String host) {
        updatePlayerList(players, host, java.util.Collections.emptyMap());
    }

    /** Update player list with ready states. readyMap: username -> "1" if ready. */
    public void updatePlayerList(String[] players, String host, java.util.Map<String, String> readyMap) {
        playerListPanel.removeAll();
        for (int i = 0; i < players.length; i++) {
            String name = players[i].trim();
            boolean isHost = name.equals(host);
            boolean isReady = isHost || "1".equals(readyMap.get(name));

            JPanel chip = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
            chip.setBorder(BorderFactory.createLineBorder(
                isReady ? GameColors.ACCENT : GameColors.BORDER, isReady ? 2 : 1));
            chip.setBackground(GameColors.PAPER);

            JLabel dot = new JLabel("\u25CF");
            dot.setForeground(GameColors.getPlayerColor(i));
            JLabel nameLabel = new JLabel(name);
            nameLabel.setFont(GameFonts.BODY);
            chip.add(dot);
            chip.add(nameLabel);

            if (isHost) {
                JLabel badge = new JLabel("HOST");
                badge.setFont(GameFonts.CAPTION);
                badge.setForeground(GameColors.TEXT_PRIMARY);
                badge.setOpaque(true);
                badge.setBackground(GameColors.ACCENT);
                badge.setBorder(BorderFactory.createEmptyBorder(1, 4, 1, 4));
                chip.add(badge);
            } else if (isReady) {
                JLabel badge = new JLabel("READY");
                badge.setFont(GameFonts.CAPTION);
                badge.setForeground(GameColors.SUCCESS);
                badge.setBorder(BorderFactory.createEmptyBorder(1, 4, 1, 4));
                chip.add(badge);
            }
            playerListPanel.add(chip);
        }
        playerListPanel.revalidate();
        playerListPanel.repaint();
    }

    /** Show/hide buttons based on whether the user is host. */
    public void setIsHost(boolean isHost) {
        startBtn.setVisible(isHost);
        readyBtn.setVisible(!isHost);
    }

    /** Update ready button label/style based on local ready state. */
    public void setMyReadyState(boolean ready) {
        myReadyState = ready;
        readyBtn.setText(ready ? "Not Ready" : "Ready");
        readyBtn.setBackground(ready ? GameColors.ACCENT : GameColors.BG_SECONDARY);
    }

    /** Enable/disable Start button based on whether all non-host players are ready. */
    public void setCanStart(boolean canStart) {
        startBtn.setEnabled(canStart);
    }

    public void setStartEnabled(boolean enabled) { startBtn.setEnabled(enabled); }
    public void setListener(LobbyListener l)     { this.listener = l; }
}
