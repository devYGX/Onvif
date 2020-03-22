#include "OnvifPlayer.h"
#include <stdlib.h>

extern "C" {
#include "libavcodec/avcodec.h"
#include "libavformat/avformat.h"
#include "libavutil/imgutils.h"
#include "libswscale/swscale.h"
}

OnvifPlayer::OnvifPlayer(JavaVM *jvm) :
        jvm(jvm) {
    pthread_mutex_init(&lock, NULL);
    pthread_mutex_init(&frame_callback_lock, NULL);
    pthread_mutex_init(&window_lock, NULL);
    pthread_cond_init(&loopStop, NULL);
    pthread_mutex_init(&observer_lock, NULL);

    avcodec_register_all();
    av_register_all();

    avformat_network_init();
}

OnvifPlayer::~OnvifPlayer() {
    pthread_mutex_destroy(&lock);
    pthread_mutex_destroy(&frame_callback_lock);
    pthread_mutex_destroy(&window_lock);
    pthread_cond_destroy(&loopStop);
    pthread_mutex_destroy(&observer_lock);
    LOGD("~OnvifPlayer");

}

int OnvifPlayer::setPath(const char *path) {
    int ret = SUCCESS;
    pthread_mutex_lock(&lock);
    if (state != STATE_DEFAULT) {
        ret = STATE_NOT_DEFAULT;
        goto __END;
    }

    if (path == NULL) {
        if (this->path != NULL) {
            free(this->path);
            this->path = NULL;
        }
    } else {
        if (this->path != NULL) {
            if (strcmp(this->path, path) == 0) {
                goto __END;
            }
            free(this->path);
        }
        this->path = (char *) malloc(strlen(path) + 1);

        memset(this->path, 0, strlen(path) + 1);
        memcpy(this->path, path, strlen(path));
    }

    __END:
    pthread_mutex_unlock(&lock);
    return ret;
}

