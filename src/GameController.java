
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
		ButtonInterface {
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

	// Thread musicThread;
	// MusicController music;
	// Thread heartThread;
	float heartRatePrevious;
	// BeatControls beats;

	BeatController beatController;
	BeatControls beatControls;
	private BeatControls.Beat currentBeat;  // track active beat for comparison
	private final double HIT_TOLERANCE = 0.2;  // seconds allowed for “perfect” hit

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
	    arduino1Handler.writeByte((byte) ArduinoHandler.START_BYTE);
	    arduino2Handler.writeByte((byte) ArduinoHandler.START_BYTE);

	    // Load beats and start LED thread
	    beatControls = new BeatControls(currentSong);
	    beatController = new BeatController(
	        beatControls.getBeats(),
	        arduino2Handler,
	        this::onBeatTriggered
	    );
	    beatController.start();

	    System.out.println("[GameController] BeatController thread started for Song #" + currentSong);
	}


	void onBeatTriggered(BeatControls.Beat beat) {
	    this.currentBeat = beat;
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
	    if (currentBeat == null) return;

	    double now = (System.currentTimeMillis() - beatController.getStartTime()) / 1000.0;
	    double delta = Math.abs(now - currentBeat.timestamp);

	    if (index == currentBeat.sensorIndex && delta <= HIT_TOLERANCE) {
	        try {
	            sendVibrationOK(); // send to Arduino1
	            updateScore();
	            System.out.printf("[GameController] Correct hit! Pad %d within %.2fs%n", index, delta);
	        } catch (SerialPortException e) {
	            System.err.println("[GameController] Failed to send VIBRATION_OK: " + e.getMessage());
	        }
	    } else {
	        System.out.printf("[GameController] Missed! Pad %d at delta %.2fs%n", index, delta);
	    }
	}

	@Override
	public void decodeButtonOP(int buttonIndex) {
		// control Music based on button index
		if (buttonIndex == 0) {
			// toggle Music
		}
		if (buttonIndex == 1) {
			// Skip Forward
			if (currentSong == courseNumbers.length - 1)
				return; // you cannot go more than the number of courses
		}
		if (buttonIndex == 2) {
			// Go backwards
			if (currentSong == 0)
				return; // you cannot go below the number of courses
		}

	}
}
