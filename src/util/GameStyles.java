package util;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;

/**
 * Component factory — Playground retro-modern style.
 * Dark buttons, paper backgrounds, grid texture, strong borders.
 */
public final class GameStyles {
    private GameStyles() {}

    public static void initUIDefaults() {
        // Force English locale so JOptionPane buttons say "OK"/"Cancel" instead of localized terms
        java.util.Locale.setDefault(java.util.Locale.ENGLISH);
        javax.swing.JComponent.setDefaultLocale(java.util.Locale.ENGLISH);

        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception ignored) {}

        // Override JOptionPane button labels (in case look-and-feel still uses system locale)
        UIManager.put("OptionPane.okButtonText", "OK");
        UIManager.put("OptionPane.cancelButtonText", "Cancel");
        UIManager.put("OptionPane.yesButtonText", "Yes");
        UIManager.put("OptionPane.noButtonText", "No");

        UIManager.put("Panel.background", GameColors.BG_PRIMARY);
        UIManager.put("OptionPane.background", GameColors.BG_PRIMARY);
        UIManager.put("OptionPane.messageFont", GameFonts.BODY);
        UIManager.put("OptionPane.buttonFont", GameFonts.SUBTITLE);
        UIManager.put("Button.font", GameFonts.SUBTITLE);
        UIManager.put("Label.font", GameFonts.BODY);
        UIManager.put("TextField.font", GameFonts.BODY);
        UIManager.put("TextArea.font", GameFonts.BODY);
    }

    // ═══ BUTTONS — dark bg, lime hover ═══

    public static JButton createPrimaryButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(GameFonts.SUBTITLE);
        btn.setBackground(GameColors.PRIMARY);
        btn.setForeground(GameColors.PAPER);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setOpaque(true);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(
            btn.getPreferredSize().width + GameDimensions.BUTTON_PADDING_X * 2,
            GameDimensions.BUTTON_HEIGHT
        ));

        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                if (btn.isEnabled()) {
                    btn.setBackground(GameColors.PRIMARY_HOVER); // lime
                    btn.setForeground(GameColors.TEXT_PRIMARY);   // dark text on lime
                }
            }
            public void mouseExited(MouseEvent e) {
                if (btn.isEnabled()) {
                    btn.setBackground(GameColors.PRIMARY);       // back to dark
                    btn.setForeground(GameColors.PAPER);
                }
            }
        });
        return btn;
    }

    public static JButton createSecondaryButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(GameFonts.SUBTITLE);
        btn.setBackground(GameColors.BG_SECONDARY);
        btn.setForeground(GameColors.TEXT_PRIMARY);
        btn.setFocusPainted(false);
        btn.setOpaque(true);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(GameColors.BORDER, 1),
            BorderFactory.createEmptyBorder(6, 16, 6, 16)
        ));

        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                if (btn.isEnabled()) btn.setBackground(GameColors.PAPER);
            }
            public void mouseExited(MouseEvent e) {
                if (btn.isEnabled()) btn.setBackground(GameColors.BG_SECONDARY);
            }
        });
        return btn;
    }

    public static JButton createDangerButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(GameFonts.SUBTITLE);
        btn.setBackground(GameColors.RED_600);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setOpaque(true);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    public static JButton createToolButton(String label, boolean selected) {
        JButton btn = new JButton(label);
        btn.setFont(GameFonts.SUBTITLE);
        btn.setFocusPainted(false);
        btn.setOpaque(true);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setHorizontalAlignment(SwingConstants.CENTER);
        btn.setVerticalAlignment(SwingConstants.CENTER);
        btn.setMargin(new Insets(0, 0, 0, 0));
        // Compact uniform width — all 3 buttons match
        Dimension sz = new Dimension(64, 28);
        btn.setPreferredSize(sz);
        btn.setMinimumSize(sz);
        btn.setMaximumSize(sz);
        setToolButtonSelected(btn, selected);
        return btn;
    }

    public static void setToolButtonSelected(JButton btn, boolean selected) {
        if (selected) {
            btn.setBackground(GameColors.TEXT_PRIMARY);
            btn.setForeground(GameColors.PAPER);
            btn.setBorder(BorderFactory.createEmptyBorder());
        } else {
            btn.setBackground(GameColors.BG_SECONDARY);
            btn.setForeground(GameColors.TEXT_SECONDARY);
            btn.setBorder(BorderFactory.createLineBorder(GameColors.BORDER, 1));
        }
    }

    // ═══ TEXT FIELDS — strong border, paper bg ═══

    public static JTextField createTextField(String defaultText) {
        JTextField field = new JTextField(defaultText);
        field.setFont(GameFonts.BODY);
        field.setForeground(GameColors.TEXT_PRIMARY);
        field.setBackground(GameColors.BG_SECONDARY);
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(GameColors.BORDER, 1),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        field.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                field.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(GameColors.BORDER_FOCUS, 2),
                    BorderFactory.createEmptyBorder(7, 11, 7, 11)
                ));
            }
            public void focusLost(FocusEvent e) {
                field.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(GameColors.BORDER, 1),
                    BorderFactory.createEmptyBorder(8, 12, 8, 12)
                ));
            }
        });
        return field;
    }

    public static String getFieldText(JTextField field, String placeholder) {
        String text = field.getText().trim();
        return text.equals(placeholder) ? "" : text;
    }

    public static JPasswordField createPasswordField() {
        JPasswordField field = new JPasswordField();
        field.setFont(GameFonts.BODY);
        field.setBackground(GameColors.BG_SECONDARY);
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(GameColors.BORDER, 1),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        return field;
    }

    // ═══ PANELS ═══

    public static JPanel createCardPanel() {
        JPanel panel = new JPanel();
        panel.setBackground(GameColors.BG_SECONDARY);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(GameColors.BORDER, 1),
            BorderFactory.createEmptyBorder(
                GameDimensions.PANEL_PADDING, GameDimensions.PANEL_PADDING,
                GameDimensions.PANEL_PADDING, GameDimensions.PANEL_PADDING)
        ));
        return panel;
    }

    public static JPanel createTitledPanel(String title, JComponent content) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(GameColors.BG_SECONDARY);
        panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(GameColors.BORDER, 1),
            title,
            TitledBorder.LEFT, TitledBorder.TOP,
            GameFonts.MONO_LABEL, GameColors.TEXT_SECONDARY
        ));
        panel.add(content, BorderLayout.CENTER);
        return panel;
    }

    public static JPanel createDarkPanel() {
        JPanel panel = new JPanel();
        panel.setBackground(GameColors.BG_SIDEBAR);
        return panel;
    }

    /**
     * Creates a panel with grid-paper background texture.
     */
    public static JPanel createPaperPanel() {
        return new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(GameColors.GRID_LINE);
                int gridSize = 24;
                for (int x = 0; x < getWidth(); x += gridSize) {
                    g.drawLine(x, 0, x, getHeight());
                }
                for (int y = 0; y < getHeight(); y += gridSize) {
                    g.drawLine(0, y, getWidth(), y);
                }
            }
        };
    }

    // ═══ LABELS ═══

    public static JLabel createHeaderLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(GameFonts.HEADER);
        label.setForeground(GameColors.TEXT_PRIMARY);
        return label;
    }

    public static JLabel createTitleLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(GameFonts.TITLE);
        label.setForeground(GameColors.TEXT_PRIMARY);
        return label;
    }

    public static JLabel createCaptionLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(GameFonts.CAPTION);
        label.setForeground(GameColors.TEXT_SECONDARY);
        return label;
    }

    public static JLabel createMonoLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(GameFonts.MONO_LABEL);
        label.setForeground(GameColors.TEXT_SECONDARY);
        return label;
    }

    // ═══ STYLED DIALOG (replaces JOptionPane) ═══

    public static void showStyledDialog(Component parent, String title, String message) {
        JDialog dialog = new JDialog(
            parent instanceof JFrame ? (JFrame) parent : null, title, true);
        dialog.getContentPane().setBackground(GameColors.BG_SECONDARY);
        dialog.setLayout(new BorderLayout());
        dialog.setSize(340, 160);
        dialog.setLocationRelativeTo(parent);
        dialog.setResizable(false);

        JLabel msgLabel = new JLabel("<html><div style='text-align:center;width:260px'>" + message + "</div></html>");
        msgLabel.setFont(GameFonts.BODY);
        msgLabel.setForeground(GameColors.TEXT_PRIMARY);
        msgLabel.setHorizontalAlignment(SwingConstants.CENTER);
        msgLabel.setBorder(BorderFactory.createEmptyBorder(24, 20, 12, 20));
        dialog.add(msgLabel, BorderLayout.CENTER);

        JButton okBtn = createPrimaryButton("OK");
        okBtn.setPreferredSize(new Dimension(100, 34));
        okBtn.addActionListener(e -> dialog.dispose());
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        btnPanel.setOpaque(false);
        btnPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 16, 0));
        btnPanel.add(okBtn);
        dialog.add(btnPanel, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }

    // ═══ SCROLL PANE ═══

    public static JScrollPane createScrollPane(JComponent view) {
        JScrollPane sp = new JScrollPane(view);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.getVerticalScrollBar().setUnitIncrement(16);
        return sp;
    }
}
