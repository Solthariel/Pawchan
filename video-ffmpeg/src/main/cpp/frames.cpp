#include "frames.hpp"
#include <cerrno>
namespace paw {
namespace {
constexpr size_t MAX_BYTES = 32 * 1024 * 1024;
constexpr size_t MAX_PACKETS = 8192;
constexpr size_t LOOKAHEAD_BYTES = 8 * 1024 * 1024;
constexpr size_t LOOKAHEAD_PACKETS = 512;
}
Frames::Frames(AVFormatContext* format, Decoder& video, Decoder& sound)
    : format_(format), video_(video), sound_(sound) {}
Frames::~Frames() { reset(); }
void Frames::consume(bool video) { av_frame_free(video ? &videoFrame_ : &soundFrame_); }
void Frames::reset() {
    consume(true); consume(false);
    for (auto* queue : {&videoPackets_, &soundPackets_}) {
        while (!queue->empty()) { av_packet_free(&queue->front()); queue->pop_front(); }
    }
    bytes_ = 0; packets_ = 0;
    inputEnded_ = false; videoFlushed_ = false; soundFlushed_ = false;
}
int Frames::packet(Decoder& decoder, AVPacket* target) {
    auto& queue = decoder.stream == video_.stream ? videoPackets_ : soundPackets_;
    if (!queue.empty()) {
        AVPacket* front = queue.front(); queue.pop_front();
        bytes_ -= static_cast<size_t>(front->size); --packets_;
        av_packet_move_ref(target, front); av_packet_free(&front);
        return 0;
    }
    while (!inputEnded_) {
        // An audio stream can end long before video (or start much later).
        // Yield to queued packets instead of reading the entire remainder first.
        if (bytes_ >= LOOKAHEAD_BYTES || packets_ >= LOOKAHEAD_PACKETS) return AVERROR(EAGAIN);
        int result = av_read_frame(format_, target);
        if (result == AVERROR_EOF) { inputEnded_ = true; break; }
        if (result < 0) return result;
        if (target->stream_index == decoder.stream) return 0;
        if (target->stream_index == video_.stream || target->stream_index == sound_.stream) {
            size_t size = static_cast<size_t>(target->size);
            // An unusual/unbounded interleave fails cleanly rather than exhausting RAM.
            if (packets_ >= MAX_PACKETS || size > MAX_BYTES - bytes_) return AVERROR(ENOBUFS);
            AVPacket* saved = av_packet_alloc();
            if (!saved) return AVERROR(ENOMEM);
            auto& destination = target->stream_index == video_.stream ? videoPackets_ : soundPackets_;
            destination.push_back(saved);
            av_packet_move_ref(saved, target); bytes_ += size; ++packets_;
        } else {
            av_packet_unref(target);
        }
    }
    return AVERROR_EOF;
}
int Frames::fill(bool video) {
    auto& decoder = video ? video_ : sound_;
    auto*& frame = video ? videoFrame_ : soundFrame_;
    bool& flushed = video ? videoFlushed_ : soundFlushed_;
    if (frame) return 1;
    if (!decoder.context) return 0;
    AVFrame* decoded = av_frame_alloc();
    AVPacket* input = av_packet_alloc();
    if (!decoded || !input) { av_frame_free(&decoded); av_packet_free(&input); return AVERROR(ENOMEM); }
    int result;
    while ((result = avcodec_receive_frame(decoder.context, decoded)) == AVERROR(EAGAIN)) {
        if (flushed) { result = AVERROR_INVALIDDATA; break; }
        result = packet(decoder, input);
        if (result == AVERROR_EOF) {
            flushed = true;
            result = avcodec_send_packet(decoder.context, nullptr);
        } else if (result >= 0) {
            result = avcodec_send_packet(decoder.context, input);
        }
        av_packet_unref(input);
        if (result < 0) break;
    }
    av_packet_free(&input);
    if (result >= 0) { frame = decoded; return 1; }
    av_frame_free(&decoded);
    return result == AVERROR_EOF ? 0 : result == AVERROR(EAGAIN) ? 2 : result;
}
}
