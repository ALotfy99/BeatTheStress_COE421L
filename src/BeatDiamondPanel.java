import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.EnumMap;

/**
 * Beat Diamond Panel implementing Observer pattern
 */
public class BeatDiamondPanel extends JPanel implements BeatObserver {

    public enum Lane { TOP, LEFT, RIGHT, BOTTOM }

    private static final int FLASH_MS = 350;

    private String title = "Beat the Stress";
    private int score = 0;
    private String difficultyText = "Easy";

    private Lane requiredLane = null;

    private Lane lastHitLane = null;
    private Color lastHitColor = null;
    private long lastHitUntil = 0;
    private boolean sequenceEnded = false;

    // NEW: beatmap-changed overlay
    private String overlayMsg = null;
    private long overlayUntil = 0;
    private static final int OVERLAY_MS = 1500;

    // NEW: active beatmap index shown in GUI
    private int activeBeatmapIndex = 0;

    private final Timer repaintTimer;
    private final BeatSubject subject;

    public BeatDiamondPanel(BeatSubject subject) {
        this(subject, "Beat The Stress");
    }

    public BeatDiamondPanel(BeatSubject subject, String title) {
        this.subject = subject;
        this.title = title;

        subject.registerObserver(this);

        setPreferredSize(new Dimension(420, 520));
        setBackground(Color.BLACK);

        repaintTimer = new Timer(16, (ActionEvent e) -> repaint());
        repaintTimer.start();
    }

    public void setTitle(String title) {
        this.title = title;
        repaint();
    }
    
    public void setDifficulty(DifficultyStrategy difficulty) {
        if (difficulty != null) {
            int level = difficulty.getLevel();
            switch (level) {
                case 1:
                    difficultyText = "Easy";
                    break;
                case 2:
                    difficultyText = "Medium";
                    break;
                case 3:
                    difficultyText = "Hard";
                    break;
                default:
                    difficultyText = difficulty.getDescription();
            }
        }
        repaint();
    }

    public int getScore() { return score; }

    public void setScore(int score) { this.score = score; }

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
        lastHitColor = good ? new Color(0x2ECC71) : new Color(0xE74C3C);
        lastHitUntil = System.currentTimeMillis() + FLASH_MS;

        if (good) {
            score++;
        }
        
        // Clear the yellow highlight after any hit (correct or wrong)
        requiredLane = null;

        repaint();
    }

    @Override
    public void onSequenceEnd() {
        sequenceEnded = true;
        repaint();
    }

    @Override
    public void onBeatmapChanged(String msg) {
        overlayMsg = msg;
        overlayUntil = System.currentTimeMillis() + OVERLAY_MS;
        sequenceEnded = false;
        repaint();
    }

    @Override
    public void onBeatmapIndexChanged(int beatmapIndex) {
        this.activeBeatmapIndex = beatmapIndex;
        repaint();
    }

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

        // ===== TITLE =====
        g2.setFont(getFont().deriveFont(Font.BOLD, 36f));
        g2.setColor(new Color(0xF1C40F));
        FontMetrics titleFm = g2.getFontMetrics();
        int titleWidth = titleFm.stringWidth(title);
        g2.drawString(title, (w - titleWidth) / 2, 50);

        // ===== DIFFICULTY =====
        g2.setFont(getFont().deriveFont(Font.BOLD, 18f));
        g2.setColor(new Color(0xFFD700)); // Gold color for difficulty
        String diffText = "Difficulty: " + difficultyText;
        FontMetrics diffFm = g2.getFontMetrics();
        int diffWidth = diffFm.stringWidth(diffText);
        g2.drawString(diffText, (w - diffWidth) / 2, 75);
        
        // ===== BEATMAP INDEX (NEW) =====
        g2.setFont(getFont().deriveFont(Font.BOLD, 16f));
        g2.setColor(new Color(0xDDDDDD));
        String mapText = "Beatmap: " + activeBeatmapIndex;
        FontMetrics mapFm = g2.getFontMetrics();
        int mapWidth = mapFm.stringWidth(mapText);
        g2.drawString(mapText, (w - mapWidth) / 2, 95);

        // ===== SCORE =====
        g2.setFont(getFont().deriveFont(Font.BOLD, 24f));
        g2.setColor(new Color(0x2ECC71));
        String scoreText = "Score: " + score;
        FontMetrics scoreFm = g2.getFontMetrics();
        int scoreWidth = scoreFm.stringWidth(scoreText);
        g2.drawString(scoreText, (w - scoreWidth) / 2, 120);

        // ===== DIAMOND PADS =====
        int cy = h / 2 + 30;

        int boxW = (int)(w * 0.22);
        int boxH = (int)(h * 0.22);
        int gap  = (int)(w * 0.18);

        EnumMap<Lane, Rectangle> rects = new EnumMap<>(Lane.class);
        rects.put(Lane.TOP,    new Rectangle(cx - boxW/2, cy - gap - boxH, boxW, boxH));
        rects.put(Lane.LEFT,   new Rectangle(cx - gap - boxW, cy - boxH/2, boxW, boxH));
        rects.put(Lane.RIGHT,  new Rectangle(cx + gap, cy - boxH/2, boxW, boxH));
        rects.put(Lane.BOTTOM, new Rectangle(cx - boxW/2, cy + gap, boxW, boxH));

        long now = System.currentTimeMillis();
        boolean flashActive = lastHitLane != null && now <= lastHitUntil;

        for (Lane lane : Lane.values()) {
            Rectangle r = rects.get(lane);

            Color fill = new Color(0x222222);

            if (lane == requiredLane) fill = new Color(0xF1C40F);

            if (flashActive && lane == lastHitLane) fill = lastHitColor;

            g2.setColor(fill);
            g2.fillRoundRect(r.x, r.y, r.width, r.height, 26, 26);

            g2.setStroke(new BasicStroke(4f));
            g2.setColor(new Color(0xDDDDDD));
            g2.drawRoundRect(r.x, r.y, r.width, r.height, 26, 26);

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

        g2.setFont(getFont().deriveFont(Font.PLAIN, 14f));
        g2.setColor(new Color(0xAAAAAA));
        g2.drawString("Required = Yellow | GOOD = Green | WRONG = Red", 10, h - 10);

        // ===== Beatmap changed overlay =====
        if (overlayMsg != null && now <= overlayUntil) {
            g2.setColor(new Color(0, 0, 0, 170));
            g2.fillRect(0, 0, w, h);

            g2.setColor(Color.WHITE);
            g2.setFont(getFont().deriveFont(Font.BOLD, 26f));
            int mw = g2.getFontMetrics().stringWidth(overlayMsg);
            g2.drawString(overlayMsg, (w - mw) / 2, h / 2);
        }

        // ===== sequence ended overlay =====
        if (sequenceEnded) {
            g2.setColor(new Color(0, 0, 0, 150));
            g2.fillRect(0, 0, w, h);

            g2.setColor(Color.WHITE);
            g2.setFont(getFont().deriveFont(Font.BOLD, 32f));
            String msg = "Sequence Ended";
            int msgWidth = g2.getFontMetrics().stringWidth(msg);
            g2.drawString(msg, (w - msgWidth) / 2, h / 2);

            g2.setFont(getFont().deriveFont(Font.BOLD, 24f));
            String finalScore = "Final Score: " + score;
            int fsWidth = g2.getFontMetrics().stringWidth(finalScore);
            g2.drawString(finalScore, (w - fsWidth) / 2, h / 2 + 40);
        }

        g2.dispose();
    }
}
