import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.ArrayList;

/**
 * EmulatedDriver - Development/Testing driver that simulates hardware via keyboard.
 * DOES NOT connect to Arduino. Maps keyboard keys to hardware interrupts.
 * 
 * Key mappings:
 *   I -> AR2 Beat Pad 1
 *   J -> AR2 Beat Pad 2
 *   K -> AR2 Beat Pad 3
 *   L -> AR2 Beat Pad 4
 *   u -> AR1 Speed Up
 *   z -> AR3 Previous Level
 *   x -> AR3 Next Level
 *   c -> AR3 Pause/Stop Song
 */
public class EmulatedDriver {
    
    /**
     * Emulated Subject that simulates Arduino packets without real hardware
     */
    private static class EmulatedSubject implements Subject, Observer {
        private final int arduinoId;
        private final ArrayList<Observer> observers = new ArrayList<>();
        
        public EmulatedSubject(int arduinoId) {
            this.arduinoId = arduinoId;
            System.out.println("[EmulatedSubject] Created for AR" + arduinoId);
        }
        
        @Override
        public void registerObserver(Observer o) {
            synchronized (observers) {
                if (!observers.contains(o)) {
                    observers.add(o);
                    System.out.println("[EmulatedSubject AR" + arduinoId + "] Registered observer: " + 
                            o.getClass().getSimpleName());
                }
            }
        }
        
        @Override
        public void removeObsever(Observer o) {
            synchronized (observers) {
                observers.remove(o);
            }
        }
        
        @Override
        public void notifyObservers(ArduinoPacket pkt) {
            ArrayList<Observer> copy;
            synchronized (observers) {
                copy = new ArrayList<>(observers);
            }
            for (Observer o : copy) {
                o.update(pkt);
            }
        }
        
        @Override
        public void update(ArduinoPacket pkt) {
            // This subject can also observe other subjects if needed
            notifyObservers(pkt);
        }
        
        /**
         * Simulate a packet from this Arduino
         */
        public void simulatePacket(byte packetData) {
            ArduinoPacket pkt = new ArduinoPacket(packetData);
            System.out.println("[EmulatedSubject AR" + arduinoId + "] Simulating packet: " + 
                    pkt.getPayload());
            notifyObservers(pkt);
        }
    }
    
    private EmulatedSubject tempoSubject;
    private EmulatedSubject gameplaySubject;
    private EmulatedSubject systemControlSubject;
    private GameOrchestrator orchestrator;
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new EmulatedDriver().start();
        });
    }
    
    private void start() {
        String[] playlist = {
            "music/KOTON.wav",
            "music/MCR_HOUSE_OF_WOLVES.wav",
            "music/THISISHOWIDISAPPEAR.wav",
            "music/MOZART.wav",
            "music/MARIO.wav",
            "music/ZELDA.wav",
        };
        
        System.out.println("[EmulatedDriver] Initializing emulated mode...");
        System.out.println("[EmulatedDriver] Keyboard controls:");
        System.out.println("  i -> Beat Pad 4 (AR2)");
        System.out.println("  j -> Beat Pad 2 (AR2)");
        System.out.println("  k -> Beat Pad 1 (AR2)");
        System.out.println("  l -> Beat Pad 3 (AR2)");
        System.out.println("  u -> Cycle Difficulty (AR1: 1=Easy, 2=Medium, 3=Hard)");
        System.out.println("  z -> Previous Level (AR3)");
        System.out.println("  x -> Next Level (AR3)");
        System.out.println("  c -> Pause/Resume (AR3)");
        
        // Create emulated subjects (no Arduino handlers needed)
        tempoSubject = new EmulatedSubject(1);
        gameplaySubject = new EmulatedSubject(2);
        systemControlSubject = new EmulatedSubject(3);
        
        // Create orchestrator (prompts for name and difficulty)
        orchestrator = new GameOrchestrator(playlist);
        
        // Wrap emulated subjects with the appropriate wrapper classes that filter by Arduino ID
        TempoSubject tempoWrapper = new TempoSubject(tempoSubject);
        GameplaySubject gameplayWrapper = new GameplaySubject(gameplaySubject);
        SystemControlSubject systemWrapper = new SystemControlSubject(systemControlSubject);
        
        // Initialize all components with wrapped emulated subjects
        orchestrator.initializeEmulated(tempoWrapper, gameplayWrapper, systemWrapper);
        
        // Create keyboard listener
        setupKeyboardListener();
        
        System.out.println("[EmulatedDriver] Game started in emulated mode!");
        System.out.println("[EmulatedDriver] Focus the game window and use keyboard controls");
    }
    
    private void setupKeyboardListener() {
        KeyboardFocusManager.getCurrentKeyboardFocusManager()
            .addKeyEventDispatcher(new KeyEventDispatcher() {
                @Override
                public boolean dispatchKeyEvent(KeyEvent e) {
                    if (e.getID() == KeyEvent.KEY_PRESSED) {
                        handleKeyPress(e.getKeyCode());
                    }
                    return false;
                }
            });
    }
    
    private void handleKeyPress(int keyCode) {
        switch (keyCode) {
            case KeyEvent.VK_I:
                // AR2 Beat Pad 4
                simulateGameplayHit(4);
                break;
            case KeyEvent.VK_J:
                // AR2 Beat Pad 2
                simulateGameplayHit(2);
                break;
            case KeyEvent.VK_K:
                // AR2 Beat Pad 1
                simulateGameplayHit(1);
                break;
            case KeyEvent.VK_L:
                // AR2 Beat Pad 3
                simulateGameplayHit(3);
                break;
            case KeyEvent.VK_U:
                // Simulate AR1 difficulty change (cycle: 1->2->3->1)
                // Get current difficulty and cycle to next
                if (orchestrator != null) {
                    DifficultyStrategy current = orchestrator.getDifficultyStrategy();
                    int currentLevel = current.getLevel();
                    int nextLevel = (currentLevel % 3) + 1; // Cycle: 1->2, 2->3, 3->1
                    simulateDifficultyChange(nextLevel);
                }
                break;
            case KeyEvent.VK_Z:
                // AR3 Previous Level
                simulateSystemControl(0);
                break;
            case KeyEvent.VK_X:
                // AR3 Next Level
                simulateSystemControl(1);
                break;
            case KeyEvent.VK_C:
                // AR3 Pause/Resume
                simulateSystemControl(2);
                break;
        }
    }
    
    private void simulateGameplayHit(int padNumber) {
        // Pad number is 1-4, need to encode as Arduino2 packet
        // Format: ArduinoID (2) in upper 2 bits, payload (padNumber-1) in lower 6 bits
        byte packetData = (byte) ((2 << 6) | ((padNumber - 1) & 0x3F));
        gameplaySubject.simulatePacket(packetData);
    }
    
    private void simulateDifficultyChange(int difficultyLevel) {
        // Difficulty level 1-3, encode as Arduino1 packet
        // Format: ArduinoID (1) in upper 2 bits, payload (1-3) in lower 6 bits
        // 1=Easy, 2=Medium, 3=Hard
        int payload = Math.max(1, Math.min(difficultyLevel, 3)); // Clamp 1-3
        byte packetData = (byte) ((1 << 6) | (payload & 0x3F));
        tempoSubject.simulatePacket(packetData);
    }
    
    private void simulateSystemControl(int buttonID) {
        // Button ID 0-3, encode as Arduino3 packet
        // Format: ArduinoID (3) in upper 2 bits, buttonID in lower 6 bits
        byte packetData = (byte) ((3 << 6) | (buttonID & 0x3F));
        systemControlSubject.simulatePacket(packetData);
    }
}

