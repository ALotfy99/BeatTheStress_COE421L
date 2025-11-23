import javax.swing.SwingUtilities;
import jssc.SerialPortException;

/**
 * FunctionalDriver - Production driver that connects to real Arduino hardware.
 * Instantiates real ArduinoHandler instances and connects them to the game system.
 */
public class FunctionalDriver {
    
    public static void main(String[] args) throws SerialPortException {
        
        String[] playlist = {
            "music/KOTON.wav",
            "music/MCR_HOUSE_OF_WOLVES.wav",
            "music/THISISHOWIDISAPPEAR.wav",
            "music/MOZART.wav",
            "music/MARIO.wav",
            "music/ZELDA.wav",
        };
        
        // Create orchestrator (prompts for name and difficulty)
        GameOrchestrator orchestrator = new GameOrchestrator(playlist);
        
        // Arduino port paths - UPDATE THESE FOR YOUR SYSTEM
        String arduino1Port = "/dev/cu.usbserial-A700eDMk";  // AR1: Tempo
        String arduino2Port = "/dev/cu.usbserial-A10LIIP2";  // AR2: Beat hits
        String arduino3Port = "/dev/cu.usbserial-A10LIDLA";  // AR3: Button controls
        
        System.out.println("[FunctionalDriver] Initializing real Arduino connections...");
        System.out.println("[FunctionalDriver] AR1 (Tempo): " + arduino1Port);
        System.out.println("[FunctionalDriver] AR2 (Gameplay): " + arduino2Port);
        System.out.println("[FunctionalDriver] AR3 (System Control): " + arduino3Port);
        
        // Initialize Arduino handlers
        ArduinoHandler ar1 = new ArduinoHandler(arduino1Port, 1);
        ArduinoHandler ar2 = new ArduinoHandler(arduino2Port, 2);
        ArduinoHandler ar3 = new ArduinoHandler(arduino3Port, 3);
        
        System.out.println("[FunctionalDriver] Arduino handlers initialized");
        
       
        
        // Initialize all components with real Arduino handlers
        orchestrator.initializeFunctional(ar1, ar2, ar3);
        
        System.out.println("[FunctionalDriver] Game started successfully!");
        System.out.println("[FunctionalDriver] AR1: Tempo changes | AR2: Pad hits | AR3: Level controls");
        System.out.println("[FunctionalDriver] AR3 Button 0: Previous | Button 1: Next | Button 2: Pause/Resume");
    }
}

