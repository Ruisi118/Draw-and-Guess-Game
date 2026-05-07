package client.gui;

import client.model.DrawAction;
import client.model.Tool;
import client.net.MessageListener;
import client.net.ServerConnection;
import shared.MessageType;
import util.*;

import javax.swing.*;
import java.awt.*;
import java.util.*;

/**
 * Main application window — manages screen transitions and message routing.
 * Implements MessageHandler to receive server messages on EDT.
 */
public class MainFrame extends JFrame implements MessageListener.MessageHandler {
    private CardLayout cardLayout;
    private JPanel mainPanel;

    // Screens
    private LoginDialog loginDialog;
    private LobbyPanel lobbyPanel;
    private JPanel gamePanel;

    // Game sub-components
    private ToolBar toolBar;
    private DrawingPanel drawingPanel;
    private ChatPanel chatPanel;
    private ScorePanel scorePanel;

    // Network
    private ServerConnection connection;
    private Thread listenerThread;
    private volatile int currentConnectionId = 0;

    // State
    private String myUsername;
    private String currentRoomCode;
    private String currentDrawer;
    private boolean isHost;
    private final Map<String, Integer> playerScores = new LinkedHashMap<>();
    private final Map<String, Integer> playerIndex = new LinkedHashMap<>();
    private final Set<String> guessedPlayers = new HashSet<>();
    private final Map<String, String> readyMap = new HashMap<>();
    private String currentHost = "";

    public MainFrame() {
        super("Draw & Guess");
        GameStyles.initUIDefaults();

        setSize(GameDimensions.WINDOW_WIDTH, GameDimensions.WINDOW_HEIGHT);
        setMinimumSize(new Dimension(GameDimensions.WINDOW_MIN_W, GameDimensions.WINDOW_MIN_H));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        buildLoginScreen();
        buildLobbyScreen();
        buildGameScreen();

        mainPanel.add(loginDialog, "login");
        mainPanel.add(lobbyPanel, "lobby");
        mainPanel.add(gamePanel, "game");

        add(mainPanel);
        showScreen("login");
    }

    // ═══ Screen Builders ═══

    private void buildLoginScreen() {
        loginDialog = new LoginDialog();
        loginDialog.setListener(new LoginDialog.LoginListener() {
            @Override
            public void onLogin(String username, String hash, String host, int port) {
                connectAndAuth("LOGIN", username, hash, host, port);
            }
            @Override
            public void onRegister(String username, String hash, String host, int port) {
                connectAndAuth("REGISTER", username, hash, host, port);
            }
        });
    }

    private void buildLobbyScreen() {
        lobbyPanel = new LobbyPanel();
        lobbyPanel.setListener(new LobbyPanel.LobbyListener() {
            @Override
            public void onCreateRoom() { connection.send("CREATE_ROOM"); }
            @Override
            public void onJoinRoom(String code) { connection.send("JOIN_ROOM|" + code); }
            @Override
            public void onStartGame() { connection.send("START_GAME"); }
            @Override
            public void onLeaveRoom() {
                connection.send("LEAVE_ROOM");
                lobbyPanel.showJoinView();
            }
            @Override
            public void onToggleReady() { connection.send("READY_TOGGLE"); }
        });
    }

