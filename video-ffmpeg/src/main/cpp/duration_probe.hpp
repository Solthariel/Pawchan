#pragma once
#include <atomic>
#include <cstdint>
#include <jni.h>
extern "C" {
#include <libavformat/avformat.h>
}
namespace paw {
// Decode-thread owned. Only a complete, small published file is eligible;
// a separate demux cursor never changes playback's buffered packets or seek state.
class DurationProbe {
    bool finished_ = false;
public:
    bool pending() const { return !finished_; }
    int64_t check(JNIEnv* env, jobject reader, AVFormatContext* source, const std::atomic<bool>& quit);
};
}
