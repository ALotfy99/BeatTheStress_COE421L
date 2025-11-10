// Game Controller
/*
 * VERY IMPORTANT:
 * Arduino 1 ONLY sends HR data and nothing else
 * Arduino 2 ONLY sends Pressure Sensor Data, Button Presses
 * PC sends (Beat control = LEDs) values and the Vibration OK!
 * */
import jssc.SerialPortException;

public class GameController 
implements 
HeartRateInterface, 
PressureSensorInterface,
ButtonInterface
{
	String playerName;
	int[] courseNumbers;
	int difficulty;
	int score;
	String port1;
	String port2;
	ArduinoHandler arduino1Handler;
	ArduinoHandler arduino2Handler;
	private Thread arduino1HandlerThread;
	private Thread arduino2HandlerThread;
	private int currentSong;

	//Thread musicThread;
	//MusicController music;
	//Thread heartThread;
	float heartRatePrevious;
	//BeatControls beats;
	
	/* 
	 * 
	 *  
	 *  
	 *  */
	public GameController(String playerName, int[] courseNumbers, int difficulty,
			String port1, String port2) {
		super();
		this.playerName = playerName;
		this.courseNumbers = courseNumbers;
		this.difficulty = difficulty;
		this.score = 0;
		this.arduino1Handler = new ArduinoHandler(port1, 0x01, this, this, this);
		this.arduino2Handler = new ArduinoHandler(port2, 0x02, this, this, this);
		this.arduino1HandlerThread = new Thread(arduino1Handler, "Arduino1 Listen");
		this.arduino2HandlerThread = new Thread(arduino2Handler, "Arduino2 Listen");
		this.currentSong = 0;
	}
	
	void initializeGame() {
		// launch all threads
		arduino1HandlerThread.start(); // this starts the threads
		arduino2HandlerThread.start(); // this starts the threads
	}
	void startGame() throws SerialPortException {
		// starting the game means sending start bytes
		arduino1Handler.writeByte((byte) ArduinoHandler.START_BYTE);
		arduino2Handler.writeByte((byte) ArduinoHandler.START_BYTE);
	}
	void setDifficulty(int bpm) {
		// this is a functionality by itself
		this.difficulty = courseNumbers[currentSong] + bpm;
	}
	// based on beat controls
	void setLEDs() {
		
	}
	
	void sendVibrationOK() throws SerialPortException {
		arduino1Handler.writeByte((byte) ArduinoHandler.VIBRATION_OK);
	}
	void updateScore() {
		// based on beat
		this.score += 1;
	}
	

	@Override
	public void onHeartRate(double bpm) {
		setDifficulty((int) bpm);
	}

	@Override
	public void onPressureSensor(int index) {
		// MUST compare with beat controls
	}

	@Override
	public void decodeButtonOP(int buttonIndex) {
		// control Music based on button index
		if (buttonIndex == 0) {
			// toggle Music
		}
		if (buttonIndex == 1) {
			//Skip Forward
			if (currentSong == courseNumbers.length-1) return; // you cannot go more than the number of courses
		}
		if (buttonIndex == 2) {
			//Go backwards
			if (currentSong == 0) return; //you cannot go below the number of courses
		}
		
	}
}
