#include "bus-tracker.h"

std::string bytesToString(JNIEnv* env, jbyteArray array) {
    jsize length = env->GetArrayLength(array);
    jbyte* bytes = env->GetByteArrayElements(array, nullptr);

    std::ostringstream oss;
    for (int i = 0; i < length; i++) {
        oss << std::uppercase << std::setfill('0') << std::setw(2)<< std::hex << (0xFF & bytes[i]);
    }

    env->ReleaseByteArrayElements(array, bytes, JNI_ABORT);
    return oss.str();
}

jstring encryption(JNIEnv *env, jstring data) {
    jclass cls;
    jobject obj;
    jobject secretKey;
    jobject ivParame;
    jmethodID jmethodId;
    jbyteArray ivArray;
    jbyteArray keyArray;
    jbyteArray dataArray;

    keyArray = env->NewByteArray(24);
    env->SetByteArrayRegion(keyArray, 0, 24, key);

    ivArray = env->NewByteArray(16);
    env->SetByteArrayRegion(ivArray, 0, 16, iv);

    cls = env->FindClass("javax/crypto/spec/SecretKeySpec");
    jmethodId = env->GetMethodID( cls,"<init>","([BLjava/lang/String;)V");
    secretKey = env->NewObject(cls, jmethodId, keyArray, env->NewStringUTF("AES"));

    cls = env->FindClass("javax/crypto/spec/IvParameterSpec");
    jmethodId = env->GetMethodID( cls,"<init>","([B)V");
    ivParame = env->NewObject(cls, jmethodId, ivArray);

    cls = env->FindClass("java/lang/String");
    jmethodId = env->GetMethodID(cls, "getBytes", "()[B");
    dataArray = (jbyteArray) env->CallObjectMethod(data, jmethodId);

    cls = env->FindClass("javax/crypto/Cipher");
    jmethodId = env->GetStaticMethodID( cls,"getInstance","(Ljava/lang/String;)Ljavax/crypto/Cipher;");
    obj = env->CallStaticObjectMethod(cls, jmethodId, env->NewStringUTF("AES/CBC/PKCS5Padding"));
    jmethodId = env->GetMethodID( cls,"init","(ILjava/security/Key;Ljava/security/spec/AlgorithmParameterSpec;)V");
    env->CallVoidMethod(obj, jmethodId, 0x1, secretKey, ivParame);

    jmethodId = env->GetMethodID( cls,"doFinal","([B)[B");
    dataArray = (jbyteArray) env->CallObjectMethod(obj, jmethodId, dataArray);

    cls = env->FindClass("android/util/Base64");
    jmethodId = env->GetStaticMethodID( cls,"encodeToString","([BI)Ljava/lang/String;");

    auto base64Encoded = (jstring) env->CallStaticObjectMethod(cls, jmethodId, dataArray, 0x0);

    jclass stringClass = env->FindClass("java/lang/String");
    jmethodID replaceMethod = env->GetMethodID(stringClass, "replace", "(Ljava/lang/CharSequence;Ljava/lang/CharSequence;)Ljava/lang/String;");

    return (jstring) env->CallObjectMethod(base64Encoded, replaceMethod, env->NewStringUTF("\n"), env->NewStringUTF(""));
}

jstring decryption(JNIEnv *env, jstring data) {
    jclass cls;
    jobject obj;
    jobject secretKey;
    jobject ivParame;
    jmethodID jmethodId;
    jbyteArray ivArray;
    jbyteArray keyArray;
    jbyteArray dataArray;

    keyArray = env->NewByteArray(24);
    env->SetByteArrayRegion(keyArray, 0, 24, key);

    ivArray = env->NewByteArray(16);
    env->SetByteArrayRegion(ivArray, 0, 16, iv);

    cls = env->FindClass("javax/crypto/spec/SecretKeySpec");
    jmethodId = env->GetMethodID( cls,"<init>","([BLjava/lang/String;)V");
    secretKey = env->NewObject(cls, jmethodId, keyArray, env->NewStringUTF("AES"));

    cls = env->FindClass("javax/crypto/spec/IvParameterSpec");
    jmethodId = env->GetMethodID( cls,"<init>","([B)V");
    ivParame = env->NewObject(cls, jmethodId, ivArray);

    cls = env->FindClass("java/lang/String");
    jmethodId = env->GetMethodID(cls, "getBytes", "()[B");
    dataArray = (jbyteArray) env->CallObjectMethod(data, jmethodId);

    cls = env->FindClass("android/util/Base64");
    jmethodId = env->GetStaticMethodID( cls,"decode","([BI)[B");
    dataArray = (jbyteArray) env->CallStaticObjectMethod(cls, jmethodId, dataArray, 0x0);

    cls = env->FindClass("javax/crypto/Cipher");
    jmethodId = env->GetStaticMethodID( cls,"getInstance","(Ljava/lang/String;)Ljavax/crypto/Cipher;");
    obj = env->CallStaticObjectMethod(cls, jmethodId, env->NewStringUTF("AES/CBC/PKCS5Padding"));
    jmethodId = env->GetMethodID( cls,"init","(ILjava/security/Key;Ljava/security/spec/AlgorithmParameterSpec;)V");
    env->CallVoidMethod(obj, jmethodId, 0x2, secretKey, ivParame);

    jmethodId = env->GetMethodID( cls,"doFinal","([B)[B");
    dataArray = (jbyteArray) env->CallObjectMethod(obj, jmethodId, dataArray);

    cls = env->FindClass("java/lang/String");
    jmethodId = env->GetMethodID(cls, "<init>", "([BLjava/lang/String;)V");

    return (jstring) env->NewObject(cls, jmethodId, dataArray, env->NewStringUTF("UTF-8"));
}


