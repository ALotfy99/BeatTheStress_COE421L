import java.util.ArrayList;
import java.util.List;

/**
 * GameLevelManager - Observes SystemControlSubject to handle level changes and pause/resume logic.
 * Manages beatmap loading and notifies observers when beatmaps change.
 */
public class GameLevelManager implements Observer {
    
    // Beatmaps (5 total) - same as in BeatController
    private static final BeatJudge.Beat[] BEATMAP_0 = new BeatJudge.Beat[] {
        new BeatJudge.Beat(0), new BeatJudge.Beat(1), new BeatJudge.Beat(2), new BeatJudge.Beat(3),
        new BeatJudge.Beat(0), new BeatJudge.Beat(2), new BeatJudge.Beat(1), new BeatJudge.Beat(3)
    };
    
    private static final BeatJudge.Beat[] BEATMAP_1 = new BeatJudge.Beat[] {
        new BeatJudge.Beat(3), new BeatJudge.Beat(2), new BeatJudge.Beat(1), new BeatJudge.Beat(0),
        new BeatJudge.Beat(3), new BeatJudge.Beat(1), new BeatJudge.Beat(2), new BeatJudge.Beat(0)
    };
    
    private static final BeatJudge.Beat[] BEATMAP_2 = new BeatJudge.Beat[] {
        new BeatJudge.Beat(1), new BeatJudge.Beat(1), new BeatJudge.Beat(2), new BeatJudge.Beat(2),
        new BeatJudge.Beat(0), new BeatJudge.Beat(3), new BeatJudge.Beat(0), new BeatJudge.Beat(3)
    };
    
    private static final BeatJudge.Beat[] BEATMAP_3 = new BeatJudge.Beat[] {
        new BeatJudge.Beat(2), new BeatJudge.Beat(0), new BeatJudge.Beat(2), new BeatJudge.Beat(1),
        new BeatJudge.Beat(3), new BeatJudge.Beat(1), new BeatJudge.Beat(0), new BeatJudge.Beat(3)
    };
    
    private static final BeatJudge.Beat[] BEATMAP_4 = new BeatJudge.Beat[] {
        new BeatJudge.Beat(0), new BeatJudge.Beat(0), new BeatJudge.Beat(0), new BeatJudge.Beat(1),
        new BeatJudge.Beat(2), new BeatJudge.Beat(3), new BeatJudge.Beat(2), new BeatJudge.Beat(1)
    };
    
    // Bank of maps in order
    private final BeatJudge.Beat[][] BEATMAP_BANK = new BeatJudge.Beat[][] {
        BEATMAP_0, BEATMAP_1, BEATMAP_2, BEATMAP_3, BEATMAP_4
    };
    
    // Level names
    private final String[] LEVEL_NAMES = {
        "Level 1: KOTON",
        "Level 2: MCR House of Wolves",
        "Level 3: This is How I Disappear",
        "Level 4: Mozart",
        "Level 5: Mario",
        "Level 6: Zelda"
    };
    
    private final SystemControlSubject systemControlSubject;
    private final BeatJudge beatJudge;
    private final MusicController musicController;
    
    private volatile int currentLevelIndex = 0;
    private volatile boolean isPaused = false;
    
    // Observers for level changes
    private final List<LevelChangeObserver> levelObservers = new ArrayList<>();
    
    /**
     * Interface for observing level changes
     */
    public interface LevelChangeObserver {
        void onLevelChanged(int levelIndex, String levelName);
        void onPauseStateChanged(boolean isPaused);
        void onSequenceEnd();
    }
    
    /**
     * Constructor - registers with SystemControlSubject
     */
    public GameLevelManager(SystemControlSubject systemControlSubject, 
                           BeatJudge beatJudge, 
                           MusicController musicController) {
        if (systemControlSubject == null || beatJudge == null || musicController == null) {
            throw new IllegalArgumentException("All parameters must be non-null");
        }
        this.systemControlSubject = systemControlSubject;
        this.beatJudge = beatJudge;
        this.musicController = musicController;
        
        // Register this manager with the SystemControlSubject
        systemControlSubject.registerObserver(this);
        
        // Load initial beatmap
        loadBeatmap(currentLevelIndex);
    }
    
