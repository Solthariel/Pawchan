#include "duration_probe.hpp"
#include "transport.hpp"
#include <algorithm>
#include <cerrno>
extern "C" {
#include <libavutil/time.h>
#include <libavutil/mem.h>
}
namespace paw {
namespace {
constexpr int64_t MAX_FILE = 16 * 1024 * 1024;
constexpr int64_t MAX_READ = 32 * 1024 * 1024;
constexpr int64_t MAX_TIME_US = 500000;
constexpr int MAX_PACKETS = 100000;
struct Scan {
    Input input;
    const std::atomic<bool>& quit;
    int64_t deadline = av_gettime_relative() + MAX_TIME_US;
    int64_t bytes = 0;
    bool limited = false;
    Scan(JNIEnv* env, jobject reader, const std::atomic<bool>& cancelled) : input(env, reader), quit(cancelled) {}
    static int interrupted(void* opaque) {
        auto& scan = *static_cast<Scan*>(opaque);
        if (scan.quit || av_gettime_relative() >= scan.deadline || scan.bytes >= MAX_READ) scan.limited = true;
        return scan.limited;
    }
    static int read(void* opaque, uint8_t* data, int count) {
        auto& scan = *static_cast<Scan*>(opaque);
        if (interrupted(opaque)) return AVERROR_EXIT;
        int result = Input::read(&scan.input, data, static_cast<int>(std::min<int64_t>(count, MAX_READ - scan.bytes)));
        if (result > 0) scan.bytes += result;
        return result;
    }
    static int64_t seek(void* opaque, int64_t offset, int mode) {
        auto& scan = *static_cast<Scan*>(opaque);
        return interrupted(opaque) ? AVERROR_EXIT : Input::seek(&scan.input, offset, mode);
    }
};
bool uncertain(AVFormatContext* source) {
    int64_t streamMs = 0;
    for (unsigned i = 0; i < source->nb_streams; ++i) {
        AVStream* stream = source->streams[i];
        auto type = stream->codecpar->codec_type;
        if (type != AVMEDIA_TYPE_AUDIO && type != AVMEDIA_TYPE_VIDEO) continue;
        if (stream->disposition & AV_DISPOSITION_ATTACHED_PIC) continue;
        if (stream->duration > 0) streamMs = std::max(streamMs, av_rescale_q(stream->duration, stream->time_base, AVRational{1, 1000}));
    }
    int64_t declared = source->duration > 0 ? source->duration / 1000 : 0;
    return streamMs <= 0 || std::max(declared, streamMs) - std::min(declared, streamMs) > 1000;
}
int64_t packetEnd(AVFormatContext* format, const AVPacket* packet, int64_t origin) {
    if (packet->stream_index < 0 || static_cast<unsigned>(packet->stream_index) >= format->nb_streams) return 0;
    AVStream* stream = format->streams[packet->stream_index];
    auto type = stream->codecpar->codec_type;
    if ((type != AVMEDIA_TYPE_AUDIO && type != AVMEDIA_TYPE_VIDEO) ||
            (stream->disposition & AV_DISPOSITION_ATTACHED_PIC)) return 0;
    int64_t stamp = packet->pts != AV_NOPTS_VALUE ? packet->pts : packet->dts;
    if (stamp == AV_NOPTS_VALUE) return 0;
    int64_t length = std::max<int64_t>(0, packet->duration);
    if (stamp > INT64_MAX - length) return 0;
    int64_t end = av_rescale_q(stamp + length, stream->time_base, AV_TIME_BASE_Q);
    if (end <= origin || (origin < 0 && end > INT64_MAX + origin)) return 0;
    return (end - origin) / 1000;
}
int64_t measure(Scan& scan, int64_t total, int64_t origin) {
    if (!scan.input.valid()) return 0;
    scan.input.io->opaque = &scan;
    scan.input.io->read_packet = Scan::read;
    scan.input.io->seek = Scan::seek;
    AVFormatContext* format = avformat_alloc_context();
    if (!format) return 0;
    format->pb = scan.input.io; format->flags |= AVFMT_FLAG_CUSTOM_IO;
    format->interrupt_callback = AVIOInterruptCB{Scan::interrupted, &scan};
    format->protocol_whitelist = av_strdup("");
    format->probesize = 1024 * 1024;
    if (!format->protocol_whitelist || avformat_open_input(&format, "", nullptr, nullptr) < 0) {
        avformat_close_input(&format); return 0;
    }
    AVPacket* packet = av_packet_alloc();
    int result = 0, packets = 0;
    int64_t observed = 0;
    if (packet) {
        while (!Scan::interrupted(&scan) && packets < MAX_PACKETS && (result = av_read_frame(format, packet)) >= 0) {
            observed = std::max(observed, packetEnd(format, packet, origin));
            av_packet_unref(packet); ++packets;
        }
    }
    bool verified = packet && result == AVERROR_EOF && !Scan::interrupted(&scan) &&
            packets < MAX_PACKETS && scan.input.position() >= total;
    av_packet_free(&packet); avformat_close_input(&format);
    return verified ? observed : 0;
}
}
int64_t DurationProbe::check(JNIEnv* env, jobject reader, AVFormatContext* source, const std::atomic<bool>& quit) {
    if (finished_ || quit) return 0;
    if (!uncertain(source)) { finished_ = true; return 0; }
    jclass type = env->GetObjectClass(reader);
    jmethodID complete = env->GetMethodID(type, "complete", "()Z");
    jmethodID length = env->GetMethodID(type, "length", "()J");
    env->DeleteLocalRef(type);
    if (!complete || !length || env->ExceptionCheck()) {
        env->ExceptionClear(); finished_ = true; return 0;
    }
    int64_t total = env->CallLongMethod(reader, length);
    if (env->ExceptionCheck()) { env->ExceptionClear(); finished_ = true; return 0; }
    // A known ineligible large file must not lose seek while it downloads.
    if (total > MAX_FILE) { finished_ = true; return 0; }
    if (total <= 0) return 0;
    bool available = env->CallBooleanMethod(reader, complete);
    if (env->ExceptionCheck()) { env->ExceptionClear(); finished_ = true; return 0; }
    if (!available) return 0;
    finished_ = true;
    Scan scan(env, reader, quit);
    int64_t origin = source->start_time != AV_NOPTS_VALUE ? source->start_time : 0;
    int64_t observed = measure(scan, total, origin);
    int64_t declared = source->duration > 0 ? source->duration / 1000 : 0;
    return observed > 0 && std::max(declared, observed) - std::min(declared, observed) > 1000 ? observed : 0;
}
}
