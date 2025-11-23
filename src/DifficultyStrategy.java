public interface DifficultyStrategy {
    /**
     * Get the music tempo value (range: -2 to 3)
     * @return music tempo value
     */
    int getMusicTempo();
    
    /**
     * Get the inter-beat delay in seconds (range: 2.0 to 5.0, where 5.0 is easiest)
     * @return inter-beat delay in seconds
     */
    double getBeatTempo();
    
    /**
     * Get a description of this difficulty level
     */
    String getDescription();
    
    /**
     * Get the difficulty level number
     */
    int getLevel();
}