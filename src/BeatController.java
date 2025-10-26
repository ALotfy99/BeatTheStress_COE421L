// BeatController.java
// Threaded LED control for Beat The Stress (communicates with Arduino2)

import java.util.List;
import jssc.SerialPortException;

/**
 * BeatController runs independently from the main game logic.
 * It sends LED commands to Arduino2 at the correct timestamps.
 */
public class BeatController extends Thread {

    private final List<BeatControls.Beat> beats;
    private final ArduinoHandler arduino2Handler;
    private boolean running = true;
    private long startTime;

    public interface BeatListener {
        void onBeatTriggered(BeatControls.Beat beat);
    }

    private final BeatListener listener;

    public BeatController(List<BeatControls.Beat> beats,
                          ArduinoHandler arduino2Handler,
                          BeatListener listener) {
        this.beats = beats;
        this.arduino2Handler = arduino2Handler;
        this.listener = listener;
    }

    public void stopController() {
        running = false;
        this.interrupt();
    }

    public long getStartTime() {
        return startTime;
    }

    @Override
    public void run() {
        if (beats == null || beats.isEmpty()) {
            System.err.println("[BeatController] No beats to execute.");
            return;
        }

        startTime = System.currentTimeMillis();
        System.out.println("[BeatController] Thread started: " + beats.size() + " beats.");

        for (BeatControls.Beat beat : beats) {
            if (!running) break;

            double elapsed = (System.currentTimeMillis() - startTime) / 1000.0;
            double waitTime = beat.timestamp - elapsed;

            if (waitTime > 0) {
                try { Thread.sleep((long) (waitTime * 1000)); }
                catch (InterruptedException e) { if (!running) break; }
            }

            byte ledSignal = (byte) (beat.sensorIndex + 1);
            try {
                arduino2Handler.writeByte(ledSignal);
                System.out.printf("[BeatController] LED %d ON (t=%.2fs)%n", ledSignal, beat.timestamp);
            } catch (SerialPortException e) {
                System.err.println("[BeatController] Serial write error: " + e.getMessage());
            }

            if (listener != null) listener.onBeatTriggered(beat);

            try { Thread.sleep(100); } catch (InterruptedException ignored) {}
        }

        System.out.println("[BeatController] Sequence complete.");
    }
}
