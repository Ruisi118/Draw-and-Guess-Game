package shared;

/**
 * All message types for the pipe-delimited protocol.
 * Format: TYPE|field1|field2|...\n
 */
public enum MessageType {
    // Authentication
    LOGIN,           // C→S  LOGIN|username|passwordHash
    REGISTER,        // C→S  REGISTER|username|passwordHash
    AUTH_OK,         // S→C  AUTH_OK|username
    AUTH_FAIL,       // S→C  AUTH_FAIL|reason

    // Room Management
    CREATE_ROOM,     // C→S  CREATE_ROOM
    ROOM_CREATED,    // S→C  ROOM_CREATED|roomCode
    JOIN_ROOM,       // C→S  JOIN_ROOM|roomCode
    JOIN_OK,         // S→C  JOIN_OK|roomCode|player1,player2,...
    JOIN_FAIL,       // S→C  JOIN_FAIL|reason
    PLAYER_JOINED,   // S→C  PLAYER_JOINED|username
    PLAYER_LEFT,     // S→C  PLAYER_LEFT|username
    LEAVE_ROOM,      // C→S  LEAVE_ROOM
    HOST_CHANGED,    // S→C  HOST_CHANGED|newHostUsername
    READY_TOGGLE,    // C→S  READY_TOGGLE  (player toggles their ready state)
    READY_STATUS,    // S→C  READY_STATUS|user1:0,user2:1,user3:1  (1=ready, 0=not)

    // Game Flow
    START_GAME,      // C→S  START_GAME
    NEW_ROUND,       // S→C  NEW_ROUND|roundNum|totalRounds|drawerUsername|wordLength
    WORD_CHOICES,    // S→C  WORD_CHOICES|word1|word2|word3  (drawer only)
    WORD_CHOSEN,     // C→S  WORD_CHOSEN|word
    WORD_HINT,       // S→C  WORD_HINT|_ _ _ _ _  (guessers only)

    // Drawing
    DRAW,            // C→S→C  DRAW|toolType|colorRGB|strokeWidth|x1,y1;x2,y2;...
    CLEAR_CANVAS,    // C→S→C  CLEAR_CANVAS

    // Guessing
    GUESS,           // C→S  GUESS|guessText
    GUESS_CORRECT,   // S→C  GUESS_CORRECT|username|points
    GUESS_CLOSE,     // S→C  GUESS_CLOSE  (sender only)
    GUESS_WRONG,     // S→C  GUESS_WRONG|username|guessText
    GUESS_CHAT,      // S→C  GUESS_CHAT|username|message  (drawer + already-guessed only)

    // Timer
    TIMER_UPDATE,    // S→C  TIMER_UPDATE|secondsLeft

    // Round/Game End
    ROUND_END,       // S→C  ROUND_END|answer|user1:score1,user2:score2,...
    GAME_END,        // S→C  GAME_END|user1:totalScore1,user2:totalScore2,...

    // System
    CHAT,            // C→S→C  CHAT|username|message
    SYSTEM_MSG,      // S→C  SYSTEM_MSG|message
    ERROR,           // S→C  ERROR|message
    PING,            // C→S  PING
    PONG;            // S→C  PONG

    /**
     * Parse a raw message line and extract the MessageType.
     * @param raw e.g. "DRAW|BRUSH|0|3|10,20;11,21"
     * @return the MessageType, or null if invalid
     */
    public static MessageType fromRaw(String raw) {
        if (raw == null || raw.isEmpty()) return null;
        String typePart = raw.contains("|") ? raw.substring(0, raw.indexOf('|')) : raw;
        try {
            return MessageType.valueOf(typePart);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Extract payload fields from a raw message (everything after the type).
     * @param raw e.g. "DRAW|BRUSH|0|3|10,20;11,21"
     * @return String[] of fields, e.g. ["BRUSH", "0", "3", "10,20;11,21"]
     */
    public static String[] parseFields(String raw) {
        if (raw == null || !raw.contains("|")) return new String[0];
        return raw.substring(raw.indexOf('|') + 1).split("\\|");
    }
}
