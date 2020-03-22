package org.android.ffmpeg;

import android.util.Log;
import android.view.Surface;

public class OnvifPlayer {
    private static final String TAG = "libonvif";

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

    public synchronized int setPath(String path) {
        return nativeSetPath(mPtr, path);
    }

    public synchronized void setFrameCallback(OnvifStreamCallback streamCallback) {
        nativeSetFrameCallback(mPtr, streamCallback);
    }

    public synchronized void reset() {
        nativeReset(mPtr);
    }

    public synchronized void setSurface(Surface surface) {
        nativeSetSurface(mPtr, surface);
    }

    public synchronized int prepare() {
        return nativePrepare(mPtr);
    }

    public synchronized int start() {
        return nativeStart(mPtr);
    }

    public synchronized int stop() {
        return nativeStop(mPtr);
    }

    public synchronized void release() {
        nativeRelease(mPtr);
        mPtr = 0;
    }

    public synchronized void setPlayerObserver(OnvifPlayerObserver observer) {
        nativeSetObserver(mPtr, observer);
    }
}
