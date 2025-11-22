// BeatController.java
// Simple beat sequencer + judge for Beat The Stress.
// One beat at a time, sequential, resolution-driven.
// Now implements BeatSubject for Observer pattern with BeatDiamondPanel

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import jssc.SerialPortException;

public class BeatController implements Runnable, Observer, BeatSubject {

    // --- Observer pattern for beat events ---
    private final List<BeatObserver> beatObservers = new ArrayList<>();

    // --- Tunable constants ---

    // ASSUMPTION: Arduino payload is 1..4, we store lanes as 0..3
    private static final int NUM_LANES = 4;

    // --- Beat definition ---
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

    // --- Beatmap defined inside this class ---
    static final Beat[] DEFAULT_BEATMAP = new Beat[] {
        new Beat(0),
        new Beat(1),
        new Beat(2),
        new Beat(3),
        new Beat(0),
        new Beat(2),
        new Beat(1),
        new Beat(3),
        new Beat(1),
        new Beat(1),
        new Beat(2),
        new Beat(2),
        new Beat(3),
        new Beat(3)
    };

    // --- Core fields ---
    private final Beat[] beats;
    private final Subject subject;
    private final Thread thread;

    private volatile boolean running = true;

    // shared state for current beat
    private final Object lock = new Object();
    private Beat currentBeat = null;
    private int currentBeatIndex = -1;
    private long currentBeatStartMs = 0;
    private long currentBeatDeadlineMs = 0;
    private volatile int beat_interval = 500;
    private volatile int hit_window = 2000;

    private boolean resolved = false;
    private String lastJudgment = null;

    public BeatController(Subject subject) {
        this(DEFAULT_BEATMAP, subject);
    }

    public BeatController(Beat[] beats, Subject subject) {
        if (beats == null || beats.length == 0) {
            throw new IllegalArgumentException("Beat array must not be empty");
        }
        this.beats = Arrays.copyOf(beats, beats.length);
        this.subject = subject;
        subject.registerObserver(this);

        thread = new Thread(this, "BeatControllerThread");
        thread.start();
    }

    // ===== BeatSubject Implementation =====

    @Override
    public void registerObserver(BeatObserver observer) {
        synchronized (beatObservers) {
            if (!beatObservers.contains(observer)) {
                beatObservers.add(observer);
                System.out.println("[BeatController] Registered BeatObserver: " + observer.getClass().getSimpleName());
            }
        }
    }

    @Override
    public void removeObserver(BeatObserver observer) {
        synchronized (beatObservers) {
            beatObservers.remove(observer);
            System.out.println("[BeatController] Removed BeatObserver: " + observer.getClass().getSimpleName());
        }
    }

    @Override
    public void notifyBeatObservers(int laneIndex) {
        List<BeatObserver> observersCopy;
        synchronized (beatObservers) {
            observersCopy = new ArrayList<>(beatObservers);
        }
        for (BeatObserver observer : observersCopy) {
            observer.onBeatActivated(laneIndex);
        }
    }

    @Override
    public void notifyHitResult(int laneIndex, String judgment) {
        List<BeatObserver> observersCopy;
        synchronized (beatObservers) {
            observersCopy = new ArrayList<>(beatObservers);
        }
        for (BeatObserver observer : observersCopy) {
            observer.onHitResult(laneIndex, judgment);
        }
    }

    @Override
    public void notifySequenceEnd() {
        List<BeatObserver> observersCopy;
        synchronized (beatObservers) {
            observersCopy = new ArrayList<>(beatObservers);
        }
        for (BeatObserver observer : observersCopy) {
            observer.onSequenceEnd();
        }
    }

    // ===== Main Game Loop =====

    @Override
    public void run() {
        System.out.println("[BeatController] Thread started. beats=" + beats.length);

        for (int i = 0; i < beats.length && running; i++) {
            Beat beat = beats[i];

            // ---- Activate this beat immediately ----
            long activationTime = System.currentTimeMillis();
            long deadline = activationTime + hit_window;

            // update state with all needed variables
            synchronized (lock) {
                currentBeat = beat;
                currentBeatIndex = i;
                currentBeatStartMs = activationTime;
                currentBeatDeadlineMs = deadline;
                resolved = false;
                lastJudgment = null;
            }

            // Notify observers about new beat activation
            notifyBeatObservers(beat.sensorIndex);

            System.out.printf(
                "[BeatController] Beat #%d START -> lane=%d, t=%d%n",
                i, beat.sensorIndex+1, activationTime
            );

            // ---- Wait for hit or timeout ----
            synchronized (lock) {
                while (!resolved && running) {
                    long now = System.currentTimeMillis();
                    long remaining = currentBeatDeadlineMs - now;

                    if (remaining <= 0) {
                        lastJudgment = "MISS (timeout)";
                        resolved = true;
                        
                        // Notify observers of miss
                        notifyHitResult(beat.sensorIndex, lastJudgment);
                        break;
                    }

                    try {
                        lock.wait(remaining);
                    } catch (InterruptedException e) {
                        if (!running) break;
                    }
                }

                if (!running) break;

                System.out.printf(
                    "[BeatController] Beat #%d RESOLVED -> %s%n",
                    currentBeatIndex,
                    lastJudgment
                );

                // clear current beat
                currentBeat = null;
                currentBeatIndex = -1;
                currentBeatStartMs = 0;
                currentBeatDeadlineMs = 0;
            }

            // ---- Small delay before next beat ----
            if (running && i < beats.length - 1 && beat_interval > 0) {
                try {
                    Thread.sleep(beat_interval);
                } catch (InterruptedException e) {
                    if (!running) break;
                }
            }
        }

        System.out.println("[BeatController] Sequence complete.");
        
        // Notify observers that sequence ended
        notifySequenceEnd();
        
        running = false;
    }

    // ===== Observer Implementation (for Arduino packets) =====

    @Override
    public void update(ArduinoPacket pkt) {
        int payload = (pkt.getPayload() & 0x03) + 1; // assumed 1..4
        
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
                "[BeatListener] Stray hit: payload=%d at t=%d (no active beat)%n",
                payload, now
            );
            return;
        }

        // ---- Judge this hit ----
        boolean correctLane = isCorrectLane(beatSnapshot, payload);
        long deltaMs = now - startSnapshot;

        String judgment = null;

        if (now > deadlineSnapshot) {
            judgment = "MISS (too late - after window)";
        } else if (!correctLane) {
            judgment = "WRONG LANE";
        } else {
            // correct lane and inside window
            judgment = "GOOD";
            
        }

        // Notify all beat observers about the hit result
        notifyHitResult(payload - 1, judgment); // Convert payload 1..4 to lane 0..3

        System.out.printf(
            "[BeatListener] Beat #%d HIT: expectedLane=%d (LED=%d), gotPayload=%d, " +
            "delta=%d ms -> %s%n",
            beatIndexSnapshot,
            beatSnapshot.sensorIndex,
            beatSnapshot.sensorIndex + 1,
            payload,
            deltaMs,
            judgment
        );

        // ---- Resolve beat & wake scheduler ----
        synchronized (lock) {
            if (!resolved) {
                lastJudgment = judgment;
                resolved = true;
                lock.notifyAll();
            }
        }
    }

    // --- Helpers ---

    private boolean isCorrectLane(Beat beat, int payload) {
        // ASSUMPTION: payload is 1..4, beat.sensorIndex is 0..3
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

	
}
