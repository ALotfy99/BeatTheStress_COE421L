public class DifficultyFactory {
    private static final DifficultyStrategy EASY = new EasyDifficulty();
    private static final DifficultyStrategy MEDIUM = new MediumDifficulty();
    private static final DifficultyStrategy HARD = new HardDifficulty();
    
    /**
     * Get a difficulty strategy for the given level
     * @param level The difficulty level (1=Easy, 2=Medium, 3=Hard)
     * @return The corresponding DifficultyStrategy, or Medium if level is invalid
     */
    public static DifficultyStrategy getDifficulty(int level) {
        switch (level) {
            case 1:
                return EASY;
            case 2:
                return MEDIUM;
            case 3:
                return HARD;
            default:
                System.out.println("[DifficultyFactory] Unknown level " + level + ", defaulting to Medium");
                return MEDIUM;
        }
    }
}

