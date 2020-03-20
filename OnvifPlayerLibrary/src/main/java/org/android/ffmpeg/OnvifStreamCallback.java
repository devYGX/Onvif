package org.android.ffmpeg;

public interface OnvifStreamCallback {

    void onFrame(byte[] buf, int width, int height);
}
