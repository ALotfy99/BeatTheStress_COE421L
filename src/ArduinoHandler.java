/*
 * Ardunio Handler is a Subject Class that notifies the interested observers with the Arduino Packet recieved
 * The interested observers can then deal with the Arduino Packet as they wish.
 * */
import java.util.ArrayList;
import jssc.SerialPortException;

public class ArduinoHandler extends SerialPortHandle implements Runnable, Subject {
	//attributes
	private int arduinoID;
	private static final ArrayList<Observer> observers = new ArrayList<Observer>();

	public static final int START_BYTE  = 0xA5;   // [AI] distinct start/handshake byte
	public static final int VIBRATION_OK = 0x88;
	private Thread t1;
	
	// constructor
	public ArduinoHandler(
			String path, 
			int arduinoID
			) {
		super(path);
		this.arduinoID = arduinoID;
		t1 = new Thread(this);
		t1.start();
	}
	
	// start function
	@Override
	public void run() {
		
		try {			
			// thread runs
			while (true) { 		// wait for data
				byte control = readByte(); // program stalls to read for any byte coming
				ArduinoPacket pkt = new ArduinoPacket(control); // establish a packet based on your data
				notifyObservers(pkt); //notify your interested observers
			}
		} catch (SerialPortException e) {
			e.printStackTrace();
		}	
	}
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
		for (int i = 0; i < observers.size(); i++)
		{
			Observer observer = observers.get(i);
			observer.update(pkt); //it is the specific observer JOB to figure out the packet mapping
		}
	}


}
