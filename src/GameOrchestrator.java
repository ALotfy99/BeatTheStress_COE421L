import javax.swing.*;
import java.util.Scanner;
import jssc.SerialPortException;

/**
 * GameOrchestrator - Entry point that orchestrates the game setup.
 * Prompts user for name and difficulty, then instantiates all components.
 */
public class GameOrchestrator {
    
    private final String playerName;
    private DifficultyStrategy difficultyStrategy;
    private final String[] playlist;
    
    // Components
    private TempoSubject tempoSubject;
    private GameplaySubject gameplaySubject;
    private SystemControlSubject systemControlSubject;
    private MusicController musicController;
    private BeatJudge beatJudge;
    private GameLevelManager levelManager;
    private BeatGameUI gameUI;
    
    /**
     * Constructor - prompts for player name and difficulty
     */
    public GameOrchestrator(String[] playlist) {
        this.playlist = playlist;
        
        // Prompt for player name
        this.playerName = promptPlayerName();
        
        // Prompt for difficulty
        this.difficultyStrategy = promptDifficulty();
        
        System.out.println("[GameOrchestrator] Player: " + playerName);
        System.out.println("[GameOrchestrator] Difficulty: " + difficultyStrategy.getDescription());
    }
    
    /**
     * Initialize all game components (without Arduino - for emulation)
     */
    public void initializeEmulated() {
        System.out.println("[GameOrchestrator] Initializing emulated mode...");
        
        // Create emulated subjects (will be connected to keyboard in EmulatedDriver)
        // For now, create null handlers - EmulatedDriver will create proper ones
        tempoSubject = null;
        gameplaySubject = null;
        systemControlSubject = null;
        
        // Create MusicPlayer
        MusicPlayer player = new RealtimeTempoPlayer(playlist);
        
        // Create subjects will be done by EmulatedDriver
        // For now, we'll create a method that accepts subjects
    }
    
    /**
     * Initialize all game components with real Arduino handlers
     */
    public void initializeFunctional(ArduinoHandler ar1, ArduinoHandler ar2, ArduinoHandler ar3) 
            throws SerialPortException {
        System.out.println("[GameOrchestrator] Initializing functional mode...");
        
        // Create Subject wrappers (ArduinoHandler implements Subject)
        tempoSubject = new TempoSubject(ar1);
        gameplaySubject = new GameplaySubject(ar2);
        systemControlSubject = new SystemControlSubject(ar3);
        
        // Register for difficulty changes from AR1
        tempoSubject.setDifficultyObserver(level -> setDifficulty(level));
        
        // Create MusicPlayer
        MusicPlayer player = new RealtimeTempoPlayer(playlist);
        
        // Create MusicController with difficulty strategy (no longer observes TempoSubject)
        musicController = new MusicController(player, playlist, null, difficultyStrategy);
        
        // Create BeatJudge with inter-beat delay from strategy
        beatJudge = new BeatJudge(gameplaySubject, difficultyStrategy.getBeatTempo());
        
        // Create GameLevelManager
        levelManager = new GameLevelManager(systemControlSubject, beatJudge, musicController);
        
        // Link BeatJudge to GameLevelManager for sequence end notification
        beatJudge.setLevelManager(levelManager);
        
        // Create GUI on EDT
        SwingUtilities.invokeLater(() -> {
            gameUI = new BeatGameUI(beatJudge, levelManager);
        });
        
        // Start MusicController
        musicController.start();
        
        System.out.println("[GameOrchestrator] All components initialized!");
    }
    