int OnvifPlayer::prepare(JNIEnv *env) {
    int ret = SUCCESS;

    int cState = this->state;

    // 1. 声明临时变量
    AVFormatContext *av_fc = NULL;
    int ret_avformat_open_input = -1;
    AVCodecContext *avCodecContext = NULL;
    AVCodecParameters *avCodecParameters = NULL;
    AVCodec *codec_decoder = NULL;
    AVPacket *avPacket = NULL;
    AVFrame *frame = NULL;
    AVFrame *renderFrame = NULL;
    AVFrame *callbackFrame = NULL;
    int size = 0;
    int callback_size = 0;
    uint8_t *buffer = NULL;
    uint8_t *callback_buffer = NULL;
    struct SwsContext *swsContext = NULL;
    struct SwsContext *nv21_swsContext = NULL;
    int video_stream_index = -1;
    AVPixelFormat dstFormat = AV_PIX_FMT_RGBA;
    AVPixelFormat callback_dstFormat = AV_PIX_FMT_NV21;
    const char *video_path;
    AVDictionary *options = NULL;

    pthread_mutex_lock(&lock);

    if (!inited) {
        avcodec_register_all();
        avformat_network_init();
        inited = true;
    }

    if (state != STATE_DEFAULT) {
        ret = STATE_NOT_DEFAULT;
        goto __END;
    }

    if (!this->path) {
        ret = PATH_IS_NULL;
        goto __END;
    }

    video_path = this->path;
    av_fc = avformat_alloc_context();
    if (av_fc == NULL) {
        ret = AVFC_ALLOC_FAIL;
        goto __FAIL;
    }
    // 3. 打开文件流
    av_dict_set(&options, "rtsp_transport", "tcp", 0);
    ret_avformat_open_input = avformat_open_input(&av_fc, video_path, NULL, &options);
    if (ret_avformat_open_input != 0) {
        LOGE("open input file failed!");
        ret = OPEN_PATH_FAIL;
        goto __FAIL;
    }

    LOGD("avformat_open_input success %d %s\nav_fc: %d", ret_avformat_open_input, video_path,
         av_fc);


    // 4. 检索多媒体流的信息, 其结果将会被赋值在av_fc中
    ret = avformat_find_stream_info(av_fc, NULL);
    LOGD("ret %d avformat_find_stream_info 1: ", ret);
    if (ret < 0) {
        LOGE("avformat_find_stream_info failed: %d", ret);
        ret = FIND_VIDEO_STREAM_FAILD;
        goto __FAIL;
    }

    LOGD("avformat_find_stream_info success");
    // 5. 找到视频流的索引
    for (int i = 0; i < av_fc->nb_streams; i++) {
        if (av_fc->streams[i]->codecpar->codec_type == AVMEDIA_TYPE_VIDEO) {
            video_stream_index = i;
            break;
        }
    }
    LOGD("video_stream_index success %d", video_stream_index);
    if (video_stream_index == -1) {
        LOGE("no video stream found!");
        ret = FIND_VIDEO_STREAM_FAILD;
        goto __FAIL;
    }


    // 6.根据视频的编码ID找到编码器
    avCodecParameters = av_fc->streams[video_stream_index]->codecpar;
    codec_decoder = avcodec_find_decoder(avCodecParameters->codec_id);
    if (codec_decoder == NULL) {
        LOGE("codec decoder not found");
        ret = DECODER_NOT_FOUND;
        goto __FAIL;
    }

    LOGD("avcodec_find_decoder success %d", video_stream_index);
    // 7.创建一个编解码器上下文
    avCodecContext = avcodec_alloc_context3(codec_decoder);
    if (avCodecContext == NULL) {
        LOGE("alloc avcodec context fail");
        goto __FAIL;
    }
    LOGD("avcodec_alloc_context3 success %d", video_stream_index);

    ret = avcodec_parameters_to_context(avCodecContext, avCodecParameters);
    if (ret < 0) {
        LOGE("fill avcode context by media avcodec parameters failed");
        goto __FAIL;
    }
    LOGD("avcodec_parameters_to_context success %d", video_stream_index);

    // 8. 打开解码器
    ret = avcodec_open2(avCodecContext, codec_decoder, NULL);
    if (ret != 0) {
        LOGE("open avcodec failed %d", ret);
        goto __FAIL;
    }
    LOGD("avcodec_open2 success %d", video_stream_index);



    // 9 分配内存空间windowBuffer
    avPacket = av_packet_alloc(); // 用于保存从多媒体中读取的数据包
    frame = av_frame_alloc();      // 用于保存从编码器读出来的数据帧
    renderFrame = av_frame_alloc();// 用户保存转换成RGBA后的数据帧
    callbackFrame = av_frame_alloc();

    size = av_image_get_buffer_size(dstFormat, avCodecContext->width, avCodecContext->height,
                                    1);
    buffer = (uint8_t *) malloc(size * sizeof(uint8_t *));
    av_image_fill_arrays(renderFrame->data, renderFrame->linesize, buffer, dstFormat,
                         avCodecContext->width, avCodecContext->height, 1);

    callback_size = av_image_get_buffer_size(callback_dstFormat, avCodecContext->width,
                                             avCodecContext->height,
                                             1);
    callback_buffer = (uint8_t *) malloc(callback_size * sizeof(uint8_t *));
    av_image_fill_arrays(callbackFrame->data, callbackFrame->linesize, callback_buffer,
                         callback_dstFormat, avCodecContext->width, avCodecContext->height, 1);

    // 10.创建swsContext, 用于格式转换
    // avCodecContext 默认是 AV_PIX_FMT_YUV420P; 这里的YUV420P是 I420格式的, YYYY UU VV

    swsContext = sws_getContext(
            avCodecContext->width, avCodecContext->height, avCodecContext->pix_fmt,
            avCodecContext->width, avCodecContext->height, dstFormat,
            SWS_BILINEAR,
            NULL, NULL, NULL);

    nv21_swsContext = sws_getContext(
            avCodecContext->width, avCodecContext->height, avCodecContext->pix_fmt,
            avCodecContext->width, avCodecContext->height, callback_dstFormat,
            SWS_BILINEAR,
            NULL, NULL, NULL);

    if (this->renderWindow) {
        ANativeWindow_setBuffersGeometry(this->renderWindow,
                                         avCodecContext->width,
                                         avCodecContext->height,
                                         WINDOW_FORMAT_RGBA_8888);
    }

    if (swsContext == NULL || nv21_swsContext == NULL) {
        LOGE("sws_getContext failed");
        goto __FAIL;
    }


    this->avFormatContext = av_fc;
    this->avCodecContext = avCodecContext;
    this->avCodecParameters = avCodecParameters;
    this->codec_decoder = codec_decoder;
    this->avPacket = avPacket;
    this->frame = frame;
    this->renderFrame = renderFrame;
    this->callbackFrame = callbackFrame;
    this->buffer = buffer;
    this->callback_buffer = callback_buffer;
    this->swsContext = swsContext;
    this->nv21_swsContext = swsContext;
    this->video_stream_index = video_stream_index;
    this->nv21_frame_size = callback_size;
    this->codec_decoder = codec_decoder;

    this->state = STATE_PREPARE;
    ret = SUCCESS;

    goto __END;

    __FAIL:

    if (ret_avformat_open_input == 0) { avformat_close_input(&av_fc); }
    avformat_free_context(av_fc);
    if (avCodecContext != NULL) { avcodec_free_context(&avCodecContext); }
    if (avCodecContext != NULL) { avcodec_close(avCodecContext); }
    if (avPacket != NULL) { av_packet_free(&avPacket); }
    if (frame != NULL) { av_frame_free(&frame); }
    if (renderFrame != NULL) { av_frame_free(&renderFrame); }
    if (swsContext != NULL) { sws_freeContext(swsContext); }
    if (buffer != NULL) { free(buffer); }
    if (nv21_swsContext != NULL) { sws_freeContext(nv21_swsContext); }
    if (callback_buffer != NULL) { free(callback_buffer); }
    if (avCodecParameters) { avcodec_parameters_free(&avCodecParameters); }
    avformat_network_deinit();

    __END:
    pthread_mutex_unlock(&lock);
    if (cState != this->state && this->state == STATE_PREPARE) {
        pthread_mutex_lock(&observer_lock);
        if (this->playerObserver
            && this->jplayer
            && this->onPrepare) {
            env->CallVoidMethod(this->playerObserver, this->onPrepare, this->jplayer,
                                avCodecContext->width, avCodecContext->height);
        }
        pthread_mutex_unlock(&observer_lock);
    }
    return ret;
}

