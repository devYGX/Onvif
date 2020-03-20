#include "base.h"
#include <android/native_window_jni.h>
#include "OnvifPlayer.h"

#define CLASS_NAME "org/android/ffmpeg/OnvifPlayer"


jlong nativeCreate(JNIEnv *env, jobject obj) {
    JavaVM *jvm;
    env->GetJavaVM(&jvm);
    OnvifPlayer *player = new OnvifPlayer(jvm);
    return reinterpret_cast<jlong>(player);
}

void nativeSetFrameCallback(JNIEnv *env, jobject obj, jlong ptr, jobject callback) {
    if (ptr) {
        OnvifPlayer *player = reinterpret_cast<OnvifPlayer *>(ptr);
        jobject global_cb = env->NewGlobalRef(callback);
        player->setFrameCallback(env, global_cb);
    }
}

jint nativeSetPath(JNIEnv *env, jobject obj, jlong ptr, jstring path) {
    if (ptr) {
        OnvifPlayer *player = reinterpret_cast<OnvifPlayer *>(ptr);
        const char *str_path = env->GetStringUTFChars(path, NULL);
        int ret = player->setPath(str_path);
        env->ReleaseStringUTFChars(path, str_path);
        return ret;
    } else {
        return NULL_POINTER;
    }
}

jint nativePrepare(JNIEnv *env, jobject obj, jlong ptr) {
    if (ptr) {
        OnvifPlayer *player = reinterpret_cast<OnvifPlayer *>(ptr);
        return player->prepare(env);
    } else {
        return NULL_POINTER;
    }

}

void nativeRelease(JNIEnv *env, jobject obj, jlong ptr) {
    if (ptr) {
        OnvifPlayer *player = reinterpret_cast<OnvifPlayer *>(ptr);
        player->release(env);
        delete player;
    }
}

void nativeReset(JNIEnv *env, jobject obj, jlong ptr) {
    if (ptr) {
        OnvifPlayer *player = reinterpret_cast<OnvifPlayer *>(ptr);
        player->reset();
    }
}

void nativeSetSurface(JNIEnv *env, jobject obj, jlong ptr, jobject surface) {
    if (ptr) {
        OnvifPlayer *player = reinterpret_cast<OnvifPlayer *>(ptr);
        if (surface == NULL) {
            player->setWindow(NULL);
        } else {
            ANativeWindow *renderWindow = ANativeWindow_fromSurface(env, surface);
            if (renderWindow == NULL) {
                LOGD("nativeSetSurface renderWindow is null ");
            }
            player->setWindow(renderWindow);
        }
    }
}

jint nativeStart(JNIEnv *env, jobject obj, jlong ptr) {
    if (ptr) {
        OnvifPlayer *player = reinterpret_cast<OnvifPlayer *>(ptr);
        return player->start();
    } else {
        return NULL_POINTER;
    }
}

jint nativeStop(JNIEnv *env, jobject obj, jlong ptr) {
    if (ptr) {
        OnvifPlayer *player = reinterpret_cast<OnvifPlayer *>(ptr);
        return player->stop();
    } else {
        return NULL_POINTER;
    }

}

void nativeSetObserver(JNIEnv *env, jobject obj, jlong ptr, jobject observer) {
    if (ptr) {
        OnvifPlayer *player = reinterpret_cast<OnvifPlayer *>(ptr);
        jobject gObj = env->NewGlobalRef(obj);
        jobject gObserver = env->NewGlobalRef(observer);
        player->setObserver(env, gObj, gObserver);
        env->DeleteLocalRef(obj);
        env->DeleteLocalRef(observer);
    }
}

static JNINativeMethod method[] = {
        {"nativeCreate",           "()J",                                          (void *) nativeCreate},
        {"nativeSetPath",          "(JLjava/lang/String;)I",                       (void *) nativeSetPath},
        {"nativeSetFrameCallback", "(JLorg/android/ffmpeg/OnvifStreamCallback;)V", (void *) nativeSetFrameCallback},
        {"nativePrepare",          "(J)I",                                         (void *) nativePrepare},
        {"nativeRelease",          "(J)V",                                         (void *) nativeRelease},
        {"nativeReset",            "(J)V",                                         (void *) nativeReset},
        {"nativeSetSurface",       "(JLandroid/view/Surface;)V",                   (void *) nativeSetSurface},
        {"nativeStart",            "(J)I",                                         (void *) nativeStart},
        {"nativeStop",             "(J)I",                                         (void *) nativeStop},
        {"nativeSetObserver",      "(JLorg/android/ffmpeg/OnvifPlayerObserver;)V", (void *) nativeSetObserver},
};

int register_jni(JNIEnv *env) {

    jclass cls = env->FindClass(CLASS_NAME);

    if (cls == NULL) {
        LOGE("FindClass fail %s", CLASS_NAME);
        return JNI_ERR;
    }

    int ret = -1;
    if ((ret = env->RegisterNatives(cls, method, ARRAY_LENGTH(method))) < 0) {
        LOGE("RegisterNatives fail %d", ret);
        return JNI_ERR;
    }
    return JNI_OK;
}