    /**
     * Initialize with emulated subjects (for testing)
     */
    public void initializeEmulated(TempoSubject tempoSubj, GameplaySubject gameplaySubj, 
                                  SystemControlSubject systemSubj) {
        System.out.println("[GameOrchestrator] Initializing emulated mode with subjects...");
        
        this.tempoSubject = tempoSubj;
        this.gameplaySubject = gameplaySubj;
        this.systemControlSubject = systemSubj;
        
        // Register for difficulty changes from AR1
        tempoSubject.setDifficultyObserver(level -> setDifficulty(level));
        
        // Create MusicPlayer
        MusicPlayer player = new RealtimeTempoPlayer(playlist);
        
        // Create MusicController with difficulty strategy (no longer observes TempoSubject)
        musicController = new MusicController(player, playlist, null, difficultyStrategy);
        
        // Create BeatJudge with inter-beat delay from strategy
        beatJudge = new BeatJudge(gameplaySubject, difficultyStrategy.getBeatTempo());
        
        // Create GameLevelManager
        levelManager = new GameLevelManager(systemControlSubject, beatJudge, musicController);
        
        // Link BeatJudge to GameLevelManager for sequence end notification
        beatJudge.setLevelManager(levelManager);
        
        // Create GUI on EDT
        SwingUtilities.invokeLater(() -> {
            gameUI = new BeatGameUI(beatJudge, levelManager);
            gameUI.onDifficultyChanged(difficultyStrategy); // FORCE SYNC
        });
        
        // Start MusicController
        musicController.start();
        
        System.out.println("[GameOrchestrator] All components initialized (emulated)!");
    }
    
    /**
     * Prompt user for player name
     */
    private String promptPlayerName() {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter your name: ");
        String name = scanner.nextLine().trim();
        if (name.isEmpty()) {
            name = "Player";
        }
        return name;
    }
    
    /**
     * Prompt user for difficulty level
     */
    private DifficultyStrategy promptDifficulty() {
        Scanner scanner = new Scanner(System.in);
        
        while (true) {
            System.out.println("\nSelect difficulty:");
            System.out.println("1. Easy");
            System.out.println("2. Medium");
            System.out.println("3. Hard");
            System.out.print("Enter choice (1-3): ");
            
            String input = scanner.nextLine().trim();
            
            try {
                int choice = Integer.parseInt(input);
                DifficultyStrategy strategy = DifficultyFactory.getDifficulty(choice);
                if (strategy != null) {
                    return strategy;
                }
            } catch (NumberFormatException e) {
                // Invalid input, try again
            }
            
            System.out.println("Invalid choice. Please enter 1, 2, or 3.");
        }
    }
    
    // Getters
    public String getPlayerName() {
        return playerName;
    }
    
    public DifficultyStrategy getDifficultyStrategy() {
        return difficultyStrategy;
    }
    
    public MusicController getMusicController() {
        return musicController;
    }
    
    public BeatJudge getBeatJudge() {
        return beatJudge;
    }
    
    public GameLevelManager getLevelManager() {
        return levelManager;
    }
    
    /**
     * Set difficulty to a specific level (1=Easy, 2=Medium, 3=Hard)
     */
    public synchronized void setDifficulty(int level) {
        if (level < 1 || level > 3) {
            System.err.println("[GameOrchestrator] Invalid difficulty level: " + level + " (must be 1-3)");
            return;
        }
        
        difficultyStrategy = DifficultyFactory.getDifficulty(level);
        
        System.out.println("[GameOrchestrator] Difficulty changed to: " + difficultyStrategy.getDescription());
        
        // Update BeatJudge with new inter-beat delay
        if (beatJudge != null) {
            beatJudge.setInterBeatDelay(difficultyStrategy.getBeatTempo());
        }
        
        // Update MusicController with new tempo
        if (musicController != null) {
            musicController.setTempo(difficultyStrategy.getMusicTempo());
        }
        
        // Notify UI if available
        if (gameUI != null) {
            gameUI.onDifficultyChanged(difficultyStrategy);
        }
    }
    
    /**
     * Cycle to the next difficulty level (Easy -> Medium -> Hard -> Easy)
     */
    public synchronized void cycleDifficulty() {
        int currentLevel = difficultyStrategy.getLevel();
        int nextLevel = (currentLevel % 3) + 1; // Cycle: 1->2, 2->3, 3->1
        setDifficulty(nextLevel);
    }
}

