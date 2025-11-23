public class EasyDifficulty implements DifficultyStrategy {
    @Override
    public int getMusicTempo() {
        return -2; // Slowest music tempo
    }
    
    @Override
    public double getBeatTempo() {
        return 3; // 2 seconds inter-beat delay
    }
    
    @Override
    public String getDescription() {
        return "Easy (music_tempo=-2, beat_tempo=3.0s)";
    }
    
    @Override
    public int getLevel() {
        return 1;
    }
}