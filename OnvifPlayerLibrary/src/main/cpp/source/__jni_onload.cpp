#include "base.h"

extern int register_jni(JNIEnv *env);

int JNI_OnLoad(JavaVM *vm, void *reversed) {
    JNIEnv *env;
    if (vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) != JNI_OK) {
        return JNI_ERR;
    }
    int ret = register_jni(env);
    if(ret != 0){
        LOGE("JNI_OnLoad register jni err: %d", ret);
    }
    return JNI_VERSION_1_6;
}

void JNI_OnUnload(JavaVM *vm, void *reversed) {}

