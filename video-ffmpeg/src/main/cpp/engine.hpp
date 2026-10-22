#pragma once
#include "transport.hpp"
#include "decoder.hpp"
#include "display.hpp"
#include "audio.hpp"
#include "timeline.hpp"
#include "input_end.hpp"
#include "duration_probe.hpp"
#include <atomic>
#include <condition_variable>
#include <memory>
#include <mutex>
#include <string>
#include <vector>
namespace paw {
class Engine {
    jobject reader_ = nullptr, output_ = nullptr;
    std::unique_ptr<Input> input_;
    std::unique_ptr<Audio> audio_;
    AVFormatContext* format_ = nullptr;
    Decoder video_, sound_;
    Display display_;
    InputEnd inputEnd_;
    DurationProbe durationProbe_;
    int64_t declaredDuration_ = 0;
    std::mutex mutex_;
    std::condition_variable changed_;
    Timeline timeline_;
    bool playing_ = false, redraw_ = false, preview_ = false, buffering_ = false;
    int64_t pendingSeek_ = -1, origin_ = 0, discardBefore_ = -1;
    int rotation_ = 0;
    std::atomic<int64_t> position_{0}, duration_{0}, publishedDuration_{0}, seekRevision_{0}, eofRevision_{0};
    std::atomic<int> width_{0}, height_{0};
    std::atomic<bool> quit_{false};
public:
    bool open(JNIEnv* env, jobject reader, jobject output, bool software);
    int pump(JNIEnv* env);
    void command(int type, int64_t value);
    void surface(ANativeWindow* owned);
    void status(int64_t* values);
    std::vector<std::string> metadata() const;
    void release(JNIEnv* env);
private:
    bool ready();
    bool audioReady();
    bool waitUntil(int64_t pts);
    bool frame(Decoder& decoder, AVFrame* frame, bool video);
    bool seek(int64_t position);
    static int interrupted(void* opaque);
};
}
