import javax.swing.*;
import jssc.SerialPortException;

/**
 * GameController is the main orchestrator of Beat The Stress.
 *
 * Responsibilities:
 * 1) Observes ArduinoHandlers (array).
 * 2) Observes BeatController (BeatObserver).
 * 3) Owns the UI and drives game flow.
 * 4) Computes difficulty from user courses, then scales it via ScaleConverter.
 * 5) Starts MusicController and BeatController for the current song.
 */
public class GameController implements Observer, BeatObserver {

    // ===== Core Game Data =====
    private String playerName;
    private double difficultyScaled = 0;
    private int score = 0;

    // Level management (number of courses = number of levels)
    private int numLevels = 1;
    private int currentLevel = 1;

    // ===== Devices / Controllers =====
    private final ArduinoHandler[] arduinoHandlers; // ar1, ar2, ar3
    private final MusicController music;
    private BeatController beatController;

    // ===== UI =====
    private BeatGameUI ui;

    // ===== Song state =====
    private int currentSongIndex = 0;

    /**
     * Constructor:
     * - Stores music + arduino handlers
     * - Registers GameController as Observer to each ArduinoHandler's Subject
     * - Launches UI in idle state (waiting for Start Game)
     */
    public GameController(MusicController music, ArduinoHandler... handlers) {
        if (music == null)
            throw new IllegalArgumentException("MusicController cannot be null");
        if (handlers == null || handlers.length == 0)
            throw new IllegalArgumentException("You must pass ArduinoHandlers");

        this.music = music;
        this.arduinoHandlers = handlers;

        // Register as observer to all Arduino subjects
        for (ArduinoHandler h : arduinoHandlers) {
            if (h == null)
                continue;
            h.registerObserver(this); // you are observer to arduino handlers
        }

        // UI starts first, game not running yet
        launchUIWaitingForStart();
    }

    // ============================================================
    // ======================= UI FLOW ============================
    // ============================================================

    private void launchUIWaitingForStart() {
        SwingUtilities.invokeLater(() -> {
            // Start with no BeatController yet. We'll create it after user config.
            ui = new BeatGameUI(null, "Beat The Stress");

            // Simple start gate
            int start = JOptionPane.showConfirmDialog(
                    ui,
                    "Press Start to begin the game.",
                    "Start Game",
                    JOptionPane.OK_CANCEL_OPTION);

            if (start == JOptionPane.OK_OPTION) {
                configurePlayerAndLevels();
                currentLevel = 1;
                startLevel(currentLevel);
            } else {
                JOptionPane.showMessageDialog(ui, "Game not started.");
            }

        });
    }

    /**
     * Ask user for:
     * 1) name
     * 2) number of courses
     * 3) each course number
     * Then compute rawDifficulty and scale it.
     */
    private double ScaleConverter(int difficulty0to5) {
        // TODO Auto-generated method stub

        return difficulty0to5 - 2.0;
    }

    private void configurePlayerAndLevels() {
        // Name
        playerName = JOptionPane.showInputDialog(ui, "Enter player name:");
        if (playerName == null || playerName.trim().isEmpty())
            playerName = "Player";

        // Number of levels (courses)
        numLevels = readInt("How many courses/levels do you want to play?", 1, 30);

        JOptionPane.showMessageDialog(
                ui,
                "Player: " + playerName +
                        "\nNumber of levels: " + numLevels);
    }

    private int readInt(String msg, int min, int max) {
        while (true) {
            String s = JOptionPane.showInputDialog(ui, msg);
            if (s == null)
                return min; // user cancelled -> default
            try {
                int v = Integer.parseInt(s.trim());
                if (v < min || v > max)
                    throw new NumberFormatException();
                return v;
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(ui, "Enter a valid integer in range [" + min + ", " + max + "]");
            }
        }
    }

    // ============================================================
    // ===================== GAME START ===========================
    // ============================================================

    private void startLevel(int levelIndex) {
        // Ask for difficulty in [0,5] for this level
        int diff = readInt(
                "Enter difficulty for level " + levelIndex + " (0 = easiest, 5 = hardest):",
                0,
                5);

        // Convert [0,5] difficulty → [-2,3] tempo scaling
        difficultyScaled = ScaleConverter(diff);

        JOptionPane.showMessageDialog(
                ui,
                "Starting level " + levelIndex +
                        "\nPlayer: " + playerName +
                        "\nDifficulty (0–5) = " + diff +
                        "\nTempo scaling = " + difficultyScaled);

        // Now start the game for this level
        startGame();
    }

