public class HardDifficulty implements DifficultyStrategy {
    @Override
    public int getMusicTempo() {
        return 3; // Fastest music tempo
    }
    
    @Override
    public double getBeatTempo() {
        return 2; // 1 second inter-beat delay (hardest)
    }
    
    @Override
    public String getDescription() {
        return "Hard (music_tempo=3, beat_tempo=0.5s)";
    }
    
    @Override
    public int getLevel() {
        return 3;
    }
}