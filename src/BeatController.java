// BeatController.java
// Refactored to implement the Observer-Subject pattern
// This version makes BeatController both an Observer (it listens to Arduino inputs)
// and a Subject (it can notify other parts of the system about beat hits and scoring).

import java.util.List;
import java.util.ArrayList;
import jssc.SerialPortException;

/**
 * BeatController runs independently from the main game logic.
 * It sends LED commands to Arduino2 at the correct timestamps.
 */
public class BeatController extends Thread implements Observer, Subject {

    private final List<BeatControls.Beat> beats; // sequence of beats with timestamps
    private boolean running = true;
    private long startTime;

    // New fields added for Observer-Subject structure
    private final Subject[] subjects; // Arduino subjects to listen to (A2, A3, etc.)
    private final ArduinoHandler ledHandler; // used to send LED feedback (e.g., Arduino3Handler)
    private final List<Observer> observers = new ArrayList<>(); // Observers of BeatController (e.g., GameController)

    // ------------ Constructor -------------
    public BeatController(
            List<BeatControls.Beat> beats,
            ArduinoHandler ledHandler,
            Subject[] subjects) {
        this.beats = beats;
        this.ledHandler = ledHandler;
        this.subjects = subjects;

        // Register this BeatController as an observer of each subject
        // ->This is how it "listens" to pad hits and button presses
        for (Subject s : subjects) {
            s.registerObserver(this);
        }
    }

    // ------------ Thread control -------------

    public void stopController() {
        running = false;
        this.interrupt(); // interrupt sleep if needed
    }

    public long getStartTime() {
        return startTime;
    }

    // ------------ Observer interface implementation -------------

    @Override
    public synchronized void update(ArduinoPacket pkt) {
        // this method is called automatically whenever a Subject (Arduino handler)
        // notifies its observers with new data

        int arduinoID = pkt.getArduinoID();
        int payload = pkt.getPayload();

        // Arduino 2: Pressure pads --> user input
        if (arduinoID == 0x02) {
            int padID = payload & 0x03; // extract pad index
            evaluatePadHit(padID);
        }

        // Arduino 3: Buttons --> control events
        else if (arduinoID == 0x03) {
            int buttonId = payload & 0x03;
            handleButton(buttonId);
        }
    }

    // ------------ Subject interface implementation -------------

    @Override
    public void registerObserver(Observer o) {
        observers.add(o);
    }

    @Override
    public void removeObsever(Observer o) {
        observers.remove(o);
    }

    @Override
    public void notifyObservers(ArduinoPacket pkt) {
        // This allows BeatController to notify others (e.g., GameController)
        // when an important event happens (beat hit, miss, etc.)
        for (Observer o : observers) {
            o.update(pkt);
        }
    }

    // ------------ Main Beat Timing Loop -------------

    @Override
    public void run() {
        if (beats == null || beats.isEmpty()) {
            System.err.println("[BeatController] No beats to execute.");
            return;
        }

        startTime = System.currentTimeMillis();
        System.out.println("[BeatController] Thread started: " + beats.size() + " beats.");

        for (BeatControls.Beat beat : beats) {
            if (!running)
                break;

            double elapsed = (System.currentTimeMillis() - startTime) / 1000.0;
            double waitTime = beat.timestamp - elapsed;

            if (waitTime > 0) {
                try {
                    Thread.sleep((long) (waitTime * 1000));
                } catch (InterruptedException e) {
                    if (!running)
                        break;
                }
            }

            // Send LED signal to indicate which pad should be pressed
            triggerLed(beat.sensorIndex);

            // Notify any observers (like the UI) that a beat has been triggered
            ArduinoPacket ledEvent = new ArduinoPacket((byte) ((3 << 6) | (beat.sensorIndex & 0x3F)));
            notifyObservers(ledEvent);

            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {
            }
        }

        System.out.println("[BeatController] Sequence complete.");
    }

    // ------------ Helper Method: LED trigger -------------

    private void triggerLed(int index) {
        try {
            byte ledSignal = (byte) (index + 1);
            ledHandler.writeByte(ledSignal);
            System.out.printf("[BeatController] LED %d ON%n", ledSignal);
        } catch (SerialPortException e) {
            System.err.println("[BeatController] Serial write error: " + e.getMessage());
        }
    }

    // ------------ Helper Method: Evaluate Pad Hit Time -------------

    private void evaluatePadHit(int padID) {
        // compare when the pad hit vs. expected beat time
        double currentTime = (System.currentTimeMillis() - startTime) / 1000.0;
        BeatControls.Beat closest = null;
        double minDiff = Double.MAX_VALUE;

        for (BeatControls.Beat b : beats) {
            double diff = Math.abs(b.timestamp - currentTime);
            if (diff < minDiff) {
                minDiff = diff;
                closest = b;
            }
        }

        if (closest != null) {
            String accuracy;
            if (minDiff < 0.1)
                accuracy = "PERFECT";
            else if (minDiff < 0.25)
                accuracy = "GOOD";
            else
                accuracy = "MISS";

            System.out.printf("[BeatController] Pad %d hit (%.2fs) -> %s%n", padID, currentTime, accuracy);

            // Notify observers (e.g., scoring system)
            ArduinoPacket result = new ArduinoPacket((byte) ((2 << 6) | (padID & 0x3F)));
            notifyObservers(result);
        }
    }

    // ------------ Helper Method: button handling from Arduino3 -------------

    private void handleButton(int buttonID) {
        switch (buttonID) {
            case 0 -> System.out.println("[BeatController] Button 0: Restart sequence");
            case 1 -> System.out.println("[BeatController] Button 1: Skip to next");
            default -> System.out.println("[BeatController] Unknown button: " + buttonID);
        }
    }
}
