// BeatController.java
// Threaded LED control for Beat The Stress (communicates with Arduino2)

import java.util.List;
import jssc.SerialPortException;

/**
 * BeatController runs independently from the main game logic.
 * It sends LED commands to Arduino2 at the correct timestamps.
 */
public class BeatController implements Runnable, Observer {

    private static final long HIT_WINDOW_MS = 2000; // judging window (ms)

    private final List<BeatControls.Beat> beats;
    private final Subject subject;
    private boolean running = true;
    private long startTime;
    private final Thread t1;

    private BeatControls.Beat currentBeat = null;
    private int currentBeatIndex = -1;
    private long currentBeatStartMillis = 0;

    private final Object lock = new Object();

    // interval mode
    private volatile boolean useIntervalMode = false;
    private volatile double intervalSec = 9.0;

    public BeatController(List<BeatControls.Beat> beats, Subject subject) {
        this.beats = beats;
        this.subject = subject;
        subject.registerObserver(this);
        t1 = new Thread(this);
        t1.start();
    }

    public void enableIntervalMode(boolean enabled) {
        this.useIntervalMode = enabled;
    }

    public void setIntervalSec(double intervalSec) {
        if (intervalSec <= 0) return;
        this.intervalSec = intervalSec;
        System.out.println("[BeatController] intervalSec set to " + intervalSec + " s");
    }

    @Override
    public void run() {
        if (beats == null || beats.isEmpty()) {
            System.err.println("[BeatController] No beats to execute.");
            return;
        }

        startTime = System.currentTimeMillis();
        System.out.println("[BeatController] Thread started: " + beats.size() + " beats.");

        double nextTimeSec = 0.0;

        for (int i = 0; i < beats.size() && running; i++) {
            BeatControls.Beat beat = beats.get(i);

            long targetTimeMillis;
            if (useIntervalMode) {
                // interval mode: sequential spacing
                if (i == 0) {
                    nextTimeSec = intervalSec;
                } else {
                    nextTimeSec += intervalSec;
                }
                targetTimeMillis = startTime + (long) (nextTimeSec * 1000.0);
            } else {
                // chart mode
                targetTimeMillis = startTime + (long) (beat.timestamp * 1000.0);
            }

            long now = System.currentTimeMillis();
            long waitMillis = targetTimeMillis - now;
            if (waitMillis > 0) {
                try {
                    Thread.sleep(waitMillis);
                } catch (InterruptedException e) {
                    if (!running) break;
                }
            }
            if (!running) break;

            long activationTime = System.currentTimeMillis();
            long drift = activationTime - targetTimeMillis;

            byte ledSignal = (byte) (beat.sensorIndex + 1);

            synchronized (lock) {
                currentBeat = beat;
                currentBeatIndex = i;
                currentBeatStartMillis = activationTime;
            }

            System.out.printf(
                "[BeatController] TARGET -> LED %d ON | mode=%s beatIdx=%d " +
                "beatT=%.3fs scheduled=%dms actual=%dms drift=%dms%n",
                ledSignal,
                useIntervalMode ? "INTERVAL" : "CHART",
                i,
                useIntervalMode ? nextTimeSec : beat.timestamp,
                targetTimeMillis - startTime,
                activationTime - startTime,
                drift
            );

            // ❌ NO clearing here. Beat stays ‘current’ until next beat.
            // If you want a visible LED-off on Arduino, handle that there,
            // but logically this beat remains the one we judge against.
        }

        // After all beats are done, clear.
        synchronized (lock) {
            currentBeat = null;
            currentBeatIndex = -1;
            currentBeatStartMillis = 0;
        }

        System.out.println("[BeatController] Sequence complete.");
    }
    @Override
    public void update(ArduinoPacket pkt) {
        int payload = pkt.getPayload();
        System.out.println("[BeatListener] Beat triggered: " + payload);

        BeatControls.Beat beatSnapshot;
        int beatIndexSnapshot;
        long startSnapshot;
        long beatStartMillisSnapshot;

        synchronized (lock) {
            beatSnapshot            = currentBeat;
            beatIndexSnapshot       = currentBeatIndex;
            startSnapshot           = startTime;
            beatStartMillisSnapshot = currentBeatStartMillis;
        }

        if (beatSnapshot == null) {
            long now = System.currentTimeMillis();
            System.out.printf(
                "[BeatListener] Hit %d but NO active beat (t=%.3fs since start)%n",
                payload,
                (now - startSnapshot) / 1000.0
            );
            return;
        }

        long pressTime = System.currentTimeMillis();

        // Visual delta (LED-based, mostly for debugging)
        long deltaVisual = pressTime - beatStartMillisSnapshot;

        // Chart/tempo reference
        long targetTimeMillisTimeline;
        if (useIntervalMode) {
            // in interval mode, "chart time" ~= activation time
            targetTimeMillisTimeline = beatStartMillisSnapshot;
        } else {
            // chart mode uses Beat.timestamp
            targetTimeMillisTimeline =
                    startSnapshot + (long) (beatSnapshot.timestamp * 1000.0);
        }
        long deltaTimeline = pressTime - targetTimeMillisTimeline;

        boolean correctLane  = (payload == beatSnapshot.sensorIndex);
        String judgment;

        if (!correctLane) {
            judgment = "WRONG LANE";
        } else {
            if (Math.abs(deltaTimeline) <= HIT_WINDOW_MS) {
                // inside timing window
                judgment = "GOOD";
            } else if (deltaTimeline > HIT_WINDOW_MS) {
                // pressed after the allowed window
                judgment = "MISS (too late - window passed)";
            } else { // deltaTimeline < -HIT_WINDOW_MS
                // pressed before the window even opened
                judgment = "MISS (too early)";
            }
        }

        System.out.printf(
            "[BeatListener] Beat #%d: expected=%d, got=%d | " +
            "pressT=%.3fs, deltaVisual=%d ms, deltaTimeline=%d ms -> %s%n",
            beatIndexSnapshot,
            beatSnapshot.sensorIndex,
            payload,
            (pressTime - startSnapshot) / 1000.0,
            deltaVisual,
            deltaTimeline,
            judgment
        );
    } 
}
