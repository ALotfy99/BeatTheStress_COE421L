import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.EnumMap;

/**
 * Observer interface for beat game events
 */


/**
 * Subject interface for beat game
 */


/**
 * Beat Diamond Panel implementing Observer pattern
 */
public class BeatDiamondPanel extends JPanel implements BeatObserver {

    public enum Lane { TOP, LEFT, RIGHT, BOTTOM }

    private static final int FLASH_MS = 350;

    // configurable title
    private String title = "Beat your Stress";
    
    // score tracking
    private int score = 0;

    // required beat (yellow)
    private Lane requiredLane = null;

    // last hit feedback (green/red)
    private Lane lastHitLane = null;
    private Color lastHitColor = null;
    private long lastHitUntil = 0;
    private boolean sequenceEnded = false;

    private final Timer repaintTimer;
    private final BeatSubject subject;

    /**
     * Constructor accepting a subject and registering as observer
     */
    public BeatDiamondPanel(BeatSubject subject) {
        this(subject, "Beat your Stress");
    }

    /**
     * Constructor with configurable title
     */
    public BeatDiamondPanel(BeatSubject subject, String title) {
        this.subject = subject;
        this.title = title;
        
        // Register this panel as an observer
        subject.registerObserver(this);

        setPreferredSize(new Dimension(420, 520));
        setBackground(Color.BLACK);

        repaintTimer = new Timer(16, (ActionEvent e) -> repaint());
        repaintTimer.start();
    }

    /**
     * Set the title text
     */
    public void setTitle(String title) {
        this.title = title;
        repaint();
    }

    /**
     * Get current score
     */
    public int getScore() {
        return score;
    }
    
    public void setScore(int score) {
        this.score=score;
    }

    /**
     * Reset score
     */
    public void resetScore() {
        score = 0;
        repaint();
    }

    // ===== Observer Pattern Implementation =====

    @Override
    public void onBeatActivated(int laneIndex) {
        requiredLane = indexToLane(laneIndex);
        repaint();
    }

    @Override
    public void onHitResult(int laneIndex, String judgment) {
        lastHitLane = indexToLane(laneIndex);

        boolean good = "GOOD".equalsIgnoreCase(judgment);
        lastHitColor = good ? new Color(0x2ECC71) : new Color(0xE74C3C); // green / red
        lastHitUntil = System.currentTimeMillis() + FLASH_MS;

        // Update score for successful hits
        if (good) {
            score++;
        }

        repaint();
    }

    @Override
    public void onSequenceEnd() {
        sequenceEnded = true;
        repaint();
    }

    // ===== Helper Methods =====

    private Lane indexToLane(int idx) {
        return switch (idx) {
            case 0 -> Lane.BOTTOM;
            case 1 -> Lane.LEFT;
            case 2 -> Lane.RIGHT;
            case 3 -> Lane.TOP;
            default -> null;
        };
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        var g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();
        int cx = w / 2;

        // ===== TITLE SECTION =====
        g2.setFont(getFont().deriveFont(Font.BOLD, 36f));
        g2.setColor(new Color(0xF1C40F)); // yellow title
        FontMetrics titleFm = g2.getFontMetrics();
        int titleWidth = titleFm.stringWidth(title);
        g2.drawString(title, (w - titleWidth) / 2, 50);

        // ===== SCORE SECTION =====
        g2.setFont(getFont().deriveFont(Font.BOLD, 24f));
        g2.setColor(new Color(0x2ECC71)); // green score
        String scoreText = "Score: " + score;
        FontMetrics scoreFm = g2.getFontMetrics();
        int scoreWidth = scoreFm.stringWidth(scoreText);
        g2.drawString(scoreText, (w - scoreWidth) / 2, 85);

        // ===== DIAMOND PADS SECTION =====
        int cy = h / 2 + 30; // offset down to make room for title/score

        int boxW = (int)(w * 0.22);
        int boxH = (int)(h * 0.22);
        int gap  = (int)(w * 0.18);

        // Positions for diamond
        EnumMap<Lane, Rectangle> rects = new EnumMap<>(Lane.class);
        rects.put(Lane.TOP,    new Rectangle(cx - boxW/2, cy - gap - boxH, boxW, boxH));
        rects.put(Lane.LEFT,   new Rectangle(cx - gap - boxW, cy - boxH/2, boxW, boxH));
        rects.put(Lane.RIGHT,  new Rectangle(cx + gap, cy - boxH/2, boxW, boxH));
        rects.put(Lane.BOTTOM, new Rectangle(cx - boxW/2, cy + gap, boxW, boxH));

        long now = System.currentTimeMillis();
        boolean flashActive = lastHitLane != null && now <= lastHitUntil;

        // Draw each lane box
        for (Lane lane : Lane.values()) {
            Rectangle r = rects.get(lane);

            // base dark gray
            Color fill = new Color(0x222222);

            // required beat highlight
            if (lane == requiredLane) {
                fill = new Color(0xF1C40F); // yellow
            }

            // last hit flash overrides
            if (flashActive && lane == lastHitLane) {
                fill = lastHitColor;
            }

            // fill
            g2.setColor(fill);
            g2.fillRoundRect(r.x, r.y, r.width, r.height, 26, 26);

            // border
            g2.setStroke(new BasicStroke(4f));
            g2.setColor(new Color(0xDDDDDD));
            g2.drawRoundRect(r.x, r.y, r.width, r.height, 26, 26);

            // lane label (1..4)
            g2.setFont(getFont().deriveFont(Font.BOLD, 28f));
            g2.setColor(Color.WHITE);

            String label = switch (lane) {
                case TOP -> "4";
                case LEFT -> "2";
                case RIGHT -> "3";
                case BOTTOM -> "1";
            };

            FontMetrics fm = g2.getFontMetrics();
            int tx = r.x + (r.width - fm.stringWidth(label))/2;
            int ty = r.y + (r.height + fm.getAscent())/2 - 6;
            g2.drawString(label, tx, ty);
        }

        // legend at bottom
        g2.setFont(getFont().deriveFont(Font.PLAIN, 14f));
        g2.setColor(new Color(0xAAAAAA));
        g2.drawString("Required = Yellow | GOOD = Green | WRONG = Red", 10, h - 10);

        // sequence ended overlay
        if (sequenceEnded) {
            g2.setColor(new Color(0, 0, 0, 150));
            g2.fillRect(0, 0, w, h);

            g2.setColor(Color.WHITE);
            g2.setFont(getFont().deriveFont(Font.BOLD, 32f));
            String msg = "Sequence Ended";
            int msgWidth = g2.getFontMetrics().stringWidth(msg);
            g2.drawString(msg, (w - msgWidth) / 2, h / 2);

            // show final score
            g2.setFont(getFont().deriveFont(Font.BOLD, 24f));
            String finalScore = "Final Score: " + score;
            int fsWidth = g2.getFontMetrics().stringWidth(finalScore);
            g2.drawString(finalScore, (w - fsWidth) / 2, h / 2 + 40);
        }

        g2.dispose();
    }
}
