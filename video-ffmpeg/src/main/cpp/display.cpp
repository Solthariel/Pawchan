#include "display.hpp"
#include <algorithm>
#include <cstring>
namespace paw {
Display::~Display() {
    if (incoming_) ANativeWindow_release(incoming_);
    if (window_) ANativeWindow_release(window_);
    sws_freeContext(converter_);
}
void Display::offer(ANativeWindow* owned) {
    std::lock_guard<std::mutex> lock(mutex_);
    if (incoming_) ANativeWindow_release(incoming_);
    incoming_ = owned; changed_ = true;
}
bool Display::update(const AVFrame* frame, int rotation) {
    if (frame->width <= 0 || frame->height <= 0 || frame->width > 8192 || frame->height > 8192) return false;
    if (!sws_isSupportedInput(static_cast<AVPixelFormat>(frame->format))) return false;
    converter_ = sws_getCachedContext(converter_, frame->width, frame->height,
            static_cast<AVPixelFormat>(frame->format), frame->width, frame->height, AV_PIX_FMT_RGBA,
            SWS_BILINEAR, nullptr, nullptr, nullptr);
    if (!converter_) return false;
    width_ = frame->width; height_ = frame->height; rotation_ = rotation;
    pixels_.resize(static_cast<size_t>(width_) * height_ * 4);
    uint8_t* targets[] = {pixels_.data(), nullptr, nullptr, nullptr};
    int strides[] = {width_ * 4, 0, 0, 0};
    return sws_scale(converter_, frame->data, frame->linesize, 0, height_, targets, strides) > 0;
}
bool Display::present() {
    {
        std::lock_guard<std::mutex> lock(mutex_);
        if (changed_) {
            if (window_) ANativeWindow_release(window_);
            window_ = incoming_; incoming_ = nullptr; changed_ = false;
        }
    }
    if (!window_ || pixels_.empty()) return true;
    bool swapped = rotation_ == 90 || rotation_ == 270;
    int w = swapped ? height_ : width_, h = swapped ? width_ : height_;
    if (ANativeWindow_setBuffersGeometry(window_, w, h, WINDOW_FORMAT_RGBA_8888) != 0) return false;
    ANativeWindow_Buffer buffer{};
    if (ANativeWindow_lock(window_, &buffer, nullptr) != 0) return false;
    auto* destination = static_cast<uint32_t*>(buffer.bits);
    auto* source = reinterpret_cast<const uint32_t*>(pixels_.data());
    for (int y = 0; y < h; ++y) {
        if (rotation_ == 0) {
            std::memcpy(destination + y * buffer.stride, source + y * width_, static_cast<size_t>(w) * 4);
            continue;
        }
        for (int x = 0; x < w; ++x) {
            int sx = x, sy = y;
            if (rotation_ == 90) { sx = y; sy = height_ - 1 - x; }
            else if (rotation_ == 180) { sx = width_ - 1 - x; sy = height_ - 1 - y; }
            else if (rotation_ == 270) { sx = width_ - 1 - y; sy = x; }
            destination[y * buffer.stride + x] = source[sy * width_ + sx];
        }
    }
    return ANativeWindow_unlockAndPost(window_) == 0;
}
}
