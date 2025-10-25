import jssc.SerialPort;
import jssc.SerialPortException;
// Class declaration
class SerialPortHandle {
	SerialPort sp; //define serial port
	String path; //define serial port path
	public SerialPortHandle(String path) {
		super(); //initialize parent constructor
		this.sp = new SerialPort(path); //initialize a serial port with some path as attribute
		this.path = path; //intialize path attribute
		try {
			sp.openPort(); //open port
			//int baudRate, int dataBits, int stopBits, int parity
			sp.setParams(9600, 8, 1, 0); //set baud rate to 9600, 8 data bits (1 byte), 1 stop bit and no parity bits
			// Flush garbage data on initial open
			while (sp.getInputBufferBytesCount() > 0) {
				sp.readBytes(); // read the bytes of this port
			}
		} catch (SerialPortException e) {
			// TODO Auto-generated catch block
			e.printStackTrace(); // if the process of opening a port fails -> make exception and print stacktrace
		} // Open serial port
	}
	public String readLine() { // readLine method
		StringBuilder string = new StringBuilder(); // create a string using a StringBuilder
		while (true) { //forever loop
			try { //try
				// create a byte array consisting of a single byte read from the port
				byte[] buffer = sp.readBytes(1);
				// if nothing in the buffer or it's null -> skip the loop iteration
				if (buffer == null || buffer.length == 0)
					continue;
				char c = (char) buffer[0]; // otherwise, get the first byte of the buffer
				if (c == '\n' || c == '\r') { //if the byte is \n or \r means it's a End of Line
					if (string.length() == 0) { 
						// if my string (from string builder) is empty
						continue; // skip empty lines, you don't create strings that are '\n' or '\r'
					} else { //otherwise
						//otherwise that's just a return character, so DO NOT include it in the string
						break; // return non-empty line, break out of the while loop
					}
				}
				string.append(c); // append the byte to the string
			} catch (SerialPortException e) {
				e.printStackTrace(); //print stack trace if you get any error inside the try clause
				break;
			}
		}
		return string.toString().trim(); // convert the string builder to a string and trim
		//trim removes trailing spaces or starting spaces
	}
	public void printLine(String s) {
		byte byteArray[] = s.getBytes(); //get the bytes
		try {
			sp.writeBytes(byteArray); //write the bytes of the bytearray from the serial port
			sp.writeByte((byte) '\n'); // //write an end-line casted as a byte
		} catch (SerialPortException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace(); // do exception if anything wrong goes in the try
		}
	}
	public void writeByte(byte b) throws SerialPortException {
		sp.writeBytes(new byte[] { b }); // write a single byte to the port
	}
	public byte readByte() throws SerialPortException { // read byte
		byte[] buffer = sp.readBytes(1); // blocking read
		if (buffer != null && buffer.length > 0) { //if buffer is not null or there is value in the buffer
			return buffer[0]; // return the first byte of the buffer
		}
		else { //throw an exception if anything goes wrong
			throw new SerialPortException(path, "readByte", "No data received");
		}
	}
}
