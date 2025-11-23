/*
 * Ardunio Handler is a Subject Class that notifies the interested observers with the Arduino Packet recieved
 * The interested observers can then deal with the Arduino Packet as they wish.
 * */
import java.util.ArrayList;
import jssc.SerialPortException;

public class ArduinoHandler extends SerialPortHandle implements Runnable, Subject {
	//attributes
	private int arduinoID;
	private final ArrayList<Observer> observers = new ArrayList<Observer>();

	public static final int START_BYTE  = 0xA5;
	public static final int ACK_BYTE = 0xB5;
	public static final int EXIT_CODE = 0x99;

	private Thread t1;
	private boolean initialized;
	
	// constructor
	public ArduinoHandler(
			String path, 
			int arduinoID
			) throws SerialPortException {
		super(path);
		this.arduinoID = arduinoID;
		this.initialized = false;
		t1 = new Thread(this);
		init(); //initialize FIRST!
		t1.start(); //start thread after initialization

	}
	// a function to establish communication with the Arduino
	public void init() throws SerialPortException {
		// send START_BYTE
		// SPAM START_BYTE 5 times until the arduino gets it
		if (!sp.isOpened()) sp.openPort();
		System.out.println("[ArduinoHandler " + arduinoID + "] is sending START_BYTE.");
		for (int i = 0; i < 5; i++) {
			this.writeByte((byte) START_BYTE); //send start byte
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} //with 10 ms delay
		}
		
		System.out.println("[ArduinoHandler " + arduinoID + "] is trying to read ACK_BYTE.");
		if (this.readByte() == (byte) ACK_BYTE) {
			// if the received byte is the ACK_BYTE, then you can start
			System.out.println("[ArduinoHandler " + arduinoID + "] started.");
			System.out.println(readLine()); // read the response
			this.initialized = true;

		}
	}
	// start function
	@Override
	public void run() {
		while (this.initialized == false); // LOCK AND DO NOT RUN
		System.out.println("[ArduinoHandler " + arduinoID + "] is running.");
		// Thread running process
		try {			
			// thread runs
			while (true) { 		// wait for data
				//System.out.println("Arduino Handler is reading data");
				byte control = readByte(); // program stalls to read for any byte coming
				ArduinoPacket pkt = new ArduinoPacket(control); // establish a packet based on your data
				System.out.println("[ArduinoHandler " + arduinoID + "] recieved: "+pkt.getPayload());
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
