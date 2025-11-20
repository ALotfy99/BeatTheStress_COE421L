import java.util.List;
import java.util.Scanner;

import jssc.SerialPortException;

public class Driver {
	public static void main(String[] args) throws SerialPortException {
        String[] playlist = { 
        		"music/THISISHOWIDISAPPEAR.wav",
        		"music/MCR_HOUSE_OF_WOLVES.wav",
        		"music/KOTON.wav",
        		"music/MOZART.wav",
        		"music/MARIO.wav", 
        		"music/ZELDA.wav", 
        		};  // Must be WAV
        String arduino2Port = "";
        String arduino1Port = "";
        String arduino3Port = "/dev/cu.usbserial-A10LIDLA";



        ArduinoHandler ar2 = new ArduinoHandler(arduino3Port, 2);
        BeatControls beats = new BeatControls(0);       // your pattern
        BeatController controller = new BeatController(beats.getBeats(), ar2);

        // Go into interval mode with 9 seconds spacing
        controller.enableIntervalMode(true);
        controller.setIntervalSec(9.0);

        
	}
}