void OnvifPlayer::doLoop() {

    JNIEnv *env;
    int ret = this->jvm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6);

    if (ret != JNI_OK) {
        ret = jvm->AttachCurrentThread(&env, NULL);
        if (ret < 0) {
            env = NULL;
            return;
        }
    }

    ANativeWindow_Buffer windowBuffer;
    int read_ret = 0;
    // av_read_frame: 0 if OK, < 0 on error or end of file
    while (true) {
        pthread_mutex_lock(&lock);
        if (this->state == STATE_PLAYING &&
            (read_ret = av_read_frame(this->avFormatContext, this->avPacket)) == 0) {

            // 如果读取到的数据包为视频
            if (avPacket->stream_index == video_stream_index) {
                // 发送给解码器
                int sendState = avcodec_send_packet(avCodecContext, avPacket);
                if (sendState == 0) {
                    int receiveState = avcodec_receive_frame(avCodecContext, frame);
                    if (receiveState == 0) {
                        sws_scale(swsContext,
                                  (uint8_t const *const *) frame->data,
                                  frame->linesize,
                                  0,
                                  avCodecContext->height, renderFrame->data, renderFrame->linesize);


                        pthread_mutex_lock(&frame_callback_lock);
                        if (nv21_frameCallback) {
                            sws_scale(nv21_swsContext,
                                      (uint8_t const *const *) frame->data,
                                      frame->linesize,
                                      0,
                                      avCodecContext->height, callbackFrame->data,
                                      callbackFrame->linesize);
                            jbyteArray array = env->NewByteArray(this->nv21_frame_size);

                            //HERE I GET THE ERROR, I HAVE BEEN TRYING WITH len/2 and WORKS , PROBABLY SOME BYTS ARE GETTING LOST.
                            env->SetByteArrayRegion(array, 0, this->nv21_frame_size,
                                                    (jbyte *) (callbackFrame->data[0]));

                            // LOGD("after av_read_frame: %d", sizeof(callbackFrame->data[0]));
                            env->CallVoidMethod(nv21_frameCallback, this->onFrameMethodId, array,
                                                avCodecContext->width, avCodecContext->height);

                            env->DeleteLocalRef(array);
                        }
                        pthread_mutex_unlock(&frame_callback_lock);
                        pthread_mutex_lock(&window_lock);
                        if (this->renderWindow) {
                            ANativeWindow_lock(this->renderWindow, &windowBuffer, NULL);
                            uint8_t *dst = (uint8_t *) windowBuffer.bits;
                            uint8_t *src = renderFrame->data[0];

                            int dst_stride = windowBuffer.stride * 4;
                            int src_stride = renderFrame->linesize[0];

                            for (int i = 0; i < avCodecContext->height; i++) {
                                memcpy(dst + i * dst_stride, src + i * src_stride, src_stride);
                            }
                            ANativeWindow_unlockAndPost(this->renderWindow);
                        }
                        pthread_mutex_unlock(&window_lock);
                    } else {
                        LOGW("avcodec receive frame failed %d", receiveState);
                    }
                } else {
                    LOGW("avcodec send packet failed %d", sendState);
                }
            }
            av_packet_unref(avPacket);
            pthread_mutex_unlock(&lock);
        } else {
            LOGD("READ ret: %d %d ", ret, read_ret);
            if (read_ret < 0) {
                this->state = STATE_PREPARE;
            }
            pthread_cond_signal(&loopStop);
            pthread_mutex_unlock(&lock);
            break;
        }
    }
    if (read_ret < 0) {
        pthread_mutex_lock(&observer_lock);
        if (this->playerObserver) {

            if (read_ret == -110 && this->onPlayDisconnected) {
                // disconnected
                env->CallVoidMethod(this->playerObserver, this->onPlayDisconnected);
            } else if (this->onPlayFinish) {
                env->CallVoidMethod(this->playerObserver, this->onPlayFinish);
            }

        }
        pthread_mutex_unlock(&observer_lock);
    }
    LOGD("prepare detach cur thread");
    jvm->DetachCurrentThread();
    pthread_detach(pthread_self());
    readFrameThread = NULL;
    LOGD("end detach cur thread");
}

