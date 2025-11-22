
public class pressurePacket extends ArduinoPacket {

	public pressurePacket(byte packet_data) {
		super(packet_data);
		// TODO Auto-generated constructor stub
	}

	@Override
	public int getTempo() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getPressureIndex() {
		// TODO Auto-generated method stub
		return (packet_data & 0x03);
	}

	@Override
	public int getButtonOP() {
		// TODO Auto-generated method stub
		return 0;
	}

}
