package util;

import java.awt.Font;

/**
 * Design System — Typography Tokens (Playground style).
 * Serif for display headings, SansSerif for body, Monospaced for labels/codes.
 */
public final class GameFonts {
    private GameFonts() {}

    public static final String FONT_SERIF = "Serif";       // Georgia / Times
    public static final String FONT_SANS  = "SansSerif";   // Helvetica / Segoe
    public static final String FONT_MONO  = "Monospaced";  // Courier

    // Display — Serif (like Instrument Serif in playground)
    public static final Font DISPLAY  = new Font(FONT_SERIF, Font.PLAIN, 36);
    public static final Font HEADER   = new Font(FONT_SERIF, Font.PLAIN, 28);
    public static final Font TITLE    = new Font(FONT_SERIF, Font.PLAIN, 20);

    // Body — Sans
    public static final Font SUBTITLE = new Font(FONT_SANS, Font.BOLD,  13);
    public static final Font BODY     = new Font(FONT_SANS, Font.PLAIN, 13);
    public static final Font CAPTION  = new Font(FONT_SANS, Font.PLAIN, 11);

    // Mono — for labels, room codes, hints
    public static final Font MONO_LABEL = new Font(FONT_MONO, Font.PLAIN, 10);
    public static final Font ROOM_CODE  = new Font(FONT_MONO, Font.BOLD,  22);
    public static final Font WORD_HINT  = new Font(FONT_MONO, Font.PLAIN, 18);
}