void *read_frame(void *args) {
    OnvifPlayer *player = reinterpret_cast<OnvifPlayer *>(args);
    // 12. 开始读取视频和渲染
    player->doLoop();
    return NULL;
}

int OnvifPlayer::start() {

    int ret = SUCCESS;
    pthread_t tid;
    int err;
    LOGD("begin start");
    pthread_mutex_lock(&lock);
    if (this->state == STATE_PLAYING) {
        goto __END;
    }

    if (this->state != STATE_PREPARE) {
        ret = STATE_NOT_PREPARE;
        goto __END;
    }

    err = pthread_create(&tid, NULL, read_frame, this);

    if (err) {
        ret = CREATE_READ_THREAD_FAIL;
        goto __END;
    }
    readFrameThread = tid;
    this->state = STATE_PLAYING;
    __END:
    pthread_mutex_unlock(&lock);
    LOGD("end start");
    return ret;
}

int OnvifPlayer::stop() {


    pthread_mutex_lock(&lock);

    if (this->state == STATE_PREPARE) {
        goto __END;
    }

    if (this->state == STATE_PLAYING) {
        this->state = STATE_PREPARE;
        pthread_cond_wait(&loopStop, &lock);
        goto __END;
    }

    __END:
    pthread_mutex_unlock(&lock);
    return SUCCESS;
}

void OnvifPlayer::reset() {

    pthread_t tid;
    pthread_mutex_lock(&lock);
    if (this->state == STATE_DEFAULT) {
        goto __END;
    }
    if (this->state == STATE_PLAYING) {
        this->state = STATE_DEFAULT;
        pthread_cond_wait(&loopStop, &lock);
    }
    tid = readFrameThread;
    LOGD("reset tid: %d", tid);
    if (tid) {
        pthread_join(tid, NULL);
    }
    LOGD("after join %d %d", readFrameThread, tid);

    if (this->avCodecContext) {
        avcodec_free_context(&this->avCodecContext);
        avcodec_close(this->avCodecContext);
        this->avCodecContext = NULL;
    }
    if (this->avFormatContext) {
        avformat_close_input(&this->avFormatContext);
        avformat_free_context(this->avFormatContext);
        this->avFormatContext = NULL;
    }
    if (this->avPacket) {
        av_packet_free(&this->avPacket);
        this->avPacket = NULL;
    }
    if (this->frame) {
        av_frame_free(&this->frame);
        this->frame = NULL;
    }
    if (this->renderFrame) {
        av_frame_free(&this->renderFrame);
        this->renderFrame = NULL;
    }
    if (this->callbackFrame) {
        av_frame_free(&this->callbackFrame);
        this->callbackFrame = NULL;
    }
    if (this->swsContext) {
        sws_freeContext(this->swsContext);
        this->swsContext = NULL;
    }
    if (this->nv21_swsContext) {
        sws_freeContext(this->nv21_swsContext);
        this->nv21_swsContext = NULL;
    }
    if (this->buffer) {
        free(this->buffer);
        this->buffer = NULL;
    }
    if (this->callback_buffer) {
        free(this->callback_buffer);
        this->callback_buffer = NULL;
    }
    if (this->avCodecParameters) {
        this->avCodecParameters = NULL;
    }
    if (this->codec_decoder) { this->codec_decoder = NULL; }
    this->nv21_frame_size = 0;
    this->video_stream_index = 0;


    __END:
    pthread_mutex_unlock(&lock);
}