    private void buildGameScreen() {
        gamePanel = new JPanel(new BorderLayout());

        // Toolbar
        toolBar = new ToolBar();
        toolBar.setListener(new ToolBar.ToolBarListener() {
            @Override
            public void onToolChanged(Tool tool)    { drawingPanel.setCurrentTool(tool); }
            @Override
            public void onColorChanged(Color color) { drawingPanel.setCurrentColor(color); }
            @Override
            public void onStrokeChanged(int width)  { drawingPanel.setCurrentStrokeWidth(width); }
            @Override
            public void onClear()                   { drawingPanel.clearAndNotify(); }
        });

        // Canvas
        drawingPanel = new DrawingPanel();
        drawingPanel.setDrawListener(new DrawingPanel.DrawActionListener() {
            @Override
            public void onDrawAction(DrawAction action) { connection.send(action.serialize()); }
            @Override
            public void onClearCanvas()                 { connection.send("CLEAR_CANVAS"); }
        });

        // Right sidebar
        scorePanel = new ScorePanel();
        scorePanel.setPreferredSize(new Dimension(GameDimensions.SIDEBAR_WIDTH, 200));

        chatPanel = new ChatPanel();
        chatPanel.setListener(text -> {
            int idx = playerIndex.getOrDefault(myUsername, 0);
            // If already guessed correctly, show as private chat locally too
            if (guessedPlayers.contains(myUsername)) {
                chatPanel.addPrivateMessage(myUsername, text, GameColors.getPlayerColor(idx));
            } else {
                chatPanel.addColoredMessage(myUsername, text, GameColors.getPlayerColor(idx));
            }
            connection.send("GUESS|" + text);
        });

        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setPreferredSize(new Dimension(GameDimensions.SIDEBAR_WIDTH, 0));
        sidebar.add(scorePanel, BorderLayout.NORTH);
        sidebar.add(chatPanel, BorderLayout.CENTER);

        gamePanel.add(toolBar, BorderLayout.NORTH);
        gamePanel.add(drawingPanel, BorderLayout.CENTER);
        gamePanel.add(sidebar, BorderLayout.EAST);
    }

    // ═══ Network ═══

    private void connectAndAuth(String type, String username, String hash, String host, int port) {
        loginDialog.clearError();
        try {
            // Bump connection ID — any old listener callbacks will be ignored
            currentConnectionId++;
            if (connection != null) connection.disconnect();

            connection = new ServerConnection();
            connection.connect(host, port);

            // Start listening with current connection ID
            int connId = currentConnectionId;
            listenerThread = new Thread(new MessageListener(connection, this, connId));
            listenerThread.setDaemon(true);
            listenerThread.start();

            // Send auth
            connection.send(type + "|" + username + "|" + hash);
        } catch (Exception e) {
            loginDialog.showError("Cannot connect: " + e.getMessage());
        }
    }

    // ═══ Message Handler (called on EDT) ═══

