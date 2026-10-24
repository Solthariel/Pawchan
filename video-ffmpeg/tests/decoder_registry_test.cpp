#include "decoder.hpp"
#include <cassert>
#include <cstdint>
#include <iostream>
#include <vector>

namespace {
struct RegisteredCodec {
    AVCodec codec{};
    bool decoder = true;
    int error = 0;
};
std::vector<RegisteredCodec*> registry;
std::vector<const AVCodec*> attempts;
int softwareLookups = 0;
int availableStream = 0;
int releasedContexts = 0;
RegisteredCodec* registered(const AVCodec* codec) {
    for (auto* entry : registry) if (&entry->codec == codec) return entry;
    return nullptr;
}
RegisteredCodec entry(AVCodecID id, const char* name, const char* wrapper, bool decoder = true) {
    RegisteredCodec result;
    result.codec.id = id;
    result.codec.name = name;
    result.codec.wrapper_name = wrapper;
    result.codec.type = AVMEDIA_TYPE_VIDEO;
    result.decoder = decoder;
    return result;
}
void reset() {
    registry.clear(); attempts.clear(); softwareLookups = 0; availableStream = 0; releasedContexts = 0;
}
void openVideo(paw::Decoder& decoder, AVCodecID id, bool software = false) {
    AVCodecParameters parameters{}; parameters.codec_id = id;
    AVStream stream{}; stream.codecpar = &parameters; stream.time_base = AVRational{1, 1000};
    AVStream* streams[] = {&stream};
    AVFormatContext format{}; format.streams = streams; format.nb_streams = 1;
    assert(decoder.open(&format, AVMEDIA_TYPE_VIDEO, software));
}
void selectsRegisteredWrappersForAllPreviouslySupportedFormats() {
    for (AVCodecID id : {AV_CODEC_ID_AV1, AV_CODEC_ID_VP9, AV_CODEC_ID_H264, AV_CODEC_ID_VP8, AV_CODEC_ID_HEVC}) {
        reset();
        auto software = entry(id, "software", nullptr);
        auto unrelated = entry(AV_CODEC_ID_MPEG2VIDEO, "other", "mediacodec");
        auto encoder = entry(id, "encoder", "mediacodec", false);
        auto wrongWrapper = entry(id, "another_backend", "other");
        auto hardware = entry(id, "registered_platform_decoder", "mediacodec");
        auto audio = entry(id, "not_video", "mediacodec"); audio.codec.type = AVMEDIA_TYPE_AUDIO;
        registry = {&software, &unrelated, &encoder, &wrongWrapper, &audio, &hardware};
        { paw::Decoder decoder; openVideo(decoder, id); assert(decoder.context->codec == &hardware.codec); }
        assert(softwareLookups == 0 && attempts.size() == 1 && releasedContexts == 1);
    }
}
void forcedSoftwareNeverUsesHardware() {
    reset();
    auto hardware = entry(AV_CODEC_ID_H264, "platform", "mediacodec");
    auto software = entry(AV_CODEC_ID_H264, "software", nullptr);
    registry = {&hardware, &software};
    paw::Decoder decoder; openVideo(decoder, AV_CODEC_ID_H264, true);
    assert(decoder.context->codec == &software.codec && softwareLookups == 1);
}
void missingHardwareUsesSoftware() {
    reset(); auto software = entry(AV_CODEC_ID_VP9, "software", nullptr); registry = {&software};
    paw::Decoder decoder; openVideo(decoder, AV_CODEC_ID_VP9);
    assert(decoder.context->codec == &software.codec && attempts.size() == 1);
}
void failedHardwareIsReleasedBeforeSoftwareFallback() {
    reset();
    auto hardware = entry(AV_CODEC_ID_HEVC, "platform", "mediacodec"); hardware.error = -1;
    auto software = entry(AV_CODEC_ID_HEVC, "software", nullptr); registry = {&hardware, &software};
    { paw::Decoder decoder; openVideo(decoder, AV_CODEC_ID_HEVC);
      assert(decoder.context->codec == &software.codec && releasedContexts == 1);
      assert(attempts == std::vector<const AVCodec*>({&hardware.codec, &software.codec})); }
    assert(releasedContexts == 2);
}
void unsupportedVideoFailsAndAbsentAudioIsOptional() {
    reset();
    AVCodecParameters parameters{}; parameters.codec_id = AV_CODEC_ID_H264;
    AVStream stream{}; stream.codecpar = &parameters; AVStream* streams[] = {&stream};
    AVFormatContext format{}; format.streams = streams; format.nb_streams = 1;
    paw::Decoder decoder;
    assert(!decoder.open(&format, AVMEDIA_TYPE_VIDEO, false));
    assert(decoder.context == nullptr);
    availableStream = -1;
    assert(decoder.open(&format, AVMEDIA_TYPE_AUDIO, false));
    assert(!decoder.open(&format, AVMEDIA_TYPE_VIDEO, false));
}
void timestampsRetainTheirFallbackAndRescaling() {
    paw::Decoder decoder; decoder.timeBase = AVRational{1, 1000}; AVFrame frame{};
    frame.best_effort_timestamp = AV_NOPTS_VALUE; assert(decoder.timestamp(&frame, 123) == 123);
    frame.best_effort_timestamp = 250; assert(decoder.timestamp(&frame, 0) == 250000);
}
}
extern "C" {
const AVCodec* av_codec_iterate(void** opaque) {
    auto index = reinterpret_cast<uintptr_t>(*opaque);
    if (index >= registry.size()) return nullptr;
    *opaque = reinterpret_cast<void*>(index + 1); return &registry[index]->codec;
}
int av_codec_is_decoder(const AVCodec* codec) { return registered(codec)->decoder; }
const AVCodec* avcodec_find_decoder(AVCodecID id) {
    ++softwareLookups;
    for (auto* item : registry) if (item->codec.id == id && item->decoder && !item->codec.wrapper_name) return &item->codec;
    return nullptr;
}
int av_find_best_stream(AVFormatContext*, AVMediaType, int, int, const AVCodec**, int) { return availableStream; }
AVCodecContext* avcodec_alloc_context3(const AVCodec* codec) {
    auto* context = new AVCodecContext{}; context->codec = codec; return context;
}
void avcodec_free_context(AVCodecContext** context) {
    if (*context) { delete *context; *context = nullptr; ++releasedContexts; }
}
int avcodec_parameters_to_context(AVCodecContext* context, const AVCodecParameters* parameters) {
    context->codec_id = parameters->codec_id; return 0;
}
int avcodec_open2(AVCodecContext* context, const AVCodec* codec, AVDictionary**) {
    assert(context->thread_count == 2); attempts.push_back(codec); return registered(codec)->error;
}
int64_t av_rescale_q(int64_t value, AVRational from, AVRational to) {
    return value * from.num * to.den / (static_cast<int64_t>(from.den) * to.num);
}
}
int main() {
    selectsRegisteredWrappersForAllPreviouslySupportedFormats();
    forcedSoftwareNeverUsesHardware(); missingHardwareUsesSoftware();
    failedHardwareIsReleasedBeforeSoftwareFallback(); unsupportedVideoFailsAndAbsentAudioIsOptional();
    timestampsRetainTheirFallbackAndRescaling();
    std::cout << "Decoder registry: 6 scenario groups PASS (five formats, fallback, filtering, release, timestamps)\n";
}
