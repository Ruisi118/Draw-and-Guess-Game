package server;

import server.db.DatabaseManager;
import server.db.UserDAO;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Main game server — accepts TCP connections and spawns ClientHandler threads.
 * Usage: java server.GameServer [port]
 */
public class GameServer {
    public static final int DEFAULT_PORT = 12345;

    private final int port;
    private final RoomManager roomManager;
    private final UserDAO userDAO;
    private final Broadcaster broadcaster;
    private final ExecutorService threadPool;

    private final DatabaseManager dbManager;

    public GameServer(int port) {
        this.port = port;
        this.dbManager = new DatabaseManager();
        this.roomManager = new RoomManager();
        this.userDAO = new UserDAO(dbManager);
        this.broadcaster = new Broadcaster();
        this.threadPool = Executors.newCachedThreadPool();
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("╔══════════════════════════════════╗");
            System.out.println("║   Draw & Guess Server Started    ║");
            System.out.println("║   Port: " + port + "                     ║");
            System.out.println("╚══════════════════════════════════╝");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New connection: " + clientSocket.getInetAddress());
                ClientHandler handler = new ClientHandler(clientSocket, roomManager, userDAO, broadcaster, dbManager);
                threadPool.execute(handler);
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : DEFAULT_PORT;
        new GameServer(port).start();
    }
}