    @Override
    public void onMessage(MessageType type, String[] fields, String raw) {
        if (type == null) return;

        switch (type) {
            case AUTH_OK:
                myUsername = fields[0];
                showScreen("lobby");
                break;

            case AUTH_FAIL:
                loginDialog.showError(fields.length > 0 ? fields[0] : "Auth failed");
                break;

            case ROOM_CREATED:
                currentRoomCode = fields[0];
                isHost = true;
                currentHost = myUsername;
                playerScores.clear();
                playerIndex.clear();
                readyMap.clear();
                playerScores.put(myUsername, 0);
                playerIndex.put(myUsername, 0);
                lobbyPanel.showRoomView(currentRoomCode);
                lobbyPanel.setIsHost(true);
                lobbyPanel.setCanStart(false); // need others to ready up first
                lobbyPanel.updatePlayerList(new String[]{myUsername}, myUsername, readyMap);
                break;

            case JOIN_OK:
                currentRoomCode = fields[0];
                isHost = false;
                String[] players = fields[1].split(",");
                currentHost = players[0];
                playerScores.clear();
                playerIndex.clear();
                readyMap.clear();
                for (int i = 0; i < players.length; i++) {
                    playerScores.put(players[i], 0);
                    playerIndex.put(players[i], i);
                }
                lobbyPanel.showRoomView(currentRoomCode);
                lobbyPanel.setIsHost(false);
                lobbyPanel.setMyReadyState(false);
                lobbyPanel.updatePlayerList(players, currentHost, readyMap);
                break;

            case JOIN_FAIL:
                GameStyles.showStyledDialog(this, "Cannot Join", fields[0]);
                break;

            case PLAYER_JOINED:
                String joined = fields[0];
                playerScores.putIfAbsent(joined, 0);
                playerIndex.putIfAbsent(joined, playerIndex.size());
                refreshLobbyPlayers();
                break;

            case PLAYER_LEFT:
                playerScores.remove(fields[0]);
                refreshLobbyPlayers();
                refreshScorePanel();
                break;

            case READY_STATUS:
                readyMap.clear();
                if (fields.length > 0 && !fields[0].isEmpty()) {
                    for (String entry : fields[0].split(",")) {
                        String[] kv = entry.split(":");
                        if (kv.length == 2) readyMap.put(kv[0], kv[1]);
                    }
                }
                if (!isHost) {
                    lobbyPanel.setMyReadyState("1".equals(readyMap.get(myUsername)));
                } else {
                    // host: check if all non-host are ready
                    boolean allReady = true;
                    int nonHostCount = 0;
                    for (Map.Entry<String, String> e : readyMap.entrySet()) {
                        if (e.getKey().equals(currentHost)) continue;
                        nonHostCount++;
                        if (!"1".equals(e.getValue())) { allReady = false; break; }
                    }
                    lobbyPanel.setCanStart(allReady && nonHostCount >= 1);
                }
                refreshLobbyPlayers();
                break;

            case HOST_CHANGED:
                currentHost = fields[0];
                isHost = myUsername.equals(currentHost);
                lobbyPanel.setIsHost(isHost);
                refreshLobbyPlayers();
                break;

            case NEW_ROUND:
                handleNewRound(fields);
                break;

            case WORD_CHOICES:
                handleWordChoices(fields);
                break;

            case WORD_HINT:
                toolBar.setWordText(fields[0]);
                break;

            case DRAW:
                try {
                    DrawAction action = DrawAction.deserialize(raw);
                    drawingPanel.addRemoteStroke(action);
                } catch (Exception ex) {
                    // Defensive: malformed DRAW payloads are silently dropped instead of breaking the GUI
                }
                break;

            case CLEAR_CANVAS:
                drawingPanel.clearCanvas();
                break;

            case GUESS_CORRECT:
                String guesser = fields[0];
                int pts = Integer.parseInt(fields[1]);
                guessedPlayers.add(guesser);
                playerScores.merge(guesser, pts, Integer::sum);
                chatPanel.addCorrectMessage(guesser + " guessed correctly! (+" + pts + ")");
                refreshScorePanel();
                break;

            case GUESS_CLOSE:
                chatPanel.addCloseMessage("Almost there!");
                break;

            case GUESS_CHAT:
                int gci = playerIndex.getOrDefault(fields[0], 0);
                chatPanel.addPrivateMessage(fields[0], fields[1], GameColors.getPlayerColor(gci));
                break;

            case GUESS_WRONG:
                // Don't duplicate own message (already shown locally)
                if (!fields[0].equals(myUsername)) {
                    int gi = playerIndex.getOrDefault(fields[0], 0);
                    chatPanel.addColoredMessage(fields[0], fields[1], GameColors.getPlayerColor(gi));
                }
                break;

            case TIMER_UPDATE:
                int secs = Integer.parseInt(fields[0]);
                toolBar.setTimerText(String.valueOf(secs));
                if (secs <= 10) toolBar.setTimerColor(GameColors.DANGER);
                else if (secs <= 30) toolBar.setTimerColor(GameColors.WARNING);
                else toolBar.setTimerColor(GameColors.TEXT_PRIMARY);
                break;

            case ROUND_END:
                handleRoundEnd(fields);
                break;

            case GAME_END:
                handleGameEnd(fields);
                break;

            case SYSTEM_MSG:
                chatPanel.addSystemMessage(fields[0]);
                break;

            case ERROR:
                GameStyles.showStyledDialog(this, "Error", fields[0]);
                break;

            default:
                break;
        }
    }

    @Override
    public void onDisconnected(int connectionId) {
        // Ignore disconnect from an old connection
        if (connectionId != currentConnectionId) return;
        GameStyles.showStyledDialog(this, "Disconnected", "Connection lost.");
        showScreen("login");
    }

    // ═══ Game Flow Handlers ═══

