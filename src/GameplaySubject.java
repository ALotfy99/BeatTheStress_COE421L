import java.util.ArrayList;
import java.util.List;

/**
 * GameplaySubject - Wraps Arduino2 (AR2) to notify observers of pressure sensor hits (pad hits).
 * Observes a Subject (ArduinoHandler or emulated) and translates AR2 packets into gameplay hit events.
 */
public class GameplaySubject implements Subject, Observer {
    private final Subject sourceSubject;
    private final List<Observer> observers = new ArrayList<>();
    
    public GameplaySubject(Subject sourceSubject) {
        if (sourceSubject == null) {
            throw new IllegalArgumentException("Source Subject cannot be null");
        }
        this.sourceSubject = sourceSubject;
        // Register this subject as an observer of the source subject
        sourceSubject.registerObserver(this);
    }
    
    @Override
    public void registerObserver(Observer o) {
        synchronized (observers) {
            if (!observers.contains(o)) {
                observers.add(o);
                System.out.println("[GameplaySubject] Registered observer: " + o.getClass().getSimpleName());
            }
        }
    }
    
    @Override
    public void removeObsever(Observer o) {
        synchronized (observers) {
            observers.remove(o);
            System.out.println("[GameplaySubject] Removed observer: " + o.getClass().getSimpleName());
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
     * Observer implementation - filters AR2 packets and forwards to observers
     */
    @Override
    public void update(ArduinoPacket pkt) {
        // Only forward packets from Arduino2 (AR2)
        if (pkt.getArduinoID() == 2) {
            System.out.println("[GameplaySubject] Received pad hit from AR2: " + pkt.getPayload());
            notifyObservers(pkt);
        }
    }
}

