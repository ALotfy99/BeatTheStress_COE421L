import java.util.ArrayList;
import java.util.List;

/**
 * BeatJudge - Observes GameplaySubject to validate if the player hit the correct pad at the correct time.
 * Implements BeatSubject to notify GUI of beat activations and hit results.
 */
public class BeatJudge implements Runnable, Observer, BeatSubject {
	private volatile boolean paused = false;

    // BeatSubject implementation
    private final List<BeatObserver> beatObservers = new ArrayList<>();
    
    // Beat definition
    public static class Beat {
        public final int sensorIndex; // 0..3
        
        public Beat(int sensorIndex) {
            this.sensorIndex = sensorIndex;
        }
        
        @Override
        public String toString() {
            return "Beat{lane=" + sensorIndex + "}";
        }
    }
    
    // Current beatmap
    private volatile Beat[] beats;
    private volatile int beatmapIndex = 0;
    
    // Core fields
    private final GameplaySubject gameplaySubject;
    private final Thread thread;
    private volatile boolean running = true;
    private GameLevelManager levelManager; // Optional reference for sequence end notification
    
    // Shared state for current beat
    private final Object lock = new Object();
    private Beat currentBeat = null;
    private int currentBeatIndex = -1;
    private long currentBeatStartMs = 0;
    private long currentBeatDeadlineMs = 0;
    private volatile double beat_interval = 3.5; // Default from medium difficulty
    private volatile int hit_window = 2000;
    
    private boolean resolved = false;
    private String lastJudgment = null;
    
    // Restart flag when beatmap changes
    private volatile boolean restartRequested = false;
    
    /**
     * Constructor - registers with GameplaySubject
     */
    public BeatJudge(GameplaySubject gameplaySubject, double interBeatDelay) {
        if (gameplaySubject == null) {
            throw new IllegalArgumentException("GameplaySubject cannot be null");
        }
        this.gameplaySubject = gameplaySubject;
        this.beat_interval = interBeatDelay * 1000; // Convert seconds to milliseconds
        
        // Register this judge with the GameplaySubject
        gameplaySubject.registerObserver(this);
        
        // Initialize with empty beatmap (will be set by GameLevelManager)
        this.beats = new Beat[0];
        
        thread = new Thread(this, "BeatJudgeThread");
        thread.start();
    }
    
    /**
     * Set the GameLevelManager reference (for sequence end notification)
     */
    public void setLevelManager(GameLevelManager levelManager) {
        this.levelManager = levelManager;
    }
    
    /**
     * Set the current beatmap
     */
    public synchronized void setBeatmap(Beat[] newBeats, int beatmapIndex) {
        this.beats = newBeats != null ? newBeats.clone() : new Beat[0];
        this.beatmapIndex = beatmapIndex;
        restartRequested = true;
        notifyBeatmapIndexChanged(beatmapIndex);
        notifyBeatmapChanged("Beatmap changed to #" + beatmapIndex);
        
        // Wake scheduler if waiting and interrupt sleep if needed
        synchronized (lock) {
            resolved = true;
            lock.notifyAll();
        }
        
        // Interrupt the thread if it's sleeping to wake it up immediately
        if (thread != null && thread.isAlive()) {
            thread.interrupt();
        }
        
        System.out.println("[BeatJudge] Beatmap set -> index=" + beatmapIndex + ", beats=" + beats.length);
    }
    
    /**
     * Set the inter-beat delay (from difficulty strategy)
     */
    public synchronized void setInterBeatDelay(double seconds) {
    	this.beat_interval = seconds * 1000; // ms

        // Example: hit window is fraction of inter-beat time
        // Easy: wide window, Hard: narrower window
        int minWindow = 150; // ms
        int maxWindow = 800; // ms

        int newWindow = (int) Math.min(
            maxWindow,
            Math.max(minWindow, beat_interval * 0.9) // 40% of spacing, clamped
        );
        this.hit_window = newWindow;

        System.out.println("[BeatJudge] Inter-beat delay set to " + seconds +
                "s, hit_window=" + hit_window + " ms");
    }
    
    // ===== BeatSubject Implementation =====
    
    @Override
    public void registerObserver(BeatObserver observer) {
        synchronized (beatObservers) {
            if (!beatObservers.contains(observer)) {
                beatObservers.add(observer);
                System.out.println("[BeatJudge] Registered BeatObserver: " +
                        observer.getClass().getSimpleName());
            }
        }
    }
    
