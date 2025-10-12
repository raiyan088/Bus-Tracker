#ifndef BUBT_BUS_TRACKER__H
#define BUBT_BUS_TRACKER__H

#include <jni.h>
#include <string>
#include <sstream>
#include <random>
#include <iomanip>
#include <locale>
#include <android/log.h>

#include "path.h"
#include "unzip.h"
#include "data.h"
#include "pkg.h"
#include "md5.h"

#define LOG_TAG "BusTrackerLog"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN,  LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)


std::string bytesToString(JNIEnv* env, jbyteArray array);

jstring encryption(JNIEnv *env, jstring data);

jbyteArray getSignatureArray(JNIEnv *env);

#endif
