# Draw & Guess — A Multiplayer Drawing and Guessing Game

**Course:** CS-GY 6103 — Introduction to Java (Spring 2026)

**Team Members:** Ruisi Dai (rd3686), Tianshu Shi (ts5453)

A real-time multiplayer Pictionary-style game built in Java. One player draws a secret word on a shared canvas while others race to guess it in chat. Inspired by [skribbl.io](https://skribbl.io/).

**📺 [Watch Demo Video]([https://youtu.be/VIDEO_ID](https://drive.google.com/file/d/1r92VthmAbuokFPwRRld8nfZk5N31qi0V/view?usp=sharing))** — 3-minute walkthrough

---

## Quick Start

### Prerequisites
- **Java 17+** (verify with `java -version`)
- The SQLite JDBC driver is bundled in `lib/` — no extra downloads needed.

### Run on macOS / Linux

```bash
cd ~/Desktop/Draw-and-Guess-Game
# Terminal 1 — start the server (compiles on first run)
./run-server.sh

# Terminal 2 — start a client
./run-client.sh

# Open more terminals to launch additional clients (need at least 2 to play)
./run-client.sh
```

### Run on Windows

```cmd
run-server.bat                  :: Terminal 1
run-client.bat                  :: Terminal 2 (and more)
```

### Manual compile / run (any platform)

```bash
# Compile (Linux/macOS — uses `find` so it works regardless of shell glob settings)
javac -cp "src:lib/sqlite-jdbc.jar" -d out $(find src -name "*.java")

# Run server (Linux/macOS)
java -cp "out:lib/sqlite-jdbc.jar" server.GameServer

# Run client (Linux/macOS)
java -cp "out:lib/sqlite-jdbc.jar" client.gui.MainFrame
```

For Windows, prefer the bundled `run-server.bat` / `run-client.bat` scripts — they handle path separators (`;` vs `:`) and source enumeration automatically.

The SQLite database (`drawandguess.db`) is **created automatically on first server run**, with a seeded word bank of 88 words across 10 categories. No manual database setup needed.

---

## Testing Modes

### Mode A — Solo Test (single machine, recommended for graders)

The simplest way to verify the project works. Open multiple terminal windows on **one machine** and run:

```bash
cd ~/Desktop/Draw-and-Guess-Game
./run-server.sh                 # Terminal 1 — server on localhost:12345
./run-client.sh                 # Terminal 2 — client window 1
./run-client.sh                 # Terminal 3 — client window 2 (need ≥2 to start a game)
./run-client.sh                 # Terminal 4 — client window 3 (optional)
```

All clients automatically connect to `localhost:12345`. Register different usernames in each window, create a room in one and have the others join with the room code. This is sufficient to verify all features (drawing, guessing, scoring, ready system, private chat).

### Mode B — LAN Test (multiple machines on same WiFi)

For demoing real multiplayer with friends on the **same local network**:

**1. On the host machine — start the server:**
```bash
./run-server.sh                 # listens on all interfaces, port 12345
```

**2. Find the host's local IP:**
```bash
# macOS / Linux
ifconfig | grep "inet " | grep -v 127.0.0.1
# Example output: inet 192.168.1.42

# Windows
ipconfig | findstr IPv4
```

**3. Open firewall for port 12345** (most consumer routers / OSes block it by default):
- macOS: System Settings → Network → Firewall → temporarily allow
- Windows: Defender Firewall → allow inbound TCP 12345
- Linux: `sudo ufw allow 12345/tcp`

**4. Other players run the client with the host's IP:**
```bash
./run-client.sh 192.168.1.42 12345     # macOS / Linux
run-client.bat 192.168.1.42 12345      # Windows
```

The client launches with `localhost:12345` pre-filled if no args are passed; CLI args override the default. Friends only need a JDK 17+ install — the project is fully self-contained.

> **Note:** The server address is configured **through command-line arguments only**, not via a field in the GUI. The login window intentionally exposes just username and password to keep single-machine usage frictionless. Graders running everything locally don't need to do anything special; only multi-machine setups require the CLI args above.

> ⚠️ Mode B is intended for **trusted local networks only**. See [Security & Limitations](#security--limitations) below.

---

## How to Play

### 1. Register & Log In
On launch, each player registers a username + password (SHA-256 hashed in DB). Usernames must be 2–20 alphanumeric characters or underscores. If you forget your password, register a new account — there is no recovery flow (no email infrastructure for a course project).

### 2. Create or Join a Room
- **Host:** click **Create Room** → 4-character room code is shown.
- **Others:** enter the room code and click **Join**.

### 3. Ready Up & Start
- Non-host players click **Ready**. Their player chip turns lime green with a `READY` badge.
- The host's **Start Game** button is disabled until **all** non-host players are Ready.
- Once everyone is ready, host clicks **Start Game**.

### 4. Round Flow

```
┌─────────────────┐   ┌──────────────────┐   ┌───────────────┐
│ 10s SELECT WORD │ → │ 80s DRAW & GUESS │ → │ 5s ROUND END  │
│  (drawer picks  │   │  (drawer paints, │   │  (answer +    │
│   1 of 3 words) │   │   others guess)  │   │   scores)     │
└─────────────────┘   └──────────────────┘   └───────────────┘
                              ↓
                      next player draws
                      (game loops indefinitely)
```

- **Word Selection (10s):** Drawer picks one of 3 words (Easy ★ / Medium ★★ / Hard ★★★). If they don't pick in time, the Easy word is auto-selected (countdown shown).
- **Drawing (80s):** Drawer uses **Brush / Eraser / Clear** + color palette + brush size (− / + buttons; brush and eraser sizes are remembered separately). Strokes are broadcast in real time.
- **Guessing:** Non-drawers see the canvas + a length hint like `_ _ _ _ _`. Type guesses in chat; the server judges correctness.
- **Round End (5s):** Answer is revealed, round scores shown, then the next player draws.

### 5. Chat Visibility (skribbl-style anti-spoiler)
- **Wrong guesses** are visible to everyone (so others can avoid duplicates).
- **Close guesses** (within Levenshtein distance 2) trigger an **"Almost!"** hint shown only to the guesser.
- **Correct guesses** broadcast `✓ X guessed correctly! (+points)` — but never the actual word.
- **Already-guessed players** chat privately with the drawer + other winners (★ prefix, italic). Still-guessing players cannot see these messages, preventing leaks.

---

## Scoring

**Guesser score** — based on speed:
```
score = 50 + (timeLeft / 80s) × 250        →  range: 50–300 points
```
Faster = more points. Even the last-second guesser gets at least 50.

**Drawer score** — depends on how fast guessers find the word:
```
score = 0.3 × Σ (each guesser's score)     →  drawer wins more if others guess fast
```
If nobody guesses correctly, the drawer earns 0 (penalty for unclear drawings).

**Leaver policy** — A player who disconnects mid-game is treated as having forfeited that game: their final score is **not** persisted to the database, and `games_played` is not incremented for them. Only players still connected at game end have their high scores updated.

---

## Features at a Glance

| Category | Features |
|----------|----------|
| **Account** | Register / Login (SHA-256 hashed passwords; alphanumeric usernames) |
| **Lobby** | Create room (4-char code), Join by code, Ready system, host transfer on disconnect |
| **Drawing** | Freehand brush, eraser, clear canvas, 12-color palette, separate brush/eraser size memory, custom cursor reflecting tool & size |
| **Game Flow** | Word selection (3 difficulty tiers, 10s auto-pick), 80s drawing/guessing rounds, 5s scoreboard, infinite round loop |
| **Chat** | Real-time pipe-delimited protocol, colored player names, system messages, correct/close/wrong styling, private chat for winners |
| **Scoring** | Skribbl-style time-based scoring (guesser 50–300, drawer 30% of guesser sum) |
| **Database** | SQLite via JDBC, auto-seeded 88-word bank, persistent user accounts & high scores |
| **Resilience** | Drawer-disconnect handling in both word-selection and drawing phases (round skipped); host auto-transferred when host leaves; server-authoritative role checks reject spoofed `DRAW` / `WORD_CHOSEN` from non-drawers; input validation + prepared statements |

---

## Advanced Java Concepts (5 — exceeds the 3-minimum requirement)

### 1. GUI Development (Java Swing)

All UI is built with `javax.swing`. Key files:
- [`DrawingPanel.java`](src/client/gui/DrawingPanel.java) — Custom `JPanel` overriding `paintComponent()` with `Graphics2D` for freehand brush rendering. Uses `BufferedImage` caching so completed strokes don't re-render every frame.
- [`ChatPanel.java`](src/client/gui/ChatPanel.java) — `JTextPane` + `StyledDocument` for colored, italic, and tab-aligned chat messages. `TabStop` ensures all message types align to a consistent x-position.
- [`MainFrame.java`](src/client/gui/MainFrame.java) — `CardLayout` switches between Login, Lobby, and Game screens.
- [`LobbyPanel.java`](src/client/gui/LobbyPanel.java) — Player chips, ready badges, dynamic Start/Ready button visibility based on host status.
- Mouse event listeners (`MouseListener`, `MouseMotionListener`) capture freehand drawing input.
- Custom `Cursor` dynamically reflects the active tool color and brush size.

### 2. Networking (Java Sockets)

TCP client-server architecture using `java.net.Socket` and `java.net.ServerSocket`:
- [`GameServer.java`](src/server/GameServer.java) — Listens on port 12345, accepts connections.
- [`ClientHandler.java`](src/server/ClientHandler.java) — One per client connection, reads / dispatches messages.
- [`ServerConnection.java`](src/client/net/ServerConnection.java) — Client-side socket wrapper.
- Pipe-delimited text protocol (e.g. `DRAW|BRUSH|-16777216|3|120,45;121,46`) for easy debugging and forward-compatibility.
- 30+ message types defined in [`MessageType.java`](src/shared/MessageType.java) covering auth, room management, ready system, game flow, drawing, chat, and timer sync.

**Server-authoritative checks** — the server validates control-plane messages before acting:
- Role check: only the assigned drawer can broadcast `DRAW` / `CLEAR_CANVAS` / `WORD_CHOSEN`.
- Word check: `WORD_CHOSEN` must be one of the three offered choices.
- Lightweight `DRAW` payload check: size cap (≤ 4KB), field count, and tool-name whitelist. Numeric fields (color, stroke width, point coordinates) are not deeply validated server-side; the client wraps `DrawAction.deserialize` in try-catch as a defensive fallback for malformed values.
- Input sanitization: chat messages strip protocol delimiters and are length-capped (≤ 200 chars); usernames must match `[a-zA-Z0-9_]{2,20}`.

### 3. Multithreading

- **Server thread pool** — `ExecutorService` (`Executors.newCachedThreadPool()`) handles concurrent client connections; one thread per `ClientHandler` ([`GameServer.java`](src/server/GameServer.java)).
- **Round timer** — `ScheduledExecutorService` fires every second to broadcast countdown updates and triggers round-end logic on timeout ([`GameController.java`](src/server/GameController.java)).
- **Client message listener** — Background `Thread` continuously reads from the socket and dispatches GUI updates via `SwingUtilities.invokeLater()` to the Event Dispatch Thread, maintaining Swing thread safety ([`MessageListener.java`](src/client/net/MessageListener.java)).
- **Word-pick timer** — Client-side `javax.swing.Timer` provides a synchronized 10s countdown for word selection.

### 4. Database (JDBC + SQLite)

Embedded SQLite via the `org.xerial:sqlite-jdbc:3.41.2.2` driver:
- [`DatabaseManager.java`](src/server/db/DatabaseManager.java) — Schema creation, seeds 88 words across 10 categories × 3 difficulty levels on first launch.
- [`UserDAO.java`](src/server/db/UserDAO.java) — Registration (SHA-256 hashed passwords via [`PasswordUtil.java`](src/util/PasswordUtil.java)), login validation, score persistence. All methods are `synchronized`.
- [`WordBank.java`](src/server/WordBank.java) — Random word selection; deduplicates within a game.
- All queries use `PreparedStatement` (SQL injection safe).

```sql
users (id, username UNIQUE, password_hash, high_score, games_played)
words (id, word, category, difficulty)
```

### 5. Concurrency Control

- [`Room.java`](src/server/Room.java) — `ConcurrentHashMap<String, AtomicInteger>` for thread-safe score updates; `Collections.synchronizedSet` for guessed-player and ready-player tracking.
- [`GameController.handleGuess()`](src/server/GameController.java) — `synchronized(room)` blocks ensure correct ordering when multiple players submit guesses near-simultaneously: the first to enter the block scores; subsequent correct guesses by the same player are no-ops.
- [`RoomManager.java`](src/server/RoomManager.java) — `ConcurrentHashMap` for safe room creation, joining, and disposal across threads.
- `volatile` fields ensure visibility of game state (`state`, `currentWord`, `currentDrawer`) across the timer thread and client handler threads.

---

## Project Structure

```
project/
├── src/
│   ├── client/
│   │   ├── gui/           Swing UI: MainFrame, LoginDialog, LobbyPanel,
│   │   │                  DrawingPanel, ChatPanel, ScorePanel, ToolBar
│   │   ├── net/           ServerConnection, MessageListener
│   │   └── model/         Tool enum, DrawAction
│   ├── server/
│   │   ├── db/            DatabaseManager, UserDAO
│   │   └── *.java         GameServer, ClientHandler, GameController,
│   │                      Room, RoomManager, WordBank, Broadcaster
│   ├── shared/            MessageType enum, GameState enum
│   └── util/              GameColors, GameFonts, GameStyles,
│                          GameDimensions, GameIcons, PasswordUtil
├── lib/
│   └── sqlite-jdbc.jar    SQLite JDBC driver (external library)
├── run-server.sh / .bat   Compile + launch server
├── run-client.sh / .bat   Launch client GUI
└── README.md              This file
```

---

## External Library

This project uses **one external library**:
- **SQLite JDBC Driver** ([xerial/sqlite-jdbc](https://github.com/xerial/sqlite-jdbc) v3.41.2.2) — Used solely as the database connectivity layer. All application logic, schema design, SQL queries, networking, GUI, and game logic are written by us.

The JAR is bundled in `lib/`, so no internet connection is needed to build or run.

---

## Troubleshooting

| Symptom | Fix |
|---------|-----|
| `Address already in use` on server start | Another server is running. `lsof -i :12345` (Mac/Linux) and kill it, or pass a different port: `java ... server.GameServer 8080`. |
| Client shows `Cannot connect` | Server isn't running, or wrong host. Default is `localhost:12345`. |
| Database errors | Delete `drawandguess.db` and restart the server to recreate the schema + reseed words. |
| Window doesn't appear / GUI exception | Check terminal output; verify Java 17+ is installed. |
| Lobby `Start Game` button greyed out | All non-host players must click **Ready** first. |
| Guesser typing isn't accepted | Check that the round is in progress (timer counting down). Drawer cannot guess their own word. |

---

## Security & Limitations

This is a **course project intended for trusted, local-network use** (single machine or LAN). The following are intentional simplifications, not bugs we missed:

| Concern | Current behavior | Why we accept it |
|---------|------------------|------------------|
| **No TLS / encryption** | All traffic is plaintext over TCP | Course scope is socket programming, not crypto. Do not run on public internet. |
| **Client-side password hashing** | Client sends SHA-256 hash; server stores hash directly | Acceptable for academic context. Note: the hash itself becomes the credential — a leaked hash = a leaked password. |
| **Unsalted SHA-256** | No per-user salt | Sufficient for course demonstration; production would use PBKDF2 / bcrypt with salt. |
| **No password recovery** | Forgotten password = register a new account | No email infrastructure for a student project. |
| **No rate limiting** | A malicious client can flood login attempts | Designed for trusted LAN, not adversarial public access. |
| **No reconnection** | Disconnect = lose game state for that player; round skipped if drawer leaves | Documented behavior; reconnection out of scope. |
| **Leaver forfeits score** | Player who leaves mid-game has score discarded (not persisted) | Prevents "leave when losing" abuse; also aligns with "completed games only count" semantics. |
| **No DOS protection** | No connection cap per IP | Trusted-network assumption. |

**What the server does enforce:**
- Role authority: only the assigned drawer can broadcast `DRAW` / `CLEAR_CANVAS` / `WORD_CHOSEN`.
- Word validity: drawer's chosen word must be one of the three offered choices.
- Payload limits: `DRAW` messages capped at 4KB and validated for field count + tool name.
- Input sanitization: usernames must match `[a-zA-Z0-9_]{2,20}`; chat messages strip protocol delimiters and cap at 200 chars.
- SQL injection safety: all DB queries use `PreparedStatement`.

**Recommended deployment scope:**
- ✅ Same machine (Mode A) — completely safe
- ✅ Same trusted LAN with friends (Mode B) — safe
- ❌ Public internet exposure (e.g. ngrok / cloud server with public IP) — **not recommended** without adding TLS, rate limiting, and reconnection.

---

## References

Game design and scoring formula were inspired by [skribbl.io](https://skribbl.io/), the popular online drawing-and-guessing game:

- **Game flow & rules** — [skribbl.io official site](https://skribbl.io/) and [skribbl.io Wiki on Fandom](https://skribbl-io.fandom.com/wiki/Skribbl.io)
- **Scoring formula** — Inspired by [skribbl.io Wiki: Points](https://skribbl-io.fandom.com/wiki/Points). Our implementation:
  - Guesser score: `50 + (timeLeft / totalTime) × 250` → range 50–300 points
  - Drawer score: `30% × sum of all guesser scores this round` → drawer is rewarded when others guess fast
- **Strategy analysis** — [Cornell INFO 2040 Course Blog: Bayes' Rule and the Strategy for Winning in skribbl.io](https://blogs.cornell.edu/info2040/2020/11/13/bayes-rule-and-the-strategy-for-winning-in-skribbl-io/)

All code (networking protocol, GUI, database logic, game state machine) is written by us. Only the game concept and approximate scoring proportions are derived from public skribbl.io documentation.
