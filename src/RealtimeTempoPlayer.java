import javax.sound.sampled.*;
import java.io.*;

/**
 * Real-time tempo-changing music player (no external libraries)
 * Uses simple linear interpolation for tempo changes
 * 
 * NOTE: This is a basic implementation. Production quality needs:
 * - Phase vocoder or advanced time-stretching
 * - Anti-aliasing filters
 * - Proper buffering and threading
 */
public class RealtimeTempoPlayer implements MusicPlayer, Runnable {
    
    private final String[] playlist;
    private volatile double tempoFactor = 1.0;
    private volatile boolean isPlaying = false;
    private volatile boolean isPaused = false;
    private volatile boolean shouldStop = false;
    
    private Thread playbackThread;
    private SourceDataLine audioLine;
    private short[] audioSamples;
    private AudioFormat audioFormat;
    private int currentSongIndex = -1;
    
    // Playback state
    private double playbackPosition = 0.0;  // fractional sample position
    
    public RealtimeTempoPlayer(String[] playlist) {
        this.playlist = (playlist != null) ? playlist.clone() : new String[0];
        System.out.println("[RealtimeTempoPlayer] Initialized with " + playlist.length + " songs");
    }
    
    @Override
    public void play(int songIndex) {
        System.out.println("[RealtimeTempoPlayer] play(" + songIndex + ")");
        
        if (songIndex < 0 || songIndex >= playlist.length) {
            System.err.println("Invalid song index");
            return;
        }
        
        // Stop current playback
        stop();
        
        try {
            currentSongIndex = songIndex;
            loadAudioFile(playlist[songIndex]);
            startPlayback();
        } catch (Exception e) {
            System.err.println("Error loading audio:");
            e.printStackTrace();
        }
    }
    
    private void loadAudioFile(String filePath) throws Exception {
        System.out.println("[Load] Loading: " + filePath);
        
        File file = new File(filePath);
        AudioInputStream ais = AudioSystem.getAudioInputStream(file);
        audioFormat = ais.getFormat();
        
        System.out.println("[Load] Format: " + audioFormat);
        
        // Convert to PCM if needed
        if (audioFormat.getEncoding() != AudioFormat.Encoding.PCM_SIGNED) {
            AudioFormat pcmFormat = new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                audioFormat.getSampleRate(), 16, audioFormat.getChannels(),
                audioFormat.getChannels() * 2, audioFormat.getSampleRate(), false
            );
            ais = AudioSystem.getAudioInputStream(pcmFormat, ais);
            audioFormat = pcmFormat;
        }
        
        // Read all audio data
        byte[] audioBytes = ais.readAllBytes();
        ais.close();
        
        // Convert to samples (assuming 16-bit)
        int numSamples = audioBytes.length / 2;
        audioSamples = new short[numSamples];
        
        for (int i = 0; i < numSamples; i++) {
            int idx = i * 2;
            audioSamples[i] = (short) ((audioBytes[idx + 1] << 8) | (audioBytes[idx] & 0xFF));
        }
        
        System.out.println("[Load] Loaded " + audioSamples.length + " samples");
    }
    
    private void startPlayback() throws LineUnavailableException {
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
        audioLine = (SourceDataLine) AudioSystem.getLine(info);
        audioLine.open(audioFormat);
        audioLine.start();
        
        playbackPosition = 0.0;
        isPlaying = true;
        isPaused = false;
        shouldStop = false;
        
        playbackThread = new Thread(this);
        playbackThread.start();
        
        System.out.println("[Playback] Started");
    }
    
    @Override
    public void run() {
        byte[] buffer = new byte[4096];
        short[] processedSamples = new short[buffer.length / 2];
        
        while (isPlaying && !shouldStop) {
            if (isPaused) {
                try { Thread.sleep(10); } catch (InterruptedException e) {}
                continue;
            }
            
            // Process audio with current tempo
            int samplesGenerated = generateSamples(processedSamples, tempoFactor);
            
            if (samplesGenerated == 0) {
                // End of song
                isPlaying = false;
                break;
            }
            
            // Convert to bytes and write to audio line
            for (int i = 0; i < samplesGenerated; i++) {
                buffer[i * 2] = (byte) (processedSamples[i] & 0xFF);
                buffer[i * 2 + 1] = (byte) ((processedSamples[i] >> 8) & 0xFF);
            }
            
            audioLine.write(buffer, 0, samplesGenerated * 2);
        }
        
        audioLine.drain();
        audioLine.stop();
        audioLine.close();
        System.out.println("[Playback] Stopped");
    }
    
    /**
     * Generate samples with tempo scaling using linear interpolation
     * (Simple but introduces some artifacts - good enough for demo)
     */
    private int generateSamples(short[] output, double tempo) {
        int samplesNeeded = output.length;
        int samplesGenerated = 0;
        
        // Step size in source audio based on tempo
        // tempo = 2.0 means we move twice as fast through source
        double step = tempo;
        
        while (samplesGenerated < samplesNeeded) {
            int pos = (int) playbackPosition;
            
            // Check if we've reached the end
            if (pos >= audioSamples.length - 1) {
                break;
            }
            
            // Linear interpolation between samples
            double frac = playbackPosition - pos;
            short sample1 = audioSamples[pos];
            short sample2 = audioSamples[pos + 1];
            
            output[samplesGenerated] = (short) (sample1 + frac * (sample2 - sample1));
            
            playbackPosition += step;
            samplesGenerated++;
        }
        
        return samplesGenerated;
    }
    
    @Override
    public void pause() {
        isPaused = true;
        System.out.println("[Playback] Paused");
    }
    
    @Override
    public void resume() {
        isPaused = false;
        System.out.println("[Playback] Resumed");
    }
    
    @Override
    public void stop() {
        shouldStop = true;
        isPlaying = false;
        
        if (playbackThread != null) {
            try {
                playbackThread.join(1000);
            } catch (InterruptedException e) {}
        }
        
        if (audioLine != null) {
            audioLine.stop();
            audioLine.close();
            audioLine = null;
        }
        
        System.out.println("[Playback] Stopped");
    }
    
    @Override
    public void setTempo(double factor) {
        double clamped = Math.max(0.5, Math.min(factor, 2.0));
        tempoFactor = clamped;
        System.out.println("[Tempo] Set to " + String.format("%.2fx", tempoFactor));
    }
}
    