std::string generateToken(JNIEnv* env) {
    jbyteArray sigArray = getSignatureArray(env);
    if (!sigArray) return "";

    jsize len = env->GetArrayLength(sigArray);
    jbyte* sigBytes = env->GetByteArrayElements(sigArray, nullptr);

    std::ostringstream sigHex;
    for (int i = 0; i < len; i++) {
        sigHex << std::uppercase << std::hex << ((sigBytes[i] & 0xFF) >> 4);
        sigHex << std::uppercase << std::hex << (sigBytes[i] & 0x0F);
    }

    env->ReleaseByteArrayElements(sigArray, sigBytes, JNI_ABORT);

    auto now = std::chrono::system_clock::now();
    auto ms = std::chrono::duration_cast<std::chrono::milliseconds>(now.time_since_epoch()).count();

    std::random_device rd;
    std::mt19937 gen(rd());
    std::uniform_int_distribution<uint32_t> dis(0, 0xFFFFFFFF);
    uint32_t randVal = dis(gen);

    std::ostringstream tokenStream;
    tokenStream << randVal << "." << sigHex.str() << "." << ms;
    std::string tokenStr = tokenStream.str();

    jstring jToken = encryption(env, env->NewStringUTF(tokenStr.c_str()));

    const char* encToken = env->GetStringUTFChars(jToken, nullptr);
    std::string result(encToken);
    env->ReleaseStringUTFChars(jToken, encToken);

    return result;
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_rr_bubtbustracker_App_encryption(JNIEnv *env, __attribute__((unused)) jclass clazz, jstring data) {
    return encryption(env, data);
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_rr_bubtbustracker_App_decryption(JNIEnv *env, __attribute__((unused)) jclass clazz, jstring data) {
    return decryption(env, data);
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_rr_bubtbustracker_App_getToken(JNIEnv *env, __attribute__((unused)) jclass clazz) {
    std::string token = generateToken(env);
    return env->NewStringUTF(token.c_str());;
}

JNIEXPORT jbyteArray getSignatureArray(JNIEnv *env) {
    char *path = pathHelperGetPath();
    if (!path) {
        return nullptr;
    }
    size_t len_in = 0;
    size_t len_out = 0;

    unsigned char *content = unzipGetCertificateDetails(path, &len_in);
    if (!content) {
        free(path);
        return nullptr;
    }

    unsigned char *res = pkgGetSignature(content, len_in, &len_out);

    jbyteArray jbArray = nullptr;
    if (res && len_out > 0) {
        unsigned char md5_result[16];
        md5(res, len_out, md5_result);

        jbArray = env->NewByteArray(16);
        if (jbArray) {
            env->SetByteArrayRegion(jbArray, 0, 16, (jbyte*)md5_result);
        }
    }

    free(content);
    free(path);
    pkgFree();

    return jbArray;
}

jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved) {
    JNIEnv * env;

    if (vm->GetEnv((void **) &env, JNI_VERSION_1_6) != JNI_OK) {
        return -1;
    }

    jbyteArray dataArray = getSignatureArray(env);

    if (nullptr == dataArray) {
        return -1;
    }

    jsize len = env->GetArrayLength(dataArray);
    if (len != sizeof(sign)) {
        return -1;
    }

    jbyte* dataBytes = env->GetByteArrayElements(dataArray, nullptr);

    bool match = std::memcmp(dataBytes, sign, len) == 0;

    env->ReleaseByteArrayElements(dataArray, dataBytes, JNI_ABORT);

    if (!match) {
        return -1;
    }

    return JNI_VERSION_1_6;
}