public class ArduinoPacket {
	
	protected byte packet_data;
	
	public ArduinoPacket(byte packet_data) {
		super();
		this.packet_data = packet_data;
	}
	int getArduinoID() {
		return (int) ((packet_data >> 6) & 0x03); // returns ONLY the arduino ID
	}
	int getPayload() {
		return (int) (packet_data & 0x3F); //returns full payload data
	}
	


}
