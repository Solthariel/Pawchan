#pragma once
#include <jni.h>
#include <cstdint>
extern "C" {
#include <libavformat/avio.h>
}
namespace paw {
class Input {
    JNIEnv* env_; jobject reader_; jmethodID read_, length_; jbyteArray buffer_;
    int64_t position_ = 0;
    jmethodID timing_ = nullptr;
    bool tracing_ = false;
public:
    AVIOContext* io = nullptr;
    Input(JNIEnv* env, jobject reader);
    ~Input();
    bool tracing() const { return tracing_; }
    void timing(int stage, int64_t micros);
    bool valid() const { return io != nullptr; }
    int64_t position() const { return position_; }
    static int read(void* opaque, uint8_t* data, int count);
    static int64_t seek(void* opaque, int64_t offset, int mode);
};
}
