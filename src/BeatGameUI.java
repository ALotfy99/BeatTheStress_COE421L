import javax.swing.*;

public class BeatGameUI extends JFrame {

    private final BeatDiamondPanel panel;

    /**
     * Constructor using observer pattern
     * The panel automatically registers itself with the controller
     */
    public BeatGameUI(BeatController controller) {
        this(controller, "Beat your Stress");
    }

    /**
     * Constructor with custom title
     */
    public BeatGameUI(BeatController controller, String title) {
        super("Beat The Stress - Diamond HUD");
        
        // Panel registers itself as observer with the controller
        panel = new BeatDiamondPanel(controller, title);
        
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setContentPane(panel);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    /**
     * Get the panel (if you need to access score, etc.)
     */
    public BeatDiamondPanel getPanel() {
        return panel;
    }
}
