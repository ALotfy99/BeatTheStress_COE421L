import java.util.ArrayList;
import java.util.List;

import java.util.ArrayList;
import java.util.List;

/**
 * TempoSubject - Wraps Arduino1 (AR1) to notify observers of difficulty changes.
 * Observes a Subject (ArduinoHandler or emulated) and translates AR1 packets into difficulty change events.
 * AR1 payload values: 1=Easy, 2=Medium, 3=Hard
 */
public class TempoSubject implements Subject, Observer {
    private final Subject sourceSubject;
    private final List<Observer> observers = new ArrayList<>();
    
    // Observer for difficulty changes
    public interface DifficultyChangeObserver {
        void onDifficultyChange(int difficultyLevel); // 1=Easy, 2=Medium, 3=Hard
    }
    
    private DifficultyChangeObserver difficultyObserver;
    
    public TempoSubject(Subject sourceSubject) {
        if (sourceSubject == null) {
            throw new IllegalArgumentException("Source Subject cannot be null");
        }
        this.sourceSubject = sourceSubject;
        // Register this subject as an observer of the source subject
        sourceSubject.registerObserver(this);
    }
    
    /**
     * Register an observer for difficulty changes
     */
    public void setDifficultyObserver(DifficultyChangeObserver observer) {
        this.difficultyObserver = observer;
        System.out.println("[TempoSubject] Registered difficulty change observer: " + 
                (observer != null ? observer.getClass().getSimpleName() : "null"));
    }
    
    @Override
    public void registerObserver(Observer o) {
        synchronized (observers) {
            if (!observers.contains(o)) {
                observers.add(o);
                System.out.println("[TempoSubject] Registered observer: " + o.getClass().getSimpleName());
            }
        }
    }
    
    @Override
    public void removeObsever(Observer o) {
        synchronized (observers) {
            observers.remove(o);
            System.out.println("[TempoSubject] Removed observer: " + o.getClass().getSimpleName());
        }
    }
    
    @Override
    public void notifyObservers(ArduinoPacket pkt) {
        List<Observer> copy;
        synchronized (observers) {
            copy = new ArrayList<>(observers);
        }
        for (Observer o : copy) {
            o.update(pkt);
        }
    }
    
    /**
     * Observer implementation - filters AR1 packets and handles difficulty changes
     */
    @Override
    public void update(ArduinoPacket pkt) {
        // Only process packets from Arduino1 (AR1)
        if (pkt.getArduinoID() == 1) {
            int payload = pkt.getPayload() & 0x3F; // Get lower 6 bits
            
            // Map payload to difficulty: 1=Easy, 2=Medium, 3=Hard
            if (payload >= 1 && payload <= 3) {
                System.out.println("[TempoSubject] Received difficulty change from AR1: " + payload + 
                        " (1=Easy, 2=Medium, 3=Hard)");
                
                // Notify difficulty observer
                if (difficultyObserver != null) {
                    difficultyObserver.onDifficultyChange(payload);
                }
            } else {
                System.out.println("[TempoSubject] Invalid AR1 payload: " + payload + " (expected 1-3)");
            }
        }
    }
}

