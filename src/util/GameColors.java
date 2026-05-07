package util;

import java.awt.Color;

/**
 * Design System — Color Tokens.
 * Three-layer: Primitive → Semantic → Component.
 * NEVER use raw Color constructors in UI code — always reference these constants.
 */
public final class GameColors {
    private GameColors() {}

    // ═══ PRIMITIVE LAYER ═══

    // Gray Scale
    public static final Color GRAY_50  = new Color(249, 250, 251);
    public static final Color GRAY_100 = new Color(243, 244, 246);
    public static final Color GRAY_200 = new Color(229, 231, 235);
    public static final Color GRAY_300 = new Color(209, 213, 219);
    public static final Color GRAY_400 = new Color(156, 163, 175);
    public static final Color GRAY_500 = new Color(107, 114, 128);
    public static final Color GRAY_600 = new Color(75, 85, 99);
    public static final Color GRAY_700 = new Color(55, 65, 81);
    public static final Color GRAY_800 = new Color(31, 41, 55);
    public static final Color GRAY_900 = new Color(17, 24, 39);

    // Blue Scale
    public static final Color BLUE_50  = new Color(239, 246, 255);
    public static final Color BLUE_100 = new Color(219, 234, 254);
    public static final Color BLUE_500 = new Color(59, 130, 246);
    public static final Color BLUE_600 = new Color(37, 99, 235);
    public static final Color BLUE_700 = new Color(29, 78, 216);

    // Green Scale
    public static final Color GREEN_100 = new Color(220, 252, 231);
    public static final Color GREEN_500 = new Color(34, 197, 94);
    public static final Color GREEN_600 = new Color(22, 163, 74);

    // Red Scale
    public static final Color RED_100 = new Color(254, 226, 226);
    public static final Color RED_500 = new Color(239, 68, 68);
    public static final Color RED_600 = new Color(220, 38, 38);

    // Yellow / Amber
    public static final Color YELLOW_100 = new Color(254, 249, 195);
    public static final Color YELLOW_400 = new Color(250, 204, 21);

    // Orange
    public static final Color ORANGE_500 = new Color(249, 115, 22);

    public static final Color WHITE = new Color(255, 255, 255);
    public static final Color BLACK = new Color(0, 0, 0);

    // ═══ SEMANTIC LAYER — Playground Style ═══

    // Paper / grid background
    public static final Color PAPER          = new Color(245, 243, 237); // #F5F3ED
    public static final Color GRID_LINE      = new Color(200, 195, 185, 25); // very subtle grid

    // Backgrounds
    public static final Color BG_PRIMARY     = PAPER;
    public static final Color BG_SECONDARY   = new Color(254, 254, 250); // #FEFEFA canvas white
    public static final Color BG_SIDEBAR     = GRAY_800;                 // dark sidebar
    public static final Color BG_TOOLBAR     = PAPER;
    public static final Color BG_CANVAS      = new Color(254, 254, 250);
    public static final Color BG_OVERLAY     = new Color(0, 0, 0, 128);

    // Text
    public static final Color TEXT_PRIMARY   = new Color(26, 26, 24);    // #1A1A18
    public static final Color TEXT_SECONDARY = new Color(85, 85, 80);    // #555550
    public static final Color TEXT_ON_DARK   = GRAY_100;
    public static final Color TEXT_MUTED     = new Color(176, 173, 165); // #B0ADA5

    // Brand / Action — dark button + lime accent (like playground)
    public static final Color PRIMARY        = new Color(26, 26, 24);    // dark/black buttons
    public static final Color PRIMARY_HOVER  = new Color(196, 241, 53);  // lime on hover
    public static final Color PRIMARY_ACTIVE = new Color(158, 192, 32);  // darker lime
    public static final Color ACCENT         = new Color(196, 241, 53);  // #C4F135 lime

    // Status
    public static final Color SUCCESS    = GREEN_500;
    public static final Color SUCCESS_BG = GREEN_100;
    public static final Color DANGER     = RED_500;
    public static final Color DANGER_BG  = RED_100;
    public static final Color WARNING    = ORANGE_500;
    public static final Color WARNING_BG = YELLOW_100;
    public static final Color INFO       = new Color(59, 130, 246);
    public static final Color INFO_BG    = BLUE_50;

    // Borders — stronger, like playground's 1.5px black
    public static final Color BORDER       = new Color(26, 26, 24);      // black borders
    public static final Color BORDER_LIGHT = GRAY_200;                    // subtle separators
    public static final Color BORDER_FOCUS = new Color(196, 241, 53);  // lime accent
    public static final Color BORDER_ERROR = RED_500;

    // Disabled
    public static final Color DISABLED_BG = GRAY_100;
    public static final Color DISABLED_FG = GRAY_400;

    // ═══ PLAYER COLORS ═══

    public static final Color[] PLAYER_COLORS = {
        new Color(239, 68, 68),   // Red
        new Color(59, 130, 246),  // Blue
        new Color(34, 197, 94),   // Green
        new Color(249, 115, 22),  // Orange
        new Color(168, 85, 247),  // Purple
        new Color(236, 72, 153),  // Pink
        new Color(20, 184, 166),  // Teal
        new Color(234, 179, 8),   // Yellow
    };

    public static Color getPlayerColor(int index) {
        return PLAYER_COLORS[index % PLAYER_COLORS.length];
    }

    // ═══ CANVAS COLORS ═══

    public static final Color CANVAS_BG          = WHITE;
    public static final Color CANVAS_DEFAULT_PEN = BLACK;
    public static final Color CANVAS_ERASER      = WHITE;
    public static final Color CANVAS_GRID        = new Color(230, 230, 230);

    public static final Color[] PEN_PRESETS = {
        BLACK,
        new Color(127, 127, 127),
        new Color(185, 59, 43),
        new Color(239, 68, 68),
        new Color(249, 115, 22),
        new Color(234, 179, 8),
        new Color(34, 197, 94),
        new Color(59, 130, 246),
        new Color(168, 85, 247),
        new Color(236, 72, 153),
        new Color(139, 90, 43),
        WHITE,
    };
}
