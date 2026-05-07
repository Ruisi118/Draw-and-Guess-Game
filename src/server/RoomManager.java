package server;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Random;

/**
 * Manages all active game rooms.
 * Thread-safe via ConcurrentHashMap.
 */
public class RoomManager {
    private final ConcurrentHashMap<String, Room> rooms = new ConcurrentHashMap<>();
    private final Random random = new Random();

    public Room createRoom() {
        String code = generateCode();
        Room room = new Room(code);
        rooms.put(code, room);
        System.out.println("Room created: " + code);
        return room;
    }

    public Room getRoom(String code) {
        return rooms.get(code);
    }

    public void removeRoom(String code) {
        rooms.remove(code);
        System.out.println("Room removed: " + code);
    }

    /** Generate a 4-character alphanumeric room code. */
    private String generateCode() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // No I/O/0/1 to avoid confusion
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        String code = sb.toString();
        // Ensure uniqueness
        if (rooms.containsKey(code)) return generateCode();
        return code;
    }
}