    private void handleNewRound(String[] fields) {
        int round = Integer.parseInt(fields[0]);
        currentDrawer = fields[2];
        guessedPlayers.clear();

        // Close any leftover word-pick dialog from previous round
        if (wordPickDialog != null) {
            wordPickDialog.dispose();
            wordPickDialog = null;
        }

        showScreen("game");
        drawingPanel.clearCanvas();
        chatPanel.clearMessages();
        toolBar.setRoundText("Round " + round);
        toolBar.setTimerText("80");
        toolBar.setTimerColor(GameColors.TEXT_PRIMARY);

        boolean iAmDrawer = myUsername.equals(currentDrawer);
        drawingPanel.setDrawingEnabled(iAmDrawer);
        toolBar.setEnabled(iAmDrawer);
        chatPanel.setInputEnabled(!iAmDrawer);

        if (iAmDrawer) {
            toolBar.setWordText("Choosing word...");
            chatPanel.addSystemMessage("You are drawing this round!");
        } else {
            toolBar.setWordText("");
            chatPanel.addSystemMessage(currentDrawer + " is drawing.");
        }
        refreshScorePanel();
    }

    private JDialog wordPickDialog; // tracked so we can close it on auto-pick / round-end

    private void handleWordChoices(String[] fields) {
        String[] words = {fields[0], fields[1], fields[2]};
        String[] labels = {"\u2605 Easy", "\u2605\u2605 Medium", "\u2605\u2605\u2605 Hard"};

        // Non-modal so it doesn't block the EDT (server messages must keep arriving)
        wordPickDialog = new JDialog(this, "Your Turn!", false);
        final JDialog dialog = wordPickDialog;
        dialog.setLayout(new BorderLayout());
        dialog.setSize(520, 320);
        dialog.setLocationRelativeTo(this);
        dialog.setResizable(false);
        dialog.getContentPane().setBackground(GameColors.BG_SECONDARY);

        // Title
        JLabel title = new JLabel("Choose a word to draw", SwingConstants.CENTER);
        title.setFont(GameFonts.HEADER);
        title.setBorder(BorderFactory.createEmptyBorder(28, 0, 8, 0));
        dialog.add(title, BorderLayout.NORTH);

        // Countdown label
        JLabel countdown = new JLabel("Auto-pick in 10s", SwingConstants.CENTER);
        countdown.setFont(GameFonts.CAPTION);
        countdown.setForeground(GameColors.TEXT_SECONDARY);

        // Word buttons
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 16, 0));
        btnPanel.setOpaque(false);
        btnPanel.setBorder(BorderFactory.createEmptyBorder(8, 24, 32, 24));

        final boolean[] chosen = {false};

        Runnable pickAndClose = () -> {}; // forward-declare, set below
        for (int i = 0; i < 3; i++) {
            final String word = words[i];
            JButton btn = new JButton("<html><center><b style='font-size:14px'>" + word + "</b><br><span style='font-size:11px;color:#999'>" + labels[i] + "</span></center></html>");
            btn.setFont(GameFonts.BODY);
            btn.setPreferredSize(new Dimension(140, 80));
            btn.setBackground(GameColors.BG_SECONDARY);
            btn.setFocusPainted(false);
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btn.setBorder(BorderFactory.createLineBorder(GameColors.BORDER, 1));

            btn.addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseEntered(java.awt.event.MouseEvent e) {
                    btn.setBackground(GameColors.ACCENT);
                    btn.setBorder(BorderFactory.createLineBorder(GameColors.BORDER, 2));
                }
                public void mouseExited(java.awt.event.MouseEvent e) {
                    btn.setBackground(GameColors.BG_SECONDARY);
                    btn.setBorder(BorderFactory.createLineBorder(GameColors.BORDER, 1));
                }
            });
            btn.addActionListener(e -> {
                if (chosen[0]) return;
                chosen[0] = true;
                connection.send("WORD_CHOSEN|" + word);
                toolBar.setWordText("Drawing: " + word);
                dialog.dispose();
                wordPickDialog = null;
            });
            btnPanel.add(btn);
        }

        JPanel center = new JPanel(new BorderLayout());
        center.setOpaque(false);
        center.add(countdown, BorderLayout.NORTH);
        center.add(btnPanel, BorderLayout.CENTER);
        dialog.add(center, BorderLayout.CENTER);

        // Client-side countdown \u2014 auto-pick first word at 0s
        final int[] secondsLeft = {10};
        javax.swing.Timer timer = new javax.swing.Timer(1000, null);
        timer.addActionListener(e -> {
            secondsLeft[0]--;
            if (secondsLeft[0] <= 0) {
                timer.stop();
                if (!chosen[0]) {
                    chosen[0] = true;
                    connection.send("WORD_CHOSEN|" + words[0]);
                    toolBar.setWordText("Drawing: " + words[0]);
                }
                dialog.dispose();
                wordPickDialog = null;
            } else {
                countdown.setText("Auto-pick in " + secondsLeft[0] + "s");
            }
        });
        timer.start();

        // If dialog is closed by anything else, stop the timer
        dialog.addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosed(java.awt.event.WindowEvent e) {
                timer.stop();
            }
        });
        dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        dialog.setVisible(true); // non-blocking
    }

    private void handleRoundEnd(String[] fields) {
        String answer = fields[0];
        chatPanel.addSystemMessage("The word was: " + answer);

        // Update scores from summary
        if (fields.length > 1) {
            for (String entry : fields[1].split(",")) {
                String[] kv = entry.split(":");
                if (kv.length == 2) {
                    playerScores.put(kv[0], Integer.parseInt(kv[1]));
                }
            }
        }
        refreshScorePanel();

        drawingPanel.setDrawingEnabled(false);
        toolBar.setEnabled(false);
        toolBar.setWordText("Answer: " + answer);
    }

    private void handleGameEnd(String[] fields) {
        // Parse final scores
        if (fields.length > 0) {
            for (String entry : fields[0].split(",")) {
                String[] kv = entry.split(":");
                if (kv.length == 2) {
                    playerScores.put(kv[0], Integer.parseInt(kv[1]));
                }
            }
        }
        refreshScorePanel();

        // Game ended (not enough players) — notify and return to lobby
        chatPanel.addSystemMessage("Game ended — not enough players.");
        drawingPanel.setDrawingEnabled(false);
        toolBar.setEnabled(false);

        GameStyles.showStyledDialog(this, "Game Ended", "Not enough players to continue.");
        showScreen("lobby");
        lobbyPanel.showJoinView();
    }

    // ═══ Helpers ═══

    private void showScreen(String name) {
        cardLayout.show(mainPanel, name);
    }

    private void refreshLobbyPlayers() {
        String[] names = playerScores.keySet().toArray(new String[0]);
        String host = (currentHost != null && !currentHost.isEmpty()) ? currentHost
                     : (names.length > 0 ? names[0] : "");
        lobbyPanel.updatePlayerList(names, host, readyMap);
    }

    private void refreshScorePanel() {
        scorePanel.updatePlayers(playerScores, currentDrawer, guessedPlayers, playerIndex);
    }

    // ═══ Entry Point ═══

    /**
     * Launch the client GUI.
     * Optional CLI args: <host> <port>  (default: localhost 12345)
     * Examples:
     *   java -cp ... client.gui.MainFrame
     *   java -cp ... client.gui.MainFrame 192.168.1.42
     *   java -cp ... client.gui.MainFrame 192.168.1.42 12345
     */
    public static void main(String[] args) {
        final String host = args.length >= 1 ? args[0] : "localhost";
        final int port;
        if (args.length >= 2) {
            int p = 12345;
            try { p = Integer.parseInt(args[1]); } catch (NumberFormatException ignored) {}
            port = p;
        } else {
            port = 12345;
        }
        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            frame.loginDialog.setDefaultServer(host, port);
            frame.setVisible(true);
        });
    }
}
