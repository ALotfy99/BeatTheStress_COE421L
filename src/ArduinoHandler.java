// Arduino Handler is just a fancy SerialPortHandle, but with thread ability

import jssc.SerialPortException;

public class ArduinoHandler extends SerialPortHandle implements Runnable {
	//attributes
	int arduinoID;
	HeartRateInterface hri;
	PressureSensorInterface psi;
	ButtonInterface btn;
	static final int START_BYTE  = 0xA5;   // [AI] distinct start/handshake byte
	static final int VIBRATION_OK = 0x88;
	// constructor
	public ArduinoHandler(
			String path, 
			int arduinoID, 
			HeartRateInterface hri,
			PressureSensorInterface psi,
			ButtonInterface btn
			) {
		super(path);
		this.arduinoID = arduinoID;
		this.hri = hri;
		this.psi = psi;
		this.btn = btn;
	}
	// decode a byte and call the necessary GameController function
	// packet format
	// [ARD1][ARD2][BTN1][BTN0] [PS1][PS0][PS][HR]
	void decodeByte(byte b) throws SerialPortException {
		boolean isCurrentArduino = ((b >>> 6) & 0x03) == (arduinoID);
		boolean isHeartRate = (b & 0x01) == 1;
		boolean isPressure = ((b >>> 1) & 0x01) == 1;
		int pressureIndex = (b >>> 2) & 0x02;
		int buttonIndex = (b >>> 4) & 0x02; //default is 11 meaning NOP;
		
		if (!isCurrentArduino) return; // no business for this arduino
		System.out.println("Arduino" + arduinoID + "sent something!");
		if (arduinoID == 1 && isHeartRate) {
			hri.onHeartRate((double) readByte());
		}
		if (arduinoID == 2 && isPressure) {
			// Pressure index comes from the header
			psi.onPressureSensor(pressureIndex);
		}
		if (arduinoID == 2 && (buttonIndex != 0x03)) {
			// Button index comes from the header
			btn.decodeButtonOP(buttonIndex);
		}
	}
	
	// start function
	@Override
	public void run() {
		try {
			boolean start = (readByte() == START_BYTE); // signal start to the arduino
			
			while (true) { 		// wait for start signal
				//forever run
				if (start) { // if you start, read a control byte and then decode the control
					byte control = readByte();
					decodeByte(control);
				}
			}
		} catch (SerialPortException e) {
			e.printStackTrace();
		}	
	}

}
