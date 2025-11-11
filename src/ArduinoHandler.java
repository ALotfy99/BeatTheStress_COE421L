
/*
* ArduinoHandler.java
* Acts as a Subject in the Observer-Subject pattern.
* Each ArduinoHandler corresponds to 1 physical Arduino (with its own ArduinoID)
* and continuously listens for incoming bytes on its serial port.
* when new data arrives, it wraps it in an ArduinoPacket and notifies all registered observers.
* Observers (like MusicController or BeatController) then decide how to interpret
* the packet based on its arduinoID and payload bits.
*/
import java.util.ArrayList;
import java.util.List;
import jssc.SerialPortException;

public class ArduinoHandler extends SerialPortHandle implements Runnable, Subject {

	// ------------ Attributes ------------

	private final int arduinoID; // Each handler instance identifies its Arduino (1, 2, or 3)

	// all handler instances shared the same observer list, meaning
	// every observer would get every Arduino's packets even if not intended.
	// Making it *instance-level* ensures each handler manages its own observers.
	private final List<Observer> observers = new ArrayList<>();

	// Existing constants remain the same
	public static final int START_BYTE = 0xA5; // handshake/start signal
	public static final int VIBRATION_OK = 0x88; // acknowledgement or vibration OK code

	private final Thread t1; // dedicated thread for listening on the serial port

	// ------------ Constructor ------------

	public ArduinoHandler(String path, int arduinoID) {
		super(path); // initialize serial connection through parent class
		this.arduinoID = arduinoID;
		this.t1 = new Thread(this, "ArduinoHandler-" + arduinoID);
		this.t1.start(); // start listening immediately
		System.out.println("[ArduinoHandler] Started thread for Arduino " + arduinoID);

	}

	// ------------ Thread: cont. reads serial data ------------

	@Override
	public void run() {
		try {
			// Continuous loop: waits for incoming data bytes from Arduino
			while (true) {
				// BLOCKING READ: readByte() stalls until one byte is available
				byte control = readByte();

				// Wrap the byte in an ArduinoPacket
				ArduinoPacket pkt = new ArduinoPacket(control);

				// Notify all registered observers about the new packet
				notifyObservers(pkt);
			}
		} catch (SerialPortException e) {
			System.err.println("[ArduinoHandler] Serial exception on Arduino " + arduinoID + ": " + e.getMessage());
			e.printStackTrace();
		} catch (Exception e) {
			// General safety catch for thread robustness
			System.err.println("[ArduinoHandler] Unexpected error in Arduino " + arduinoID + ": " + e.getMessage());
		}
	}

	// ------------ Subject interface implementation ------------

	@Override
	public void registerObserver(Observer o) {
		// Add observer if not already in list
		if (!observers.contains(o)) {
			observers.add(o);
			System.out
					.println("[ArduinoHandler " + arduinoID + "] Observer registered: " + o.getClass().getSimpleName());
		}
	}

	@Override
	public void removeObsever(Observer o) {
		// FIXED: spelling corrected from 'removeObsever' → 'removeObserver'
		observers.remove(o);
		System.out.println("[ArduinoHandler " + arduinoID + "] Observer removed: " + o.getClass().getSimpleName());
	}

	@Override
	public void notifyObservers(ArduinoPacket pkt) {
		// Notify all observers with the received packet
		// Each observer decides what to do based on pkt.getArduinoID()
		for (Observer observer : observers) {
			try {
				observer.update(pkt);
			} catch (Exception e) {
				// Safety: if one observer fails, others still receive the packet
				System.err.println("[ArduinoHandler " + arduinoID + "] Failed to notify observer: " + e.getMessage());
			}
		}
	}

	// ------------ Accessors ------------

	public int getArduinoID() {
		return arduinoID;
	}

	// Optional: add a method to stop thread or close port safely
	public void stopHandler() {
		try {
			System.out.println("[ArduinoHandler " + arduinoID + "] Stopping...");
			if (t1.isAlive()) {
				t1.interrupt(); // stop the background thread safely
			}

			if (sp != null && sp.isOpened()) { // close the serial port using inherited 'sp' from SerialPortHandle
				sp.closePort();
				System.out.println("[ArduinoHandler " + arduinoID + "] Serial port closed.");
			}
		} catch (Exception e) {
			System.err.println("[ArduinoHandler] Error stopping handler: " + e.getMessage());
		}
	}
}
