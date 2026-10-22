#include "engine.hpp"
#include <android/native_window_jni.h>
#include <new>
extern "C" {
#include <libavcodec/jni.h>
}
using paw::Engine;
static Engine* engine(jlong value) { return reinterpret_cast<Engine*>(value); }
extern "C" JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void*) {
    av_jni_set_java_vm(vm, nullptr); return JNI_VERSION_1_6;
}
extern "C" JNIEXPORT jlong JNICALL Java_com_solthariel_pawchan_ffmpeg_PawNative_create(JNIEnv*, jclass) {
    return reinterpret_cast<jlong>(new(std::nothrow) Engine());
}
extern "C" JNIEXPORT jboolean JNICALL Java_com_solthariel_pawchan_ffmpeg_PawNative_open(
        JNIEnv* env, jclass, jlong handle, jobject reader, jobject audio, jboolean software) {
    try { return engine(handle)->open(env, reader, audio, software); }
    catch (...) { return false; }
}
extern "C" JNIEXPORT jobjectArray JNICALL Java_com_solthariel_pawchan_ffmpeg_PawNative_metadata(
        JNIEnv* env, jclass, jlong handle) {
    auto values = engine(handle)->metadata();
    jclass strings = env->FindClass("java/lang/String");
    if (!strings) return nullptr;
    jmethodID constructor = env->GetMethodID(strings, "<init>", "([BLjava/lang/String;)V");
    if (!constructor) { env->DeleteLocalRef(strings); return nullptr; }
    jobjectArray array = env->NewObjectArray(static_cast<jsize>(values.size()), strings, nullptr);
    jstring charset = env->NewStringUTF("UTF-8");
    if (!array || !charset) { env->DeleteLocalRef(strings); return nullptr; }
    for (size_t i = 0; i < values.size(); ++i) {
        const auto& text = values[i];
        jbyteArray bytes = env->NewByteArray(static_cast<jsize>(text.size()));
        if (!bytes) break;
        env->SetByteArrayRegion(bytes, 0, static_cast<jsize>(text.size()), reinterpret_cast<const jbyte*>(text.data()));
        jobject value = env->NewObject(strings, constructor, bytes, charset);
        env->DeleteLocalRef(bytes);
        if (env->ExceptionCheck() || !value) break;
        env->SetObjectArrayElement(array, static_cast<jsize>(i), value); env->DeleteLocalRef(value);
    }
    env->DeleteLocalRef(charset); env->DeleteLocalRef(strings);
    return array;
}
extern "C" JNIEXPORT jint JNICALL Java_com_solthariel_pawchan_ffmpeg_PawNative_pump(JNIEnv* env, jclass, jlong handle) {
    try { return engine(handle)->pump(env); } catch (...) { return -1; }
}
extern "C" JNIEXPORT void JNICALL Java_com_solthariel_pawchan_ffmpeg_PawNative_command(
        JNIEnv*, jclass, jlong handle, jint type, jlong value) { engine(handle)->command(type, value); }
extern "C" JNIEXPORT jlongArray JNICALL Java_com_solthariel_pawchan_ffmpeg_PawNative_status(
        JNIEnv* env, jclass, jlong handle) {
    int64_t status[6]; engine(handle)->status(status);
    jlongArray output = env->NewLongArray(6);
    if (output) env->SetLongArrayRegion(output, 0, 6, reinterpret_cast<const jlong*>(status));
    return output;
}
extern "C" JNIEXPORT void JNICALL Java_com_solthariel_pawchan_ffmpeg_PawNative_surface(
        JNIEnv* env, jclass, jlong handle, jobject surface) {
    engine(handle)->surface(surface ? ANativeWindow_fromSurface(env, surface) : nullptr);
}
extern "C" JNIEXPORT void JNICALL Java_com_solthariel_pawchan_ffmpeg_PawNative_destroy(
        JNIEnv* env, jclass, jlong handle) { engine(handle)->release(env); delete engine(handle); }
