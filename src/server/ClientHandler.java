package server;

import shared.MessageType;
import shared.GameState;
import server.db.DatabaseManager;
import server.db.UserDAO;
import java.io.*;
import java.net.Socket;

/**
 * Handles a single client connection on its own thread.
 * Reads messages, dispatches to appropriate handler.
 */
public class ClientHandler implements Runnable {
    private final Socket socket;
    private final RoomManager roomManager;
    private final UserDAO userDAO;
    private final Broadcaster broadcaster;
    private final DatabaseManager dbManager;

    private BufferedReader in;
    private PrintWriter out;
    private String username;
    private Room currentRoom;

    public ClientHandler(Socket socket, RoomManager roomManager, UserDAO userDAO,
                         Broadcaster broadcaster, DatabaseManager dbManager) {
        this.socket = socket;
        this.roomManager = roomManager;
        this.userDAO = userDAO;
        this.broadcaster = broadcaster;
        this.dbManager = dbManager;
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            String line;
            while ((line = in.readLine()) != null) {
                handleMessage(line);
            }
        } catch (IOException e) {
            // Client disconnected
        } finally {
            handleDisconnect();
            try { socket.close(); } catch (IOException ignored) {}
        }
    }

    private void handleMessage(String raw) {
        MessageType type = MessageType.fromRaw(raw);
        if (type == null) return;
        String[] fields = MessageType.parseFields(raw);

        switch (type) {
            case LOGIN:
                handleLogin(fields);
                break;
            case REGISTER:
                handleRegister(fields);
                break;
            case CREATE_ROOM:
                handleCreateRoom();
                break;
            case JOIN_ROOM:
                handleJoinRoom(fields);
                break;
            case LEAVE_ROOM:
                handleLeaveRoom();
                break;
            case START_GAME:
                handleStartGame();
                break;
            case READY_TOGGLE:
                handleReadyToggle();
                break;
            case WORD_CHOSEN:
                handleWordChosen(fields);
                break;
            case DRAW:
                handleDraw(raw);
                break;
            case CLEAR_CANVAS:
                handleClearCanvas();
                break;
            case GUESS:
                handleGuess(fields);
                break;
            case CHAT:
                handleChat(fields);
                break;
            case PING:
                send("PONG");
                break;
            default:
                break;
        }
    }

    // ═══ Auth ═══

    private void handleLogin(String[] fields) {
        if (fields.length < 2) { send("AUTH_FAIL|Missing fields"); return; }
        String user = fields[0], hash = fields[1];
        if (userDAO.validateLogin(user, hash)) {
            this.username = user;
            send("AUTH_OK|" + user);
            System.out.println("Login: " + user);
        } else {
            send("AUTH_FAIL|Invalid username or password");
        }
    }

    private static final java.util.regex.Pattern USERNAME_RE =
        java.util.regex.Pattern.compile("[a-zA-Z0-9_]{2,20}");

    private void handleRegister(String[] fields) {
        if (fields.length < 2) { send("AUTH_FAIL|Missing fields"); return; }
        String user = fields[0], hash = fields[1];
        if (user.isEmpty() || hash.isEmpty()) {
            send("AUTH_FAIL|Username and password required");
            return;
        }
        if (!USERNAME_RE.matcher(user).matches()) {
            send("AUTH_FAIL|Username must be 2-20 letters/digits/underscores");
            return;
        }
        if (userDAO.register(user, hash)) {
            this.username = user;
            send("AUTH_OK|" + user);
            System.out.println("Registered: " + user);
        } else {
            send("AUTH_FAIL|Username already exists");
        }
    }

    // ═══ Room ═══

    private void handleCreateRoom() {
        if (username == null) { send("ERROR|Not logged in"); return; }
        Room room = roomManager.createRoom();
        room.addPlayer(this);
        room.setGameController(new GameController(broadcaster, new server.WordBank(dbManager), userDAO));
        this.currentRoom = room;
        send("ROOM_CREATED|" + room.getRoomCode());
    }

    private void handleJoinRoom(String[] fields) {
        if (username == null) { send("ERROR|Not logged in"); return; }
        if (fields.length < 1) { send("JOIN_FAIL|Missing room code"); return; }
        String code = fields[0].toUpperCase();
        Room room = roomManager.getRoom(code);

        if (room == null) {
            send("JOIN_FAIL|Room not found");
            return;
        }
        if (room.getState() != GameState.LOBBY && room.getState() != GameState.GAME_OVER) {
            send("JOIN_FAIL|Game in progress");
            return;
        }
        if (room.getPlayers().size() >= 8) {
            send("JOIN_FAIL|Room is full");
            return;
        }

        room.addPlayer(this);
        this.currentRoom = room;
        send("JOIN_OK|" + code + "|" + room.getPlayerList());
        broadcaster.toAllExcept(room, "PLAYER_JOINED|" + username, username);
        broadcaster.toAll(room, "READY_STATUS|" + room.getReadyStatus());
    }

    private void handleLeaveRoom() {
        if (currentRoom != null) {
            GameController gc = currentRoom.getGameController();
            if (gc != null) {
                gc.handleDisconnect(currentRoom, this);
            } else {
                currentRoom.removePlayer(this);
                broadcaster.toAll(currentRoom, "PLAYER_LEFT|" + username);
            }
            if (currentRoom.isEmpty()) {
                currentRoom.shutdown(); // release timer thread before removing
                roomManager.removeRoom(currentRoom.getRoomCode());
            }
            currentRoom = null;
        }
    }

    // ═══ Game ═══

    private void handleStartGame() {
        if (currentRoom == null || currentRoom.getGameController() == null) return;
        if (!username.equals(currentRoom.getHostUsername())) return;
        if (currentRoom.getPlayers().size() < 2) {
            send("ERROR|Need at least 2 players");
            return;
        }
        if (!currentRoom.allNonHostReady()) {
            send("ERROR|All players must be Ready before starting");
            return;
        }
        currentRoom.clearReady();
        currentRoom.getGameController().startGame(currentRoom);
    }

    private void handleReadyToggle() {
        if (currentRoom == null) return;
        if (username.equals(currentRoom.getHostUsername())) return; // host is implicitly ready
        currentRoom.toggleReady(username);
        broadcaster.toAll(currentRoom, "READY_STATUS|" + currentRoom.getReadyStatus());
    }

    private void handleWordChosen(String[] fields) {
        if (currentRoom == null || currentRoom.getGameController() == null || fields.length < 1) return;
        currentRoom.getGameController().handleWordChosen(currentRoom, username, fields[0]);
    }

    private static final int MAX_DRAW_PAYLOAD = 4096; // ~200 points max per DRAW message

    private void handleDraw(String raw) {
        if (currentRoom == null) return;
        // Authority check: only the assigned drawer may broadcast strokes
        if (!username.equals(currentRoom.getCurrentDrawer())) return;
        if (currentRoom.getState() != shared.GameState.DRAWING_ACTIVE) return;
        // Lightweight payload validation: cap size, check field count and tool name.
        // Detailed numeric validation is left to the client's deserializer (which is wrapped in try-catch).
        if (raw.length() > MAX_DRAW_PAYLOAD) return;
        String[] parts = raw.split("\\|", -1);
        if (parts.length != 5) return;
        if (!"BRUSH".equals(parts[1]) && !"ERASER".equals(parts[1])) return;
        broadcaster.toAllExcept(currentRoom, raw, username);
    }

    private void handleClearCanvas() {
        if (currentRoom == null) return;
        if (!username.equals(currentRoom.getCurrentDrawer())) return;
        if (currentRoom.getState() != shared.GameState.DRAWING_ACTIVE) return;
        broadcaster.toAllExcept(currentRoom, "CLEAR_CANVAS", username);
    }

    private void handleGuess(String[] fields) {
        if (currentRoom == null || currentRoom.getGameController() == null || fields.length < 1) return;
        // Sanitize: strip protocol delimiters and cap length to keep messages well-formed
        String clean = sanitizeChat(fields[0]);
        if (clean.isEmpty()) return;
        currentRoom.getGameController().handleGuess(currentRoom, username, clean, broadcaster);
    }

    private void handleChat(String[] fields) {
        if (currentRoom == null || fields.length < 2) return;
        String clean = sanitizeChat(fields[1]);
        if (clean.isEmpty()) return;
        broadcaster.toAll(currentRoom, "CHAT|" + fields[0] + "|" + clean);
    }

    /** Strip protocol-breaking chars and cap length so chat can't corrupt the protocol. */
    private static String sanitizeChat(String input) {
        if (input == null) return "";
        String s = input.replace("|", "").replace("\n", "").replace("\r", "").trim();
        if (s.length() > 200) s = s.substring(0, 200);
        return s;
    }

    // ═══ Disconnect ═══

    private void handleDisconnect() {
        System.out.println("Disconnected: " + (username != null ? username : "unknown"));
        handleLeaveRoom();
    }

    // ═══ Utility ═══

    public void send(String message) {
        if (out != null) {
            out.println(message);
        }
    }

    public String getUsername() {
        return username;
    }
}
