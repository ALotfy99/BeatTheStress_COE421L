public interface BeatSubject {
    void registerObserver(BeatObserver observer);
    void removeObserver(BeatObserver observer);
    void notifyBeatObservers(int laneIndex);
    void notifyHitResult(int laneIndex, String judgment);
    void notifySequenceEnd();
    void notifyBeatmapChanged(String msg);
    void notifyBeatmapIndexChanged(int beatmapIndex);
}
