import java.util.ArrayList;

/**
 * MusicController - Observes TempoSubject to adjust music playback speed.
 * Uses Strategy Pattern for difficulty-based tempo and beat timing.
 */
public class MusicController implements Runnable, Observer {
    
    private final MusicPlayer player;
    private final TempoSubject tempoSubject;
    private final DifficultyStrategy difficultyStrategy;
    
    // Tempo control
    private double targetTempoScale = 1.0;  // desired tempo factor
    private double currentTempoScale = 1.0; // what the player is currently using
    
    // Smoothing for tempo changes (to avoid instant jumps)
    private static final double TEMPO_SMOOTHING_STEP = 0.05; // 5% per update tick
    private static final long TEMPO_UPDATE_INTERVAL_MS = 50; // update every 50 ms
    
    // Song selection
    private int currentSongIndex = 0;
    private final String[] totalSongs;
    
    // Play/pause state
    private boolean isPaused = false;
    private boolean running = true;
    
    private Thread t1;
    
    /**
     * Constructor - initializes with difficulty strategy
     * Note: TempoSubject is now optional (AR1 controls difficulty, not tempo directly)
     */
    public MusicController(MusicPlayer player, String[] totalSongs, 
                          TempoSubject tempoSubject, DifficultyStrategy difficultyStrategy) {
        this.player = player;
        this.totalSongs = totalSongs;
        this.tempoSubject = tempoSubject;
        this.difficultyStrategy = difficultyStrategy;
        
        // Register this controller with the TempoSubject if provided
        // (AR1 now controls difficulty, not tempo, so this may be null)
        if (tempoSubject != null) {
            tempoSubject.registerObserver(this);
        }
        
        // Initialize tempo based on difficulty strategy
        int musicTempo = difficultyStrategy.getMusicTempo();
        setTempo(musicTempo);
        
        this.t1 = new Thread(this);
    }
    
    public void start() {
        t1.start();
    }
    
    /**
     * Set tempo based on music_tempo value (range: -2 to 3)
     * Maps to tempo scale: -2 -> 0.8x, 0 -> 1.0x, 3 -> 1.3x
     */
    public synchronized void setTempo(int musicTempo) {
        // Clamp to valid range [-2, 3]
        int clamped = Math.max(-2, Math.min(musicTempo, 3));
        
        // Map from [-2, 3] to [0.8, 1.3]
        // Linear mapping: -2 -> 0.8, 0 -> 1.0, 3 -> 1.3
        double newTempo = 1.0 + 0.1 * clamped;
        
        targetTempoScale = newTempo;
        System.out.printf("[MusicController] Music tempo=%d -> target tempo=%.2fx%n",
                clamped, targetTempoScale);
    }
    
    /**
     * Get the inter-beat delay from the difficulty strategy
     */
    public double getInterBeatDelay() {
        return difficultyStrategy.getBeatTempo();
    }
    
    @Override
    public synchronized void update(ArduinoPacket pkt) {
        // Check if it's Arduino1 (tempo change)
        if (pkt.getArduinoID() == 1) {
            System.out.println("[MusicController] Received tempo change from AR1");
            int scaleValue = pkt.getPayload() & 0x07; // last 3 bits
            // Map payload to music_tempo range [-2, 3]
            // Assuming payload 0-5 maps to -2 to 3
            int musicTempo = scaleValue - 2; // Shift to center around 0
            musicTempo = Math.max(-2, Math.min(musicTempo, 3)); // Clamp
            setTempo(musicTempo);
        }
    }
    
    @Override
    public void run() {
        System.out.println("[MusicController] Thread started.");
        
        // Start with the first song
        if (totalSongs.length > 0) {
            startSong(currentSongIndex);
        } else {
            System.err.println("[MusicController] No songs configured.");
        }
        
        // Main loop: gradually adjust tempo towards targetTempoScale
        while (running) {
            smoothTempoTowardsTarget();
            try {
                Thread.sleep(TEMPO_UPDATE_INTERVAL_MS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        
        // Optional: stop playback on exit
        player.stop();
        System.out.println("[MusicController] Thread stopped.");
    }
    
    public void startSong(int index) {
        if (index < 0 || index >= totalSongs.length) {
            System.err.println("[MusicController] Invalid song index: " + index);
            return;
        }
        currentSongIndex = index;
        isPaused = false;
        System.out.println("[MusicController] Starting song #" + currentSongIndex);
        player.play(currentSongIndex);
        player.setTempo(currentTempoScale);
    }
    
    public void nextSong() {
        if (totalSongs.length <= 0) return;
        currentSongIndex = (currentSongIndex + 1) % totalSongs.length;
        System.out.println("[MusicController] Next song -> #" + currentSongIndex);
        startSong(currentSongIndex);
    }
    
    public void previousSong() {
        if (totalSongs.length <= 0) return;
        currentSongIndex = (currentSongIndex - 1 + totalSongs.length) % totalSongs.length;
        System.out.println("[MusicController] Previous song -> #" + currentSongIndex);
        startSong(currentSongIndex);
    }
    
    public void togglePlayPause() {
        if (isPaused) {
            System.out.println("[MusicController] Resuming song #" + currentSongIndex);
            isPaused = false;
            player.resume();
        } else {
            System.out.println("[MusicController] Pausing song #" + currentSongIndex);
            isPaused = true;
            player.pause();
        }
    }
    
    private void smoothTempoTowardsTarget() {
        double diff = targetTempoScale - currentTempoScale;
        
        // Small enough difference: snap to target
        if (Math.abs(diff) < 0.01) {
            if (currentTempoScale != targetTempoScale) {
                currentTempoScale = targetTempoScale;
                player.setTempo(currentTempoScale);
                System.out.printf("[MusicController] Tempo snapped to %.2fx%n", currentTempoScale);
            }
            return;
        }
        
        // Move a small step toward target
        double step = TEMPO_SMOOTHING_STEP;
        if (diff < 0) step = -step;
        
        currentTempoScale += step;
        player.setTempo(currentTempoScale);
        
        System.out.printf("[MusicController] Tempo adjusted: now %.2fx (target %.2fx)%n",
                currentTempoScale, targetTempoScale);
    }
    
    public synchronized void stop() {
        System.out.println("[MusicController] Stop requested.");
        this.running = false;
        
        if (player != null) {
            player.stop();
        }
        
        try {
            if (t1 != null) {
                System.out.println("[MusicController] Waiting for controller thread to join...");
                t1.join();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
            Thread.currentThread().interrupt();
        }
        
        System.out.println("[MusicController] Stop complete.");
    }
    
    public int getCurrentSongIndex() {
        return currentSongIndex;
    }
}
