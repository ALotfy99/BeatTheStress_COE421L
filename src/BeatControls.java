// BeatControls.java
// Defines beat sequences (LED timings) for Beat The Stress

import java.util.ArrayList;
import java.util.List;

/**
 * BeatControls is the rhythm sheet for the game.
 * It defines when (timestamp) and which LED (sensor index) should activate.
 * BeatController reads from this to trigger LEDs.
 */
public class BeatControls {

    /** Represents one beat in the rhythm timeline. */
    public static class Beat {
        public double timestamp;   // Time in seconds since song start
        public int sensorIndex;    // 0–3 for each pad/LED

        public Beat(double timestamp, int sensorIndex) {
            this.timestamp = timestamp;
            this.sensorIndex = sensorIndex;
        }

        @Override
        public String toString() {
            return "Beat{time=" + timestamp + ", sensor=" + sensorIndex + "}";
        }
    }

    private final List<Beat> beats;

    public BeatControls(int songIndex) {
        this.beats = loadBeatsForSong(songIndex);
    }

    private List<Beat> loadBeatsForSong(int songIndex) {
        List<Beat> list = new ArrayList<>();

        switch (songIndex) {
            case 0 -> { // Song 1 – calm starter
                list.add(new Beat(0.5, 0));
                list.add(new Beat(1.0, 2));
                list.add(new Beat(1.5, 1));
                list.add(new Beat(2.0, 3));
                list.add(new Beat(2.5, 0));
                list.add(new Beat(3.0, 2));
            }
            case 1 -> { // Song 2 – moderate tempo
                list.add(new Beat(0.4, 1));
                list.add(new Beat(0.9, 3));
                list.add(new Beat(1.3, 0));
                list.add(new Beat(1.8, 2));
                list.add(new Beat(2.2, 1));
                list.add(new Beat(2.6, 3));
                list.add(new Beat(3.1, 0));
            }
            case 2 -> { // Song 3 – upbeat
                list.add(new Beat(0.6, 2));
                list.add(new Beat(1.1, 0));
                list.add(new Beat(1.6, 1));
                list.add(new Beat(2.1, 3));
                list.add(new Beat(2.5, 2));
                list.add(new Beat(3.0, 0));
            }
            case 3 -> { // Song 4 – energetic
                list.add(new Beat(0.5, 0));
                list.add(new Beat(1.0, 3));
                list.add(new Beat(1.4, 1));
                list.add(new Beat(1.8, 2));
                list.add(new Beat(2.3, 0));
                list.add(new Beat(2.7, 3));
                list.add(new Beat(3.1, 1));
            }
            case 4 -> { // Song 5 – relaxed closer
                list.add(new Beat(0.7, 2));
                list.add(new Beat(1.3, 0));
                list.add(new Beat(1.9, 3));
                list.add(new Beat(2.4, 1));
                list.add(new Beat(3.0, 2));
                list.add(new Beat(3.6, 0));
            }
            default -> System.err.println("[BeatControls] Invalid song index: " + songIndex);
        }
        return list;
    }

    public List<Beat> getBeats() { return beats; }
    public int size() { return beats.size(); }
}