    private void startGame() {
        try {
            // Build BeatController for current song beatmap
            BeatController.Beat[] beatmap = getBeatmapForSong(currentSongIndex);

            // BeatController observes Arduino2-style hits via Subject in ArduinoHandlers,
            // so we pass a subject from the handler that emits hits (Arduino2 usually).
            Subject hitSubject = pickHitSubject();
            beatController = new BeatController(beatmap, hitSubject);

            // GameController observes BeatController events
            beatController.registerObserver(this);

            // Use the already-computed tempo scaling for this level
            music.setTempo(difficultyScaled);
            music.start();

            // Re-wire UI to real controller now that it exists
            ui.dispose();
            ui = new BeatGameUI(beatController, "Beat The Stress");

            System.out.println("[GameController] Game started. Player=" + playerName +
                    ", level=" + currentLevel +
                    ", tempoScale=" + difficultyScaled);
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(ui, "Failed to start game: " + e.getMessage());
        }
    }

    /**
     * Decide which Arduino subject feeds BeatController hits.
     * Assumption: Arduino2 sends pressure/button lane hits.
     */
    private Subject pickHitSubject() {
        // Find first handler that looks like Arduino2 (or just first non-null).
        return arduinoHandlers[2]; // it's always the 2nd one
    }

    /**
     * Beatmaps per song (you can expand this).
     */
    private BeatController.Beat[] getBeatmapForSong(int songIndex) {
        switch (songIndex) {
            case 0:
                return new BeatController.Beat[] {
                        new BeatController.Beat(0),
                        new BeatController.Beat(2),
                        new BeatController.Beat(1)
                };

            case 1:
                return new BeatController.Beat[] {
                        new BeatController.Beat(1),
                        new BeatController.Beat(1),
                        new BeatController.Beat(3)
                };

            default:
                return BeatController.DEFAULT_BEATMAP;
        }
    }

    // ============================================================
    // ================= BeatObserver events ======================
    // ============================================================

    @Override
    public void onBeatActivated(int laneIndex) {
        // UI already updates via BeatDiamondPanel observing BeatController.
        // You *can* add extra orchestration here if needed.
        System.out.println("[GameController] Beat activated lane=" + laneIndex);
    }

    @Override
    public void onHitResult(int laneIndex, String judgment) {
        System.out.println("[GameController] Hit result lane=" + laneIndex + " -> " + judgment);

        if ("GOOD".equals(judgment)) {
            score++;
            // Vibrate via Arduino3 (assumption: arduinoHandlers[2] is Arduino3)
            ArduinoHandler vibHandler = getArduino3();
            if (vibHandler != null) {
                try {
                    vibHandler.writeByte((byte) 0b0000_0100);
                } catch (SerialPortException e) {
                    e.printStackTrace();
                }
            }
        }

        // let UI handle that
        // If you want UI score update, do it here:
        // if (ui != null && ui.getPanel() != null) {
        // ui.getPanel().setScore(score); // assuming you add this method to panel
        // }
    }

    @Override
    public void onSequenceEnd() {
        System.out.println("[GameController] Sequence ended for level " + currentLevel);
        JOptionPane.showMessageDialog(
                ui,
                "Level " + currentLevel + " ended.\nCurrent score: " + score);

        // stop/pause music for this level
        music.togglePlayPause();

        if (currentLevel < numLevels) {
            currentLevel++;
            startLevel(currentLevel);
        } else {
            JOptionPane.showMessageDialog(
                    ui,
                    "All levels completed!\nFinal score: " + score);
        }
    }

    // ============================================================
    // ================= Arduino Observer =========================
    // ============================================================

    /**
     * Called by any ArduinoHandler when it emits a packet.
     */
    @Override
    public void update(ArduinoPacket pkt) {
        if (pkt == null)
            return;

        // Route packets based on source/type.
        // You said:
        // - Arduino1 HR only
        // - Arduino2 Pressure/Button only
        // - Arduino3 PC-controlled vibration only
        //
        // So we *ignore* HR for beat judging directly (BeatController handles hits).
        if (pkt instanceof heartRatePacket) {
            music.setTempo(ScaleConverter(pkt.getTempo()));

        } else if (pkt instanceof pressurePacket) {
            //
        } else {
            System.out.println("[GameController] Unknown packet: " + pkt);
        }
    }

    private void handleHeartRate(ArduinoPacket pkt) {
        int hr = pkt.getPayload();
        System.out.println("[GameController] HR=" + hr);

        // OPTIONAL orchestration:
        // e.g., tempo adjust based on HR zones
        // music.setTempo(dynamicTempoFromHR(hr));
    }

    private void handleHitEcho(ArduinoPacket pkt) {
        // just logging for debugging
        System.out.println("[GameController] Hit packet seen: " + pkt.getPayload());
    }

    private ArduinoHandler getArduino3() {
        if (arduinoHandlers.length >= 3)
            return arduinoHandlers[2];
        return null;
    }

    // ============================================================
    // ==================== Public getters ========================
    // ============================================================

    public int getScore() {
        return score;
    }

    public double getDifficultyScaled() {
        return difficultyScaled;
    }

    public String getPlayerName() {
        return playerName;
    }
}
