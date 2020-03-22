#include "base.h"
#include <android/native_window_jni.h>
#include <pthread.h>

extern "C" {
#include "libavformat/avformat.h"
}

#define STREAM_CALLBACK_CLASS_NAME "org/android/ffmpeg/OnvifStreamCallback"
#define OBSERVER_CLASS_NAME "org/android/ffmpeg/OnvifPlayerObserver"

#define STATE_DEFAULT 0
#define STATE_PLAYING 1
#define STATE_PREPARE 2

// define return code
#define UNSUPPORT -2
#define NULL_POINTER -1
#define SUCCESS 0
#define STATE_NOT_DEFAULT 1
#define STATE_NOT_STOP 2
#define STATE_NOT_START 3
#define PATH_IS_NULL 4
#define AVFC_ALLOC_FAIL 5
#define OPEN_PATH_FAIL 6
#define FIND_VIDEO_STREAM_FAILD 7
#define DECODER_NOT_FOUND 8
#define STATE_NOT_PREPARE 9
#define CREATE_READ_THREAD_FAIL 10
#define REPEAT_OPERATE 11


class OnvifPlayer {
private:
    // 播放的路径
    char *path = NULL;
    // 渲染的窗口
    ANativeWindow *renderWindow = NULL;
    // nv21 frame callback
    jobject nv21_frameCallback = NULL;
    // 锁
    pthread_mutex_t lock;
    pthread_mutex_t frame_callback_lock;
    pthread_mutex_t window_lock;
    pthread_mutex_t observer_lock;
    // 状态
    int state = 0;

    AVFormatContext *avFormatContext = NULL;
    AVCodecContext *avCodecContext = NULL;
    AVCodecParameters *avCodecParameters = NULL;
    AVCodec *codec_decoder = NULL;
    AVPacket *avPacket = NULL;
    AVFrame *frame = NULL;
    AVFrame *renderFrame = NULL;
    AVFrame *callbackFrame = NULL;

    uint8_t *buffer = NULL;
    uint8_t *callback_buffer = NULL;
    struct SwsContext *swsContext = NULL;
    struct SwsContext *nv21_swsContext = NULL;
    int video_stream_index = 0;
    JavaVM *jvm = NULL;
    int nv21_frame_size = 0;

    jclass streamCallbackClass = NULL;
    jmethodID onFrameMethodId = NULL;

    pthread_cond_t loopStop;

    jobject playerObserver = NULL;
    jobject jplayer = NULL;
    jclass observerClass = NULL;
    jmethodID onPrepare = NULL;
    jmethodID onPlayDisconnected = NULL;
    jmethodID onPlayFinish = NULL;
    pthread_t readFrameThread = NULL;

    bool inited = false;


public:
    OnvifPlayer(JavaVM *jvm);

    ~OnvifPlayer();

    int setPath(const char *path);

    int start();

    int stop();

    void reset();

    void release(JNIEnv *env);

    int prepare(JNIEnv *env);

    int setFrameCallback(JNIEnv *env, jobject frameCallback);

    void setWindow(ANativeWindow *window);

    void doLoop();

    void setObserver(JNIEnv *env, jobject player, jobject jobserver);
};