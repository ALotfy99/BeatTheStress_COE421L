import java.util.ArrayList;
import java.util.List;

/**
 * SystemControlSubject - Wraps Arduino3 (AR3) to notify observers of level changes and pause/resume states.
 * Observes a Subject (ArduinoHandler or emulated) and translates AR3 packets into system control events.
 */
public class SystemControlSubject implements Subject, Observer {
    private final Subject sourceSubject;
    private final List<Observer> observers = new ArrayList<>();
    
    public SystemControlSubject(Subject sourceSubject) {
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
                System.out.println("[SystemControlSubject] Registered observer: " + o.getClass().getSimpleName());
            }
        }
    }
    
    @Override
    public void removeObsever(Observer o) {
        synchronized (observers) {
            observers.remove(o);
            System.out.println("[SystemControlSubject] Removed observer: " + o.getClass().getSimpleName());
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
     * Observer implementation - filters AR3 packets and forwards to observers
     */
    @Override
    public void update(ArduinoPacket pkt) {
        // Only forward packets from Arduino3 (AR3)
        if (pkt.getArduinoID() == 3) {
            int buttonID = pkt.getPayload() & 0x03;
            System.out.println("[SystemControlSubject] Received control from AR3: button=" + buttonID);
            notifyObservers(pkt);
        }
    }
}

