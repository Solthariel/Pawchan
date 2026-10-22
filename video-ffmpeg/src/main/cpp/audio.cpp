#include "audio.hpp"
#include <vector>
#include <limits>
namespace paw {
Audio::Audio(JNIEnv* env, jobject output, std::function<bool()> proceed)
    : env_(env), output_(output), proceed_(std::move(proceed)) {
    jclass type = env->GetObjectClass(output);
    configure_ = env->GetMethodID(type, "configure", "()V");
    write_ = env->GetMethodID(type, "write", "([BI)I");
    remaining_ = env->GetMethodID(type, "remaining", "()J");
    flush_ = env->GetMethodID(type, "flush", "()V"); env->DeleteLocalRef(type);
}
Audio::~Audio() { swr_free(&converter_); av_channel_layout_uninit(&layout_); }
bool Audio::accept(const AVFrame* frame) {
    if (frame->sample_rate <= 0 || frame->nb_samples < 0) return false;
    if (!converter_ || rate_ != frame->sample_rate || format_ != frame->format ||
            av_channel_layout_compare(&layout_, &frame->ch_layout) != 0) {
        swr_free(&converter_); av_channel_layout_uninit(&layout_);
        AVChannelLayout stereo = AV_CHANNEL_LAYOUT_STEREO;
        if (swr_alloc_set_opts2(&converter_, &stereo, AV_SAMPLE_FMT_S16, 48000,
                &frame->ch_layout, static_cast<AVSampleFormat>(frame->format), frame->sample_rate, 0, nullptr) < 0)
            return false;
        if (swr_init(converter_) < 0 || av_channel_layout_copy(&layout_, &frame->ch_layout) < 0) return false;
        rate_ = frame->sample_rate; format_ = frame->format;
    }
    if (!configured_) {
        env_->CallVoidMethod(output_, configure_); configured_ = true;
        if (env_->ExceptionCheck()) { env_->ExceptionClear(); return false; }
    }
    return convert(frame);
}
bool Audio::convert(const AVFrame* frame) {
    int count = swr_get_out_samples(converter_, frame ? frame->nb_samples : 0);
    if (count < 0 || count > 480000) return false;
    std::vector<uint8_t> bytes(static_cast<size_t>(count) * 4);
    uint8_t* target = bytes.data();
    const uint8_t** input = frame ? const_cast<const uint8_t**>(frame->extended_data) : nullptr;
    int samples = swr_convert(converter_, &target, count, input, frame ? frame->nb_samples : 0);
    if (samples < 0) return false;
    if (samples == 0) return true;
    jbyteArray data = env_->NewByteArray(samples * 4);
    if (!data) return false;
    env_->SetByteArrayRegion(data, 0, samples * 4, reinterpret_cast<jbyte*>(bytes.data()));
    int offset = 0;
    bool ok = true;
    while (offset < samples * 4) {
        if (!proceed_()) break; // A pending seek discards this old audio block.
        int written = env_->CallIntMethod(output_, write_, data, offset);
        if (env_->ExceptionCheck() || written < 0) { ok = false; break; }
        offset += written;
    }
    env_->DeleteLocalRef(data);
    if (env_->ExceptionCheck()) { env_->ExceptionClear(); return false; }
    return ok;
}
bool Audio::drain() {
    if (converter_ && !convert(nullptr)) return false;
    while (env_->CallLongMethod(output_, remaining_) > 0) {
        if (env_->ExceptionCheck()) { env_->ExceptionClear(); return false; }
        if (!proceed_()) break;
    }
    return true;
}
void Audio::reset() {
    if (converter_) { swr_close(converter_); swr_init(converter_); }
    env_->CallVoidMethod(output_, flush_);
    if (env_->ExceptionCheck()) env_->ExceptionClear();
}
}
