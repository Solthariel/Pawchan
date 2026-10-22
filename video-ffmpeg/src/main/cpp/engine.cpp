#include "engine.hpp"
#include "frames.hpp"
#include <chrono>
#include <cmath>
extern "C" {
#include <libavutil/display.h>
#include <libavutil/time.h>
#include <libavutil/mem.h>
}
namespace paw {
int Engine::interrupted(void* opaque) { return static_cast<Engine*>(opaque)->quit_.load(); }
bool Engine::open(JNIEnv* env, jobject reader, jobject output, bool software) {
    reader_ = env->NewGlobalRef(reader); output_ = env->NewGlobalRef(output);
    if (!reader_ || !output_) return false;
    input_ = std::make_unique<Input>(env, reader_);
    if (!input_->valid()) return false;
    format_ = avformat_alloc_context();
    if (!format_) return false;
    format_->pb = input_->io; format_->flags |= AVFMT_FLAG_CUSTOM_IO;
    format_->interrupt_callback = AVIOInterruptCB{interrupted, this};
    format_->protocol_whitelist = av_strdup("");
    if (!format_->protocol_whitelist) return false;
    if (avformat_open_input(&format_, "", nullptr, nullptr) < 0 ||
            avformat_find_stream_info(format_, nullptr) < 0) return false;
    if (!video_.open(format_, AVMEDIA_TYPE_VIDEO, software) || !sound_.open(format_, AVMEDIA_TYPE_AUDIO, true)) return false;
    origin_ = format_->start_time == AV_NOPTS_VALUE ? 0 : format_->start_time;
    if (format_->duration != AV_NOPTS_VALUE) duration_ = std::max<int64_t>(0, format_->duration / 1000);
    declaredDuration_ = duration_;
    int64_t verified = durationProbe_.check(env, reader_, format_, quit_);
    if (verified > 0) duration_ = verified;
    if (!durationProbe_.pending()) publishedDuration_ = duration_.load();
    AVCodecParameters* parameters = format_->streams[video_.stream]->codecpar;
    const AVPacketSideData* matrix = av_packet_side_data_get(parameters->coded_side_data,
            parameters->nb_coded_side_data, AV_PKT_DATA_DISPLAYMATRIX);
    if (matrix && matrix->size >= 9 * sizeof(int32_t)) {
        double angle = -av_display_rotation_get(reinterpret_cast<const int32_t*>(matrix->data));
        if (std::isfinite(angle)) rotation_ = (static_cast<int>(std::lround(angle / 90)) * 90 % 360 + 360) % 360;
    }
    width_ = rotation_ % 180 ? video_.context->height : video_.context->width;
    height_ = rotation_ % 180 ? video_.context->width : video_.context->height;
    // Open, pump and release are confined to one Java decode thread.
    return true;
}
bool Engine::ready() {
    std::unique_lock<std::mutex> lock(mutex_);
    while (!quit_ && !playing_ && !preview_ && pendingSeek_ < 0) {
        if (redraw_) {
            redraw_ = false; lock.unlock(); display_.present(); lock.lock();
        }
        changed_.wait_for(lock, std::chrono::milliseconds(20));
    }
    if (redraw_) { redraw_ = false; lock.unlock(); display_.present(); lock.lock(); }
    return !quit_;
}
bool Engine::audioReady() {
    std::unique_lock<std::mutex> lock(mutex_);
    if (redraw_) { redraw_ = false; lock.unlock(); display_.present(); lock.lock(); }
    if (quit_ || pendingSeek_ >= 0) return false;
    changed_.wait_for(lock, std::chrono::milliseconds(5));
    return !quit_ && pendingSeek_ < 0;
}
bool Engine::waitUntil(int64_t pts) {
    std::unique_lock<std::mutex> lock(mutex_);
    while (!quit_ && pendingSeek_ < 0) {
        if (redraw_) { redraw_ = false; lock.unlock(); display_.present(); lock.lock(); }
        if (preview_) return true;
        if (!playing_) { changed_.wait_for(lock, std::chrono::milliseconds(20)); continue; }
        int64_t remaining = pts - timeline_.position(av_gettime_relative());
        if (remaining <= 0) return true;
        changed_.wait_for(lock, std::chrono::microseconds(std::min<int64_t>(remaining, 10000)));
    }
    return false;
}
bool Engine::frame(Decoder& decoder, AVFrame* decoded, bool video) {
    int64_t pts = std::max<int64_t>(0, decoder.timestamp(decoded, position_ * 1000 + origin_) - origin_);
    if (!inputEnd_.observe(decoder, decoded, pts, video, discardBefore_)) return false;
    if (discardBefore_ >= 0 && pts < discardBefore_) return true;
    if (video) {
        if (!waitUntil(pts)) return !quit_;
        if (!display_.update(decoded, rotation_)) return false;
        display_.present(); position_ = pts / 1000;
        { std::lock_guard<std::mutex> lock(mutex_); preview_ = false; }
    } else {
        { std::lock_guard<std::mutex> lock(mutex_); if (preview_) return true; }
        if (!waitUntil(std::max<int64_t>(0, pts - 100000))) return !quit_;
        if (!audio_->accept(decoded)) return false;
    }
    return true;
}
bool Engine::seek(int64_t target) {
    int64_t timestamp = target * 1000 + origin_;
    if (avformat_seek_file(format_, -1, INT64_MIN, timestamp, timestamp, AVSEEK_FLAG_BACKWARD) < 0) return false;
    avcodec_flush_buffers(video_.context);
    if (sound_.context) avcodec_flush_buffers(sound_.context);
    audio_->reset(); inputEnd_.reset(); discardBefore_ = target * 1000;
    std::lock_guard<std::mutex> lock(mutex_); preview_ = !playing_; timeline_.anchor(target * 1000, av_gettime_relative());
    return true;
}
int Engine::pump(JNIEnv* env) {
    if (!audio_) audio_ = std::make_unique<Audio>(env, output_, [this] { return audioReady(); });
    Frames frames(format_, video_, sound_);
    bool ok = true;
    int ended = 0;
    while (ok && ready()) {
        int64_t verified = durationProbe_.check(env, reader_, format_, quit_);
        if (verified > 0) duration_ = verified;
        if (!durationProbe_.pending()) publishedDuration_ = duration_.load();
        int64_t target;
        bool preview;
        { std::lock_guard<std::mutex> lock(mutex_); target = pendingSeek_; pendingSeek_ = -1; }
        if (target >= 0) { frames.reset(); ok = seek(target); }
        if (!ok) break;
        { std::lock_guard<std::mutex> lock(mutex_); preview = preview_; }
        int video = frames.fill(true);
        int sound = preview && video == 1 ? 0 : frames.fill(false);
        if (video < 0 || sound < 0) { ok = false; break; }
        if (video == 0 && sound == 0) {
            ok = audio_->drain();
            bool shortened = inputEnd_.shortened(std::max(declaredDuration_, duration_.load()));
            const AVFrame* fallback = inputEnd_.fallback();
            if (ok && shortened && fallback) {
                ok = display_.update(fallback, rotation_) && display_.present();
                if (ok) position_ = std::max<int64_t>(0, video_.timestamp(fallback, origin_) - origin_) / 1000;
            }
            if (ok) {
                std::lock_guard<std::mutex> lock(mutex_);
                if (pendingSeek_ >= 0) continue;
                if (shortened) duration_ = inputEnd_.duration();
                publishedDuration_ = duration_.load();
                eofRevision_ = seekRevision_.load();
                playing_ = false; preview_ = false;
                timeline_.anchor(position_ * 1000, av_gettime_relative());
                timeline_.running(false, av_gettime_relative());
            }
            ended = ok ? (shortened ? 2 : 1) : 0;
            break;
        }
        if (video != 1 && sound != 1) continue;
        bool showVideo = video == 1;
        if (video == 1 && sound == 1) {
            // Feed PCM slightly ahead, but never write a whole demux chunk before
            // presenting its video. DTS/B-frame reordering stays inside each decoder.
            int64_t videoPts = video_.timestamp(frames.get(true), position_ * 1000 + origin_);
            int64_t soundPts = sound_.timestamp(frames.get(false), position_ * 1000 + origin_);
            showVideo = videoPts <= soundPts - 100000;
        }
        ok = frame(showVideo ? video_ : sound_, frames.get(showVideo), showVideo);
        frames.consume(showVideo);
    }
    return quit_ ? 0 : ended ? ended : ok ? 0 : -1;
}
void Engine::command(int type, int64_t value) {
    std::lock_guard<std::mutex> lock(mutex_);
    int64_t now = av_gettime_relative();
    if (type == 1) { playing_ = value != 0; timeline_.running(playing_ && !buffering_, now); }
    else if (type == 2) {
        ++seekRevision_;
        pendingSeek_ = std::clamp<int64_t>(value, 0, duration_ > 0 ? duration_.load() : INT64_MAX / 1000);
    }
    else if (type == 3) timeline_.speed(static_cast<int>(value), now);
    else if (type == 4) quit_ = true;
    else if (type == 5) { buffering_ = value != 0; timeline_.running(playing_ && !buffering_, now); }
    changed_.notify_all();
}
void Engine::surface(ANativeWindow* owned) {
    display_.offer(owned);
    std::lock_guard<std::mutex> lock(mutex_); redraw_ = true; changed_.notify_all();
}
void Engine::status(int64_t* values) { values[0] = position_; values[1] = publishedDuration_; values[2] = width_; values[3] = height_; values[4] = seekRevision_; values[5] = eofRevision_; }
std::vector<std::string> Engine::metadata() const {
    auto* video = video_.context;
    AVRational rate = av_guess_frame_rate(format_, format_->streams[video_.stream], nullptr);
    std::vector<std::string> values{
        "width", std::to_string(width_.load()), "height", std::to_string(height_.load()),
        "video_format", avcodec_get_name(video->codec_id), "frame_rate", std::to_string(av_q2d(rate)),
        "video_decoder_backend", video->codec->name};
    if (sound_.context) {
        values.insert(values.end(), {"audio_format", avcodec_get_name(sound_.context->codec_id),
            "channels", std::to_string(sound_.context->ch_layout.nb_channels),
            "sample_rate", std::to_string(sound_.context->sample_rate)});
    }
    for (const char* key : {"title", "encoder"}) {
        AVDictionaryEntry* entry = av_dict_get(format_->metadata, key, nullptr, 0);
        if (entry && entry->value) { values.emplace_back(key); values.emplace_back(std::string(entry->value).substr(0, 1024)); }
    }
    return values;
}
void Engine::release(JNIEnv* env) {
    audio_.reset(); avformat_close_input(&format_); input_.reset();
    if (reader_) env->DeleteGlobalRef(reader_);
    if (output_) env->DeleteGlobalRef(output_);
    reader_ = nullptr; output_ = nullptr;
}
}
