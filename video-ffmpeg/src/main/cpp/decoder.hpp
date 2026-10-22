#pragma once
extern "C" {
#include <libavcodec/avcodec.h>
#include <libavformat/avformat.h>
}
namespace paw {
class Decoder {
public:
    AVCodecContext* context = nullptr;
    int stream = -1;
    AVRational timeBase{1, 1000000};
    ~Decoder();
    bool open(AVFormatContext* format, AVMediaType type, bool software);
    int64_t timestamp(const AVFrame* frame, int64_t fallback) const;
};
}
