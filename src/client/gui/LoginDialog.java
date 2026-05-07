package client.gui;

import util.*;

import javax.swing.*;
import java.awt.*;

/**
 * Login/Register screen — playground retro-modern style.
 */
public class LoginDialog extends JPanel {
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JTextField serverField;
    private JButton loginBtn, registerBtn;
    private JLabel errorLabel;
    private LoginListener listener;

    public interface LoginListener {
        void onLogin(String username, String passwordHash, String host, int port);
        void onRegister(String username, String passwordHash, String host, int port);
    }

    public LoginDialog() {
        setLayout(new GridBagLayout());
        setBackground(GameColors.PAPER);

        // Hidden server field
        serverField = new JTextField("localhost:12345");

        // Card with grid background
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(GameColors.GRID_LINE);
                for (int x = 0; x < getWidth(); x += 24) g.drawLine(x, 0, x, getHeight());
                for (int y = 0; y < getHeight(); y += 24) g.drawLine(0, y, getWidth(), y);
            }
        };
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(GameColors.BG_SECONDARY);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(GameColors.BORDER, 1),
            BorderFactory.createEmptyBorder(40, 48, 36, 48)
        ));
        // Don't set preferredSize — let BoxLayout compute from children

        // Title
        JLabel title = new JLabel("Draw & Guess");
        title.setFont(GameFonts.HEADER);
        title.setForeground(GameColors.TEXT_PRIMARY);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(title);

        card.add(Box.createVerticalStrut(4));
        JLabel sub = new JLabel("Sketch, guess, and have fun");
        sub.setFont(GameFonts.CAPTION);
        sub.setForeground(GameColors.TEXT_SECONDARY);
        sub.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(sub);

        card.add(Box.createVerticalStrut(28));

        // Username
        card.add(makeFieldLabel("USERNAME"));
        usernameField = makeInput();
        card.add(usernameField);
        card.add(Box.createVerticalStrut(16));

        // Password
        card.add(makeFieldLabel("PASSWORD"));
        passwordField = new JPasswordField();
        passwordField.setFont(GameFonts.BODY);
        passwordField.setBackground(GameColors.BG_SECONDARY);
        passwordField.setAlignmentX(Component.LEFT_ALIGNMENT);
        passwordField.setMaximumSize(new Dimension(300, 36));
        passwordField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(GameColors.BORDER, 1),
            BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));
        passwordField.addActionListener(e -> doAuth(false));
        card.add(passwordField);

        card.add(Box.createVerticalStrut(6));

        // Error
        errorLabel = new JLabel(" ");
        errorLabel.setFont(GameFonts.CAPTION);
        errorLabel.setForeground(GameColors.DANGER);
        errorLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(errorLabel);

        card.add(Box.createVerticalStrut(12));

        // Buttons — left/right aligned
        JPanel btnRow = new JPanel(new BorderLayout(10, 0));
        btnRow.setOpaque(false);
        btnRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnRow.setMaximumSize(new Dimension(300, 38));

        loginBtn = GameStyles.createPrimaryButton("Log In");
        loginBtn.setPreferredSize(new Dimension(140, 36));
        loginBtn.addActionListener(e -> doAuth(false));

        registerBtn = GameStyles.createSecondaryButton("Register");
        registerBtn.setPreferredSize(new Dimension(140, 36));
        registerBtn.addActionListener(e -> doAuth(true));

        btnRow.add(loginBtn, BorderLayout.WEST);
        btnRow.add(registerBtn, BorderLayout.EAST);
        card.add(btnRow);

        card.add(Box.createVerticalStrut(12));

        // Forgot-password hint (static text, not clickable)
        JLabel forgot = new JLabel("Forgot password? Register a new account.");
        forgot.setFont(GameFonts.CAPTION);
        forgot.setForeground(GameColors.TEXT_SECONDARY);
        forgot.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(forgot);

        add(card);
    }

    private JLabel makeFieldLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(GameFonts.MONO_LABEL);
        label.setForeground(GameColors.TEXT_SECONDARY);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        label.setBorder(BorderFactory.createEmptyBorder(0, 2, 4, 0));
        return label;
    }

    private JTextField makeInput() {
        JTextField f = new JTextField(20); // 20 columns
        f.setFont(GameFonts.BODY);
        f.setForeground(GameColors.TEXT_PRIMARY);
        f.setBackground(GameColors.BG_SECONDARY);
        f.setAlignmentX(Component.LEFT_ALIGNMENT);
        f.setMaximumSize(new Dimension(300, 36));
        f.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(GameColors.BORDER, 1),
            BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));
        return f;
    }

    private void doAuth(boolean isRegister) {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword()).trim();
        String serverText = serverField.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            showError("Username and password required");
            return;
        }

        String host = "localhost";
        int port = 12345;
        if (!serverText.isEmpty()) {
            String[] parts = serverText.split(":");
            host = parts[0];
            if (parts.length > 1) {
                try { port = Integer.parseInt(parts[1]); }
                catch (NumberFormatException e) { showError("Invalid port"); return; }
            }
        }

        String hash = PasswordUtil.hash(password);
        if (listener != null) {
            if (isRegister) listener.onRegister(username, hash, host, port);
            else listener.onLogin(username, hash, host, port);
        }
    }

    public void showError(String msg) { errorLabel.setText(msg); }
    public void clearError() { errorLabel.setText(" "); }
    public void setListener(LoginListener l) { this.listener = l; }

    /** Set the default server host:port (e.g. from CLI args). Used by main(). */
    public void setDefaultServer(String host, int port) {
        serverField.setText(host + ":" + port);
    }
}
