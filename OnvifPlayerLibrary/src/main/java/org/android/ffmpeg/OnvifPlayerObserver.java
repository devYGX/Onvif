package org.android.ffmpeg;

public interface OnvifPlayerObserver {
    void onPrepared(OnvifPlayer player, int width, int height);

    void onPlayDisconnect();

    void onPlayFinish();
}
