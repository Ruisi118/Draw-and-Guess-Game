package client.net;

import shared.MessageType;
import javax.swing.SwingUtilities;
import java.io.IOException;

/**
 * Background thread that reads messages from server and dispatches to GUI.
 * All GUI updates go through SwingUtilities.invokeLater().
 *
 * Uses a connectionId to prevent stale disconnect callbacks from
 * old connections triggering UI changes on the new connection.
 */
public class MessageListener implements Runnable {
    private final ServerConnection connection;
    private final MessageHandler handler;
    private final int connectionId;

    public interface MessageHandler {
        void onMessage(MessageType type, String[] fields, String raw);
        void onDisconnected(int connectionId);
    }

    public MessageListener(ServerConnection connection, MessageHandler handler, int connectionId) {
        this.connection = connection;
        this.handler = handler;
        this.connectionId = connectionId;
    }

    @Override
    public void run() {
        try {
            String line;
            while ((line = connection.receive()) != null) {
                final String raw = line;
                final MessageType type = MessageType.fromRaw(raw);
                final String[] fields = MessageType.parseFields(raw);
                SwingUtilities.invokeLater(() -> handler.onMessage(type, fields, raw));
            }
        } catch (IOException e) {
            // Connection lost
        }
        // Pass connectionId so MainFrame can check if this is the current connection
        final int id = this.connectionId;
        SwingUtilities.invokeLater(() -> handler.onDisconnected(id));
    }
}
