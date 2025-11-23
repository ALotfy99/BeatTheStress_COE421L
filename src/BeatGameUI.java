import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

/**
 * BeatGameUI - GUI for the rhythm game with Diamond Layout.
 * Observes BeatJudge for beat events and GameLevelManager for level changes.
 */
public class BeatGameUI extends JFrame implements BeatObserver, GameLevelManager.LevelChangeObserver {
    
    private final BeatDiamondPanel panel;
    private final BeatJudge beatJudge;
    private final GameLevelManager levelManager;
    
    /**
     * Constructor - sets up GUI and registers as observer
     */
    public BeatGameUI(BeatJudge beatJudge, GameLevelManager levelManager) {
        this(beatJudge, levelManager, "Beat the Stress");
    }
    
    /**
     * Constructor with custom title
     */
    public BeatGameUI(BeatJudge beatJudge, GameLevelManager levelManager, String title) {
        super("Beat The Stress - Diamond HUD");
        
        if (beatJudge == null || levelManager == null) {
            throw new IllegalArgumentException("BeatJudge and GameLevelManager cannot be null");
        }
        
        this.beatJudge = beatJudge;
        this.levelManager = levelManager;
        
        // Register as observer
        beatJudge.registerObserver(this);
        levelManager.registerLevelObserver(this);
        
        // Create panel
        panel = new BeatDiamondPanel(beatJudge, title);
        
        // Update with current level
        onLevelChanged(levelManager.getCurrentLevelIndex(), levelManager.getCurrentLevelName());
        
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setContentPane(panel);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
        
        System.out.println("[BeatGameUI] GUI initialized");
    }
    
    /**
     * Get the panel (if you need to access score, etc.)
     */
    public BeatDiamondPanel getPanel() {
        return panel;
    }
    
    // ===== BeatObserver Implementation =====
    
    @Override
    public void onBeatActivated(int laneIndex) {
        // Panel handles this
    }
    
    @Override
    public void onHitResult(int laneIndex, String judgment) {
        // Panel handles this
    }
    
    @Override
    public void onSequenceEnd() {
        // Panel handles this
    }
    
    @Override
    public void onBeatmapChanged(String msg) {
        // Panel handles this
    }
    
    @Override
    public void onBeatmapIndexChanged(int beatmapIndex) {
        // Panel handles this
    }
    
    // ===== LevelChangeObserver Implementation =====
    
    @Override
    public void onLevelChanged(int levelIndex, String levelName) {
        panel.setTitle(levelName);
        System.out.println("[BeatGameUI] Level changed to: " + levelName);
    }
    
    @Override
    public void onPauseStateChanged(boolean isPaused) {
        System.out.println("[BeatGameUI] Pause state: " + (isPaused ? "Paused" : "Resumed"));
    }
    
    @Override
    public void onSequenceEnd() {
        System.out.println("[BeatGameUI] Sequence ended");
    }
    
    /**
     * Called when difficulty changes
     */
    public void onDifficultyChanged(DifficultyStrategy difficulty) {
        panel.setDifficulty(difficulty);
        System.out.println("[BeatGameUI] Difficulty changed to: " + difficulty.getDescription());
    }
}
