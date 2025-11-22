public class heartRatePacket extends ArduinoPacket {

	public heartRatePacket(byte packet_data) {
		super(packet_data);
	}
	
	@Override
	public int getTempo() {
		return (int) (packet_data & 0x07); 
	}

	@Override
	public int getPressureIndex() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getButtonOP() {
		// TODO Auto-generated method stub
		return 0;
	}

}