void OnvifPlayer::release(JNIEnv *env) {
    this->reset();

    pthread_mutex_lock(&lock);
    if (inited) { avformat_network_deinit(); }
    LOGD("release state: %d", this->state);
    if (this->path) {
        free(this->path);
        this->path = NULL;
    }
    if (this->renderWindow) {
        ANativeWindow_release(this->renderWindow);
        this->renderWindow = NULL;
    }

    if (this->streamCallbackClass) {
        env->DeleteGlobalRef(this->streamCallbackClass);
        this->streamCallbackClass = NULL;
    }
    if (this->onFrameMethodId) {
        this->onFrameMethodId = NULL;
    }
    if (this->onPlayFinish) { this->onPlayFinish = NULL; }
    if (this->onPlayDisconnected) { this->onPlayDisconnected = NULL; }
    if (this->onPrepare) { this->onPrepare = NULL; }
    if (this->jplayer) {
        env->DeleteGlobalRef(this->jplayer);
        this->jplayer = NULL;
    }
    if (this->playerObserver) {
        env->DeleteGlobalRef(this->playerObserver);
        this->playerObserver = NULL;
    }
    if (this->observerClass) {
        env->DeleteGlobalRef(this->observerClass);
        this->observerClass = NULL;
    }

    pthread_mutex_unlock(&lock);
    LOGD("release finish");
}

int OnvifPlayer::setFrameCallback(JNIEnv *env, jobject frameCallback) {

    pthread_mutex_lock(&frame_callback_lock);
    if (this->nv21_frameCallback != frameCallback) {
        if (this->nv21_frameCallback != NULL) {
            env->DeleteGlobalRef(this->nv21_frameCallback);
        }
        this->nv21_frameCallback = frameCallback;
    }

    if (streamCallbackClass == NULL) {
        jclass cb_class = env->FindClass(STREAM_CALLBACK_CLASS_NAME);
        jmethodID onFrame_methodId = env->GetMethodID(cb_class, "onFrame", "([BII)V");

        streamCallbackClass = reinterpret_cast<jclass >(env->NewGlobalRef(cb_class));
        onFrameMethodId = onFrame_methodId;
        env->DeleteLocalRef(cb_class);
    }
    pthread_mutex_unlock(&frame_callback_lock);
    return SUCCESS;
}

void OnvifPlayer::setWindow(ANativeWindow *window) {
    pthread_mutex_lock(&window_lock);
    LOGD("setWindow start");

    if (this->renderWindow != window) {
        if (this->renderWindow != NULL) {
            ANativeWindow_release(this->renderWindow);
        }
        this->renderWindow = window;
        if (this->avCodecContext && this->renderWindow) {
            ANativeWindow_setBuffersGeometry(this->renderWindow,
                                             avCodecContext->width,
                                             avCodecContext->height,
                                             WINDOW_FORMAT_RGBA_8888);
        }
    }

    LOGD("setWindow end");
    pthread_mutex_unlock(&window_lock);
}

void OnvifPlayer::setObserver(JNIEnv *env, jobject player, jobject jobserver) {

    pthread_mutex_lock(&observer_lock);

    if (!this->observerClass) {
        jclass ljclass = env->FindClass(OBSERVER_CLASS_NAME);
        if (ljclass) {
            this->observerClass = reinterpret_cast<jclass >(env->NewGlobalRef(ljclass));
        } else {
            LOGE("class  %s not found!", OBSERVER_CLASS_NAME);
        }
    }

    if (!this->jplayer) {
        this->jplayer = player;
    }

    if (!this->playerObserver) {
        this->playerObserver = jobserver;
    } else if (this->playerObserver != jobserver) {
        env->DeleteGlobalRef(this->playerObserver);
        this->playerObserver = jobserver;
    }

    if (!this->onPrepare) {
        this->onPrepare = env->GetMethodID(this->observerClass, "onPrepared",
                                           "(Lorg/android/ffmpeg/OnvifPlayer;II)V");
    }
    if (!this->onPlayDisconnected) {
        this->onPlayDisconnected = env->GetMethodID(this->observerClass, "onPlayDisconnect", "()V");
    }

    if (!this->onPlayFinish) {
        this->onPlayFinish = env->GetMethodID(this->observerClass, "onPlayFinish", "()V");
    }

    pthread_mutex_unlock(&observer_lock);

}