    /**
     * Register an observer for level changes
     */
    public void registerLevelObserver(LevelChangeObserver observer) {
        synchronized (levelObservers) {
            if (!levelObservers.contains(observer)) {
                levelObservers.add(observer);
            }
        }
    }
    
    /**
     * Observer implementation - handles AR3 control packets
     */
    @Override
    public void update(ArduinoPacket pkt) {
        // Only process Arduino3 (system controls)
        if (pkt.getArduinoID() != 3) return;
        
        int buttonID = (pkt.getPayload() & 0x03);
        
        switch (buttonID) {
            case 0:
                previousLevel();
                break;
            case 1:
                nextLevel();
                break;
            case 2:
                togglePauseResume();
                break;
            default:
                System.out.println("[GameLevelManager] Unknown button: " + buttonID);
                break;
        }
    }
    
    /**
     * Load a beatmap by index
     */
    private synchronized void loadBeatmap(int index) {
        if (index < 0 || index >= BEATMAP_BANK.length) {
            System.err.println("[GameLevelManager] Invalid beatmap index: " + index);
            return;
        }
        
        currentLevelIndex = index;
        beatJudge.setBeatmap(BEATMAP_BANK[index], index);
        
        // Restart music for the new level (in case it was paused after sequence end)
        // Use the level index as the song index (they should be synchronized)
        if (isPaused) {
            isPaused = false;
            musicController.togglePlayPause(); // Resume if paused
        }
        musicController.startSong(index);
        
        String levelName = (index < LEVEL_NAMES.length) ? LEVEL_NAMES[index] : "Level " + (index + 1);
        
        // Notify observers
        synchronized (levelObservers) {
            for (LevelChangeObserver o : levelObservers) {
                o.onLevelChanged(index, levelName);
            }
        }
        
        System.out.println("[GameLevelManager] Loaded beatmap #" + index + ": " + levelName);
    }
    
    /**
     * Go to next level
     */
    public synchronized void nextLevel() {
        int nextIndex = (currentLevelIndex + 1) % BEATMAP_BANK.length;
        loadBeatmap(nextIndex);
    }
    
    /**
     * Go to previous level
     */
    public synchronized void previousLevel() {
        int prevIndex = (currentLevelIndex - 1 + BEATMAP_BANK.length) % BEATMAP_BANK.length;
        loadBeatmap(prevIndex);
    }
    
    /**
     * Toggle pause/resume
     */
    public synchronized void togglePauseResume() {
        isPaused = !isPaused;

        musicController.togglePlayPause();

        if (isPaused) {
            beatJudge.pauseBeats();
        } else {
            beatJudge.resumeBeats();
        }

        synchronized (levelObservers) {
            for (LevelChangeObserver o : levelObservers) {
                o.onPauseStateChanged(isPaused);
            }
        }

        System.out.println("[GameLevelManager] " + (isPaused ? "Paused" : "Resumed"));
    }

    
    /**
     * Called when beat sequence ends - pauses music instead of stopping completely
     * This allows the music to be restarted when level changes
     */
    public void onSequenceEnd() {
        System.out.println("[GameLevelManager] Sequence ended - pausing music");
        // Pause instead of stop to allow restart when level changes
        if (!isPaused) {
            isPaused = true;
            musicController.togglePlayPause();
            beatJudge.resumeBeats();   // ✅ resume beat scheduling too

        }
        
        // Notify observers
        synchronized (levelObservers) {
            for (LevelChangeObserver o : levelObservers) {
                o.onSequenceEnd();
            }
        }
    }
    
    public int getCurrentLevelIndex() {
        return currentLevelIndex;
    }
    
    public String getCurrentLevelName() {
        if (currentLevelIndex < LEVEL_NAMES.length) {
            return LEVEL_NAMES[currentLevelIndex];
        }
        return "Level " + (currentLevelIndex + 1);
    }
    
    public boolean isPaused() {
        return isPaused;
    }
}

