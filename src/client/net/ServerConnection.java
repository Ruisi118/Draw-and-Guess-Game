package client.net;

import java.io.*;
import java.net.Socket;

/**
 * Manages the TCP socket connection to the game server.
 * Send is thread-safe (PrintWriter with autoFlush).
 */
public class ServerConnection {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private boolean connected = false;

    public void connect(String host, int port) throws IOException {
        socket = new Socket(host, port);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);
        connected = true;
    }

    public void send(String message) {
        if (out != null && connected) {
            out.println(message);
        }
    }

    public String receive() throws IOException {
        if (in == null) return null;
        return in.readLine();
    }

    public void disconnect() {
        connected = false;
        try {
            if (socket != null) socket.close();
        } catch (IOException ignored) {}
    }

    public boolean isConnected() {
        return connected && socket != null && !socket.isClosed();
    }
}