    @Override
    public void removeObserver(BeatObserver observer) {
        synchronized (beatObservers) {
            beatObservers.remove(observer);
            System.out.println("[BeatJudge] Removed BeatObserver: " +
                    observer.getClass().getSimpleName());
        }
    }
    
    @Override
    public void notifyBeatObservers(int laneIndex) {
        List<BeatObserver> copy;
        synchronized (beatObservers) {
            copy = new ArrayList<>(beatObservers);
        }
        for (BeatObserver o : copy) o.onBeatActivated(laneIndex);
    }
    
    @Override
    public void notifyHitResult(int laneIndex, String judgment) {
        List<BeatObserver> copy;
        synchronized (beatObservers) {
            copy = new ArrayList<>(beatObservers);
        }
        for (BeatObserver o : copy) o.onHitResult(laneIndex, judgment);
    }
    
    @Override
    public void notifySequenceEnd() {
        List<BeatObserver> copy;
        synchronized (beatObservers) {
            copy = new ArrayList<>(beatObservers);
        }
        for (BeatObserver o : copy) o.onSequenceEnd();
    }
    
    @Override
    public void notifyBeatmapChanged(String msg) {
        List<BeatObserver> copy;
        synchronized (beatObservers) {
            copy = new ArrayList<>(beatObservers);
        }
        for (BeatObserver o : copy) o.onBeatmapChanged(msg);
    }
    
    @Override
    public void notifyBeatmapIndexChanged(int idx) {
        List<BeatObserver> copy;
        synchronized (beatObservers) {
            copy = new ArrayList<>(beatObservers);
        }
        for (BeatObserver o : copy) o.onBeatmapIndexChanged(idx);
    }
    
    // ===== Main Game Loop =====
    
    @Override
    public void run() {
        System.out.println("[BeatJudge] Thread started.");
        
        while (running) {
        	while (paused && running) {
        	    try {
        	        Thread.sleep(100);
        	    } catch (InterruptedException e) {
        	        // just re-check paused/running
        	    }
        	}
            restartRequested = false;
            Beat[] localMap = beats; // snapshot for this pass
            notifyBeatmapIndexChanged(beatmapIndex);
            
            if (localMap.length == 0) {
                System.out.println("[BeatJudge] No beatmap loaded, waiting...");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    if (!running) break;
                }
                continue;
            }
            
            System.out.println("[BeatJudge] Starting sequence. beats=" + localMap.length);
            
            for (int i = 0; i < localMap.length && running; i++) {
            	while (paused && running && !restartRequested) {
                    try { Thread.sleep(100); }
                    catch (InterruptedException e) {}
                }
            	
                if (!running || restartRequested) break;
                
                Beat beat = localMap[i];
                
                long activationTime = System.currentTimeMillis();
                long deadline = activationTime + hit_window;
                
                synchronized (lock) {
                    currentBeat = beat;
                    currentBeatIndex = i;
                    currentBeatStartMs = activationTime;
                    currentBeatDeadlineMs = deadline;
                    resolved = false;
                    lastJudgment = null;
                }
                
                notifyBeatObservers(beat.sensorIndex);
                
                System.out.printf(
                        "[BeatJudge] Beat #%d START -> lane=%d, t=%d%n",
                        i, beat.sensorIndex + 1, activationTime
                );
                
                synchronized (lock) {
                    while (!resolved && running && !restartRequested) {
                        long now = System.currentTimeMillis();
                        long remaining = currentBeatDeadlineMs - now;
                        
                        if (remaining <= 0) {
                            lastJudgment = "MISS (timeout)";
                            resolved = true;
                            notifyHitResult(beat.sensorIndex, lastJudgment);
                            break;
                        }
                        
                        try {
                            lock.wait(remaining);
                        } catch (InterruptedException e) {
                            if (!running) break;
                        }
                    }
                    
                    if (!running || restartRequested) break;
                    
                    System.out.printf(
                            "[BeatJudge] Beat #%d RESOLVED -> %s%n",
                            currentBeatIndex, lastJudgment
                    );
                    
                    currentBeat = null;
                    currentBeatIndex = -1;
                    currentBeatStartMs = 0;
                    currentBeatDeadlineMs = 0;
                }
                
                // Skip inter-beat delay if wrong tile was pressed (move to next beat immediately)
                boolean wrongLane = lastJudgment != null && lastJudgment.contains("WRONG LANE");
                
                // Inter-beat delay - use chunked sleep to check restartRequested periodically
                if (running && i < localMap.length - 1 && beat_interval > 0 && !wrongLane) {
                    try {
                        // Sleep in chunks to allow immediate response to restartRequested
                        long remainingDelay = (long) beat_interval;
                        long chunkSize = 100; // Check every 100ms
                        
                        while (remainingDelay > 0 && running && !restartRequested) {
                            long sleepTime = Math.min(chunkSize, remainingDelay);
                            Thread.sleep(sleepTime);
                            remainingDelay -= sleepTime;
                        }
                        
                        if (!running || restartRequested) break;
                    } catch (InterruptedException e) {
                        if (!running) break;
                    }
                }
            }
            
            if (!restartRequested) {
                System.out.println("[BeatJudge] Sequence complete.");
                
                // Clear the current beat state - stop the active beatmap
                synchronized (lock) {
                    currentBeat = null;
                    currentBeatIndex = -1;
                    currentBeatStartMs = 0;
                    currentBeatDeadlineMs = 0;
                    resolved = true;
                    lastJudgment = null;
                }
                
                // Clear the beatmap so no beats are active
                this.beats = new Beat[0];
                
                notifySequenceEnd();
                // Notify GameLevelManager to stop music
                if (levelManager != null) {
                    levelManager.onSequenceEnd();
                }
                // Don't set running = false - keep thread alive to wait for next beatmap
                // The loop will continue and wait for a new beatmap to be set
            }
        }
        
