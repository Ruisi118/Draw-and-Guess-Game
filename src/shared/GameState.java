package shared;

/**
 * Server-side game state machine.
 *
 * LOBBY → WORD_SELECTION → DRAWING_ACTIVE → ROUND_ENDING
 *   ↑                                           │
 *   └──── GAME_OVER ← GAME_ENDING ←─────────────┘
 */
public enum GameState {
    LOBBY,              // Waiting for players to join
    WORD_SELECTION,     // Drawer choosing a word (10s)
    DRAWING_ACTIVE,     // Drawing + guessing in progress (80s)
    ROUND_ENDING,       // Showing round results (5s)
    GAME_ENDING,        // Showing final leaderboard
    GAME_OVER           // Game finished, waiting for action
}
