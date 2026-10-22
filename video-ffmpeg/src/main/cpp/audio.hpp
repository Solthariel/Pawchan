#pragma once
#include <jni.h>
#include <functional>
extern "C" {
#include <libavutil/frame.h>
#include <libswresample/swresample.h>
}
namespace paw {
class Audio {
    JNIEnv* env_; jobject output_;
    jmethodID configure_, write_, flush_, remaining_;
    SwrContext* converter_ = nullptr;
    AVChannelLayout layout_{};
    int rate_ = 0, format_ = -1;
    bool configured_ = false;
    std::function<bool()> proceed_;
public:
    Audio(JNIEnv* env, jobject output, std::function<bool()> proceed);
    ~Audio();
    bool accept(const AVFrame* frame);
    bool drain();
    void reset();
private:
    bool convert(const AVFrame* frame);
};
}