        System.out.println("[BeatJudge] Thread stopped.");
    }
    
    // ===== Observer Implementation (GameplaySubject packets) =====
    
    @Override
    public void update(ArduinoPacket pkt) {
        int arduinoId = pkt.getArduinoID();
        int payload = (pkt.getPayload() & 0x03) + 1; // assumed 1..4
        
        // Only process Arduino2 (gameplay hits)
        if (arduinoId != 2) return;
        
        Beat beatSnapshot;
        int beatIndexSnapshot;
        long startSnapshot;
        long deadlineSnapshot;
        boolean alreadyResolved;
        
        synchronized (lock) {
            beatSnapshot = currentBeat;
            beatIndexSnapshot = currentBeatIndex;
            startSnapshot = currentBeatStartMs;
            deadlineSnapshot = currentBeatDeadlineMs;
            alreadyResolved = resolved;
        }
        
        long now = System.currentTimeMillis();
        
        if (beatSnapshot == null || alreadyResolved) {
            System.out.printf(
                    "[BeatJudge] Stray hit: payload=%d at t=%d (no active beat)%n",
                    payload, now
            );
            return;
        }
        
        boolean correctLane = isCorrectLane(beatSnapshot, payload);
        long deltaMs = now - startSnapshot;
        
        String judgment;
        if (now > deadlineSnapshot) {
            judgment = "MISS (too late - after window)";
        } else if (!correctLane) {
            judgment = "WRONG LANE";
        } else {
            judgment = "GOOD";
        }
        
        // GUI feedback
        notifyHitResult(payload - 1, judgment);
        
        System.out.printf(
                "[BeatJudge] Beat #%d HIT: expectedLane=%d (LED=%d), gotPayload=%d, " +
                        "delta=%d ms -> %s%n",
                beatIndexSnapshot,
                beatSnapshot.sensorIndex,
                beatSnapshot.sensorIndex + 1,
                payload,
                deltaMs,
                judgment
        );
        
        synchronized (lock) {
            if (!resolved) {
                lastJudgment = judgment;
                resolved = true;
                lock.notifyAll();
            }
        }
    }
    
    private boolean isCorrectLane(Beat beat, int payload) {
        return payload == beat.sensorIndex + 1;
    }
    
    // ===== Utility Methods =====
    
    public void stop() {
        running = false;
        synchronized (lock) {
            lock.notifyAll();
        }
    }
    
    public boolean isRunning() {
        return running;
    }
    
    public int getBeatmapIndex() {
        return beatmapIndex;
    }
    public void pauseBeats() {
        paused = true;
        synchronized (lock) {
            // mark current beat resolved so we don't hang in wait()
            resolved = true;
            lock.notifyAll();
        }
        System.out.println("[BeatJudge] Paused.");
    }

    public void resumeBeats() {
        paused = false;
        // wake thread if it's sleeping for beat_interval or idle
        if (thread != null && thread.isAlive()) thread.interrupt();
        synchronized (lock) {
            lock.notifyAll();
        }
        System.out.println("[BeatJudge] Resumed.");
    }

    public boolean isPaused() {
        return paused;
    }

}

