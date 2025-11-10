import java.util.Scanner;

public class Driver {
	public static void main(String[] args) {
        String[] playlist = { 
        		"music/MOZART.wav",
        		"music/MARIO.wav", 
        		"music/ZELDA.wav", 
        		"music/MCR_HOUSE_OF_WOLVES.wav",
        		"music/THISISHOWIDISAPPEAR.wav",
        		"music/KOTON.wav"};  // Must be WAV
        MusicController m1 = new MusicController(new RealtimeTempoPlayer(playlist), playlist);
        Scanner scan = new Scanner(System.in);
        while(true) {
        	float num = scan.nextFloat();
        	if (num == -1)
        		m1.nextSong();
        	else if (num == -2)
        		m1.previousSong();
        	else {
        		m1.setTempo(num);
        	}
        }
        //any observer should have
        private Subject subject;
    	public ConcreteObserver(Subject subject)
    	{
    	this.subject = subject;
    		subject.registerObserver(this);
    	}
        
	}
}
