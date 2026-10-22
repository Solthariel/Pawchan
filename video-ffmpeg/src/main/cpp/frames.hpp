#pragma once
#include "decoder.hpp"
#include <cstddef>
#include <deque>
namespace paw {
// Demux order need not be playback order. Buffer compressed packets, never seconds
// of decoded RGBA frames. One decoded frame per stream is enough for scheduling.
class Frames {
    AVFormatContext* format_;
    Decoder &video_, &sound_;
    std::deque<AVPacket*> videoPackets_, soundPackets_;
    size_t bytes_ = 0, packets_ = 0;
    bool inputEnded_ = false, videoFlushed_ = false, soundFlushed_ = false;
    AVFrame *videoFrame_ = nullptr, *soundFrame_ = nullptr;
public:
    Frames(AVFormatContext* format, Decoder& video, Decoder& sound);
    ~Frames();
    Frames(const Frames&) = delete;
    Frames& operator=(const Frames&) = delete;
    // 1: frame ready, 0: drained, 2: let the other stream advance, negative: error.
    int fill(bool video);
    AVFrame* get(bool video) const { return video ? videoFrame_ : soundFrame_; }
    void consume(bool video);
    void reset();
private:
    int packet(Decoder& decoder, AVPacket* target);
};
}
