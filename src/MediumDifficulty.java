public class MediumDifficulty implements DifficultyStrategy {
    @Override
    public int getMusicTempo() {
        return 0; // Medium music tempo
    }
    
    @Override
    public double getBeatTempo() {
        return 2.5; // 1 seconds inter-beat delay
    }
    
    @Override
    public String getDescription() {
        return "Medium (music_tempo=0, beat_tempo=2.0s)";
    }
    
    @Override
    public int getLevel() {
        return 2;
    }
}
