// MusicController is an Observer
// and also a Thread (Runnable) that controls the music playback loop.
import java.util.ArrayList;

public class MusicController implements Runnable, Observer {

    // MusicPlayer backend
    private final MusicPlayer player;
    private Subject[] subjects; // who to listen to data from

    // Tempo control
    private double targetTempoScale = 1.0;  // desired tempo factor
    private double currentTempoScale = 1.0; // what the player is currently using

    // Smoothing for tempo changes (to avoid instant jumps)
    private static final double TEMPO_SMOOTHING_STEP = 0.05; // 5% per update tick
    private static final long TEMPO_UPDATE_INTERVAL_MS = 50; // update every 50 ms

    // Song selection
    private int currentSongIndex = 0;
    private final String[] totalSongs;

    // Optional: simple play/pause state
    private boolean isPaused = false;
    private boolean running = true;

    
    private Thread t1;
    public MusicController(MusicPlayer player, String[] totalSongs, Subject[] subjects) {
        this.player = player;
        this.totalSongs = totalSongs;
        this.subjects = subjects; //THIS Class supports multiple subjects!!!!!
        
        // register yourself with each subject that exists
        for (int i = 0; i < subjects.length; i++) {
        	subjects[i].registerObserver(this); //register yourself!
        }
        this.t1 = new Thread(this);
        t1.start();
    }


    public void setTempo(float scaleValue) {
    	// TODO: MAP FROM [0,5] -> [0.8,1.3]
        // Example mapping:
        //   base = 1.0x
        //   each step adds 0.1x  ->  scale 0 -> 1.0x, scale 5 -> 1.5x, etc.
        double clamped = Math.max(0.0, Math.min(scaleValue, 7.0));
        double newTempo = 1.0 + 0.1 * clamped;

        targetTempoScale = newTempo;
        System.out.printf("[MusicController] Heart rate scale=%.2f -> target tempo=%.2fx%n",
                scaleValue, targetTempoScale);
        player.setTempo(newTempo); //set the tempo
    }
    
    @Override
    public synchronized void update(ArduinoPacket pkt) {
    	// CHECK if it's Arduino1
    	if (pkt.getArduinoID() == 1) {
    		int scaleValue = pkt.getPayload() & 0x07; // last 3 bits
    		setTempo(scaleValue); //forward packet to setTempo Method
    	}
    	else if (pkt.getArduinoID() == 3) {
    		int buttonID = (pkt.getPayload() & 0x03);
    		decodeButtonOP(buttonID);
    	}
    }

    public synchronized void decodeButtonOP(int buttonIndex) {
        // Button mapping example:
        // 0 -> Previous song
        // 1 -> Next song
        // 2 -> Play/Pause toggle
        // 3 -> NOP (unused)
        switch (buttonIndex) {
            case 0:
                previousSong();
                break;
            case 1:
                nextSong();
                break;
            case 2:
                togglePlayPause();
                break;
            default:
                // 3 or any unknown value: ignore
                System.out.println("[MusicController] NOP button press ignored: " + buttonIndex);
                break;
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

    // =========================
    // Public control API
    // =========================



    // =========================
    // Internal helpers (song control)
    // =========================

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

    // =========================
    // Internal helpers (tempo smoothing)
    // =========================

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

}
