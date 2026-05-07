package util;

/**
 * Design System — Spacing & Layout Tokens.
 * Base unit: 4px.
 */
public final class GameDimensions {
    private GameDimensions() {}

    // Spacing Scale
    public static final int SPACE_XXS = 4;
    public static final int SPACE_XS  = 8;
    public static final int SPACE_SM  = 12;
    public static final int SPACE_MD  = 16;
    public static final int SPACE_LG  = 24;
    public static final int SPACE_XL  = 32;

    // Panel Padding
    public static final int PANEL_PADDING    = SPACE_MD;
    public static final int PANEL_PADDING_SM = SPACE_SM;
    public static final int PANEL_PADDING_LG = SPACE_LG;

    // Component Gaps
    public static final int COMPONENT_GAP = SPACE_XS;
    public static final int FORM_GAP      = SPACE_SM;
    public static final int SECTION_GAP   = SPACE_LG;

    // Window
    public static final int WINDOW_WIDTH  = 1100;
    public static final int WINDOW_HEIGHT = 700;
    public static final int WINDOW_MIN_W  = 900;
    public static final int WINDOW_MIN_H  = 600;

    // Canvas (logical coordinates — all clients share this)
    public static final int CANVAS_WIDTH  = 800;
    public static final int CANVAS_HEIGHT = 600;

    // Sidebar
    public static final int SIDEBAR_WIDTH    = 250;
    public static final int TOOLBAR_HEIGHT   = 48;
    public static final int TIMER_BAR_HEIGHT = 6;

    // Dialogs
    public static final int DIALOG_LOGIN_W = 400;
    public static final int DIALOG_LOGIN_H = 320;
    public static final int DIALOG_WORD_W  = 480;
    public static final int DIALOG_WORD_H  = 200;

    // Buttons
    public static final int BUTTON_HEIGHT    = 36;
    public static final int BUTTON_HEIGHT_SM = 28;
    public static final int BUTTON_HEIGHT_LG = 44;
    public static final int BUTTON_PADDING_X = 16;
}
