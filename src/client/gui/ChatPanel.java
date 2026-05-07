package client.gui;

import util.*;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;

/**
 * Chat/guessing panel with colored usernames and styled messages.
 * Uses tab stops to align icon/prefix column consistently across message types.
 */
public class ChatPanel extends JPanel {
    private static final int ICON_TAB_PX = 22;

    private JTextPane messagePane;
    private StyledDocument doc;
    private JTextField inputField;
    private JButton sendBtn;
    private ChatListener listener;

    public interface ChatListener {
        void onSendGuess(String text);
    }

    public ChatPanel() {
        setLayout(new BorderLayout());
        setBackground(GameColors.BG_SECONDARY);
        setBorder(BorderFactory.createMatteBorder(0, 2, 0, 0, GameColors.BORDER));

        JLabel header = new JLabel("  CHAT");
        header.setFont(GameFonts.MONO_LABEL);
        header.setForeground(GameColors.TEXT_SECONDARY);
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, GameColors.BORDER));
        header.setPreferredSize(new Dimension(0, 28));
        add(header, BorderLayout.NORTH);

        messagePane = new JTextPane();
        messagePane.setEditable(false);
        messagePane.setBackground(GameColors.BG_SECONDARY);
        messagePane.setMargin(new Insets(8, 8, 8, 8));
        doc = messagePane.getStyledDocument();
        add(GameStyles.createScrollPane(messagePane), BorderLayout.CENTER);

        JPanel inputRow = new JPanel(new BorderLayout());
        inputRow.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, GameColors.BORDER));

        inputField = new JTextField();
        inputField.setFont(GameFonts.BODY);
        inputField.setBackground(GameColors.BG_SECONDARY);
        inputField.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        inputField.addActionListener(e -> sendMessage());

        sendBtn = new JButton("Send");
        sendBtn.setFont(GameFonts.SUBTITLE);
        sendBtn.setBackground(GameColors.TEXT_PRIMARY);
        sendBtn.setForeground(GameColors.PAPER);
        sendBtn.setFocusPainted(false);
        sendBtn.setBorderPainted(false);
        sendBtn.setOpaque(true);
        sendBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        sendBtn.addActionListener(e -> sendMessage());

        inputRow.add(inputField, BorderLayout.CENTER);
        inputRow.add(sendBtn, BorderLayout.EAST);
        add(inputRow, BorderLayout.SOUTH);
    }

    private void sendMessage() {
        String text = inputField.getText().trim();
        if (!text.isEmpty() && listener != null) {
            listener.onSendGuess(text);
            inputField.setText("");
        }
    }

    // ═══ Public message methods ═══
    // Each method uses format: ICON \t CONTENT \n
    // Tab stop ensures CONTENT always starts at the same x position.

    public void addMessage(String text) {
        startLine();
        append("\t", GameColors.TEXT_PRIMARY, false, false);
        append(text + "\n", GameColors.TEXT_PRIMARY, false, false);
        applyTabStop();
    }

    public void addColoredMessage(String username, String text, Color userColor) {
        startLine();
        append("●\t", userColor, false, false); // bullet + tab
        append(username + ": ", userColor, true, false);
        append(text + "\n", GameColors.TEXT_PRIMARY, false, false);
        applyTabStop();
    }

    public void addSystemMessage(String text) {
        startLine();
        append("\t", GameColors.TEXT_SECONDARY, false, false);
        append(text + "\n", GameColors.TEXT_SECONDARY, false, true);
        applyTabStop();
    }

    public void addCorrectMessage(String text) {
        startLine();
        append("✔\t", GameColors.SUCCESS, true, false);
        append(text + "\n", GameColors.SUCCESS, true, false);
        applyTabStop();
    }

    public void addCloseMessage(String text) {
        startLine();
        append("⚡\t", GameColors.WARNING, false, false);
        append(text + "\n", GameColors.WARNING, false, false);
        applyTabStop();
    }

    /** Private message visible only to drawer + already-guessed players. */
    public void addPrivateMessage(String username, String text, Color userColor) {
        startLine();
        append("★\t", GameColors.TEXT_SECONDARY, false, true); // ★
        append(username + ": ", userColor, true, true);
        append(text + "\n", GameColors.TEXT_SECONDARY, false, true);
        applyTabStop();
    }

    public void clearMessages() {
        messagePane.setText("");
    }

    public void setInputEnabled(boolean enabled) {
        inputField.setEnabled(enabled);
        sendBtn.setEnabled(enabled);
    }

    public void setListener(ChatListener l) { this.listener = l; }

    // ═══ Internal helpers ═══

    private int lineStartOffset = 0;

    private void startLine() {
        lineStartOffset = doc.getLength();
    }

    private void append(String text, Color color, boolean bold, boolean italic) {
        SimpleAttributeSet attrs = new SimpleAttributeSet();
        StyleConstants.setForeground(attrs, color);
        StyleConstants.setFontFamily(attrs, GameFonts.FONT_SANS);
        StyleConstants.setFontSize(attrs, 13);
        if (bold) StyleConstants.setBold(attrs, true);
        if (italic) StyleConstants.setItalic(attrs, true);
        try {
            doc.insertString(doc.getLength(), text, attrs);
        } catch (BadLocationException ignored) {}
        messagePane.setCaretPosition(doc.getLength());
    }

    /** Apply tab stop to the just-inserted paragraph so content aligns to ICON_TAB_PX. */
    private void applyTabStop() {
        SimpleAttributeSet pAttrs = new SimpleAttributeSet();
        TabStop[] stops = new TabStop[] { new TabStop(ICON_TAB_PX) };
        StyleConstants.setTabSet(pAttrs, new TabSet(stops));
        int len = doc.getLength() - lineStartOffset;
        if (len > 0) {
            doc.setParagraphAttributes(lineStartOffset, len, pAttrs, false);
        }
    }
}
