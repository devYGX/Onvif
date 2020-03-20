package org.android.ffmpeg;

import android.view.Surface;

public class OnvifPlayer {

    static {
        System.loadLibrary("onvif_player");
    }

    private long mPtr;

    public OnvifPlayer() {
        this.mPtr = nativeCreate();
    }

    private native long nativeCreate();

    private native int nativeSetPath(long ptr, String path);

    private native void nativeSetFrameCallback(long ptr, OnvifStreamCallback streamCallback);

    private native void nativeSetSurface(long ptr, Surface surface);

    private native int nativePrepare(long ptr);

    private native int nativeStart(long ptr);

    private native int nativeStop(long ptr);

    private native void nativeRelease(long ptr);

    private native void nativeReset(long ptr);

    private native void nativeSetObserver(long ptr, OnvifPlayerObserver observer);

    public int setPath(String path) {
        return nativeSetPath(mPtr, path);
    }

    public void setFrameCallback(OnvifStreamCallback streamCallback) {
        nativeSetFrameCallback(mPtr, streamCallback);
    }

    public void reset() {
        nativeReset(mPtr);
    }

    public void setSurface(Surface surface) {
        nativeSetSurface(mPtr, surface);
    }

    public int prepare() {
        return nativePrepare(mPtr);
    }

    public int start() {
        return nativeStart(mPtr);
    }

    public int stop() {
        return nativeStop(mPtr);
    }

    public void release() {
        nativeRelease(mPtr);
        mPtr = 0;
    }

    public void setPlayerObserver(OnvifPlayerObserver observer) {
        nativeSetObserver(mPtr, observer);
    }
}
