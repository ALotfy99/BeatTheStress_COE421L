interface BeatObserver {
    void onBeatActivated(int laneIndex);
    void onHitResult(int laneIndex, String judgment);
    void onSequenceEnd();
}
