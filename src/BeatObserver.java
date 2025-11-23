public interface BeatObserver {
    void onBeatActivated(int laneIndex);
    void onHitResult(int laneIndex, String judgment);
    void onSequenceEnd();
    void onBeatmapChanged(String msg);
    void onBeatmapIndexChanged(int beatmapIndex);
}
