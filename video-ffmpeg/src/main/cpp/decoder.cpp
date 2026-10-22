#include "decoder.hpp"
namespace paw {
static const char* hardwareName(AVCodecID codec) {
    switch (codec) {
        case AV_CODEC_ID_H264: return "h264_mediacodec";
        case AV_CODEC_ID_HEVC: return "hevc_mediacodec";
        case AV_CODEC_ID_VP8: return "vp8_mediacodec";
        case AV_CODEC_ID_VP9: return "vp9_mediacodec";
        case AV_CODEC_ID_AV1: return "av1_mediacodec";
        default: return nullptr;
    }
}
Decoder::~Decoder() { avcodec_free_context(&context); }
bool Decoder::open(AVFormatContext* format, AVMediaType type, bool software) {
    stream = av_find_best_stream(format, type, -1, -1, nullptr, 0);
    if (stream < 0) return type == AVMEDIA_TYPE_AUDIO;
    AVStream* input = format->streams[stream]; timeBase = input->time_base;
    const char* hardware = type == AVMEDIA_TYPE_VIDEO && !software ? hardwareName(input->codecpar->codec_id) : nullptr;
    const AVCodec* codec = hardware ? avcodec_find_decoder_by_name(hardware) : nullptr;
    for (int attempt = 0; attempt < 2; ++attempt) {
        if (!codec) codec = avcodec_find_decoder(input->codecpar->codec_id);
        if (!codec) return false;
        context = avcodec_alloc_context3(codec);
        if (!context) return false;
        int error = avcodec_parameters_to_context(context, input->codecpar);
        context->thread_count = 2;
        if (error >= 0) error = avcodec_open2(context, codec, nullptr);
        if (error >= 0) return true;
        avcodec_free_context(&context); codec = nullptr;
    }
    return false;
}
int64_t Decoder::timestamp(const AVFrame* frame, int64_t fallback) const {
    int64_t value = frame->best_effort_timestamp;
    return value == AV_NOPTS_VALUE ? fallback : av_rescale_q(value, timeBase, AVRational{1, 1000000});
}
}
