cmake_minimum_required(VERSION 3.4.1)

# 指定项目的c/cpp文件路径
# AUX_SOURCE_DIRECTORY(${CMAKE_SOURCE_DIR}/src/main/cpp/source CPP_SOURCE)

find_library(log-lib log)
find_library(android-lib android)

aux_source_directory(src/main/cpp/source CPP_SOURCE)
# 添加要编译的动态库
add_library(onvif_player SHARED ${CPP_SOURCE})

# 设置jni libs的路径; CMAKE_SOURCE_DIR是cmake默认的宏, 表示CMakeLists.txt的路径
set(JNI_LIBS_DIR ${CMAKE_SOURCE_DIR}/src/main/jniLibs)

# 添加库, 库名avcodec, SHARED表示这是动态库, IMPORTED表示这是从外导入的库
add_library(avcodec SHARED IMPORTED)
set_target_properties(avcodec PROPERTIES IMPORTED_LOCATION ${JNI_LIBS_DIR}/${ANDROID_ABI}/libavcodec.so)

add_library(avdevice SHARED IMPORTED)
set_target_properties(avdevice PROPERTIES IMPORTED_LOCATION ${JNI_LIBS_DIR}/${ANDROID_ABI}/libavdevice.so)

add_library(avfilter SHARED IMPORTED)
set_target_properties(avfilter PROPERTIES IMPORTED_LOCATION ${JNI_LIBS_DIR}/${ANDROID_ABI}/libavfilter.so)

add_library(avformat SHARED IMPORTED)
set_target_properties(avformat PROPERTIES IMPORTED_LOCATION ${JNI_LIBS_DIR}/${ANDROID_ABI}/libavformat.so)

add_library(avutil SHARED IMPORTED)
set_target_properties(avutil PROPERTIES IMPORTED_LOCATION ${JNI_LIBS_DIR}/${ANDROID_ABI}/libavutil.so)

add_library(swresample SHARED IMPORTED)
set_target_properties(swresample PROPERTIES IMPORTED_LOCATION ${JNI_LIBS_DIR}/${ANDROID_ABI}/libswresample.so)

add_library(swscale SHARED IMPORTED)
set_target_properties(swscale PROPERTIES IMPORTED_LOCATION ${JNI_LIBS_DIR}/${ANDROID_ABI}/libswscale.so)

# 指定ffmpeg 头文件目录
include_directories( ${CMAKE_SOURCE_DIR}/src/main/cpp/include ${JNI_LIBS_DIR}/includes)

# 链接库
target_link_libraries(onvif_player
        avcodec avdevice avfilter avformat avutil swresample swscale
        ${log-lib} ${android-lib})