#pragma once
#include "decoder.hpp"
#include <algorithm>
namespace paw {
// Decode-thread owner. A seek may discard every frame in a malformed tail;
// retain only its last video frame, not a growing decoded-frame queue.
class InputEnd {
    int64_t end_ = 0;
    AVFrame* fallback_ = nullptr;
public:
    ~InputEnd() { av_frame_free(&fallback_); }
    InputEnd() = default;
    InputEnd(const InputEnd&) = delete;
    InputEnd& operator=(const InputEnd&) = delete;
    void reset() { end_ = 0; av_frame_free(&fallback_); }
    bool observe(const Decoder& decoder, const AVFrame* frame, int64_t pts, bool video, int64_t target) {
        int64_t length = frame->duration > 0 ? av_rescale_q(frame->duration, decoder.timeBase, AVRational{1, 1000000}) : 0;
        if (!video && frame->sample_rate > 0) length = static_cast<int64_t>(frame->nb_samples) * 1000000 / frame->sample_rate;
        end_ = std::max(end_, pts + std::clamp<int64_t>(length, 0, INT64_MAX - pts));
        if (!video) return true;
        if (target < 0 || pts >= target) { av_frame_free(&fallback_); return true; }
        AVFrame* next = av_frame_clone(frame);
        if (!next) return false;
        av_frame_free(&fallback_); fallback_ = next;
        return true;
    }
    // Tolerate rounding, encoder padding and ordinary unequal track lengths.
    bool shortened(int64_t declaredMs) const { return end_ > 0 && declaredMs > end_ / 1000 + 1000; }
    int64_t duration() const { return end_ / 1000; }
    const AVFrame* fallback() const { return fallback_; }
};
}
