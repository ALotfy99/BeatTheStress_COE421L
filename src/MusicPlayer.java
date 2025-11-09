public interface MusicPlayer {
    void play(int songIndex);    // start given song
    void pause();                // pause playback
    void resume();               // resume playback
    void stop();                 // stop playback
    void setTempo(double factor);// 1.0 = normal speed, 1.5 = 50% faster
}
