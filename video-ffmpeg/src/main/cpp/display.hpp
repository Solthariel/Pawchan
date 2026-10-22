#pragma once
#include <android/native_window.h>
#include <mutex>
#include <vector>
extern "C" {
#include <libavutil/frame.h>
#include <libswscale/swscale.h>
}
namespace paw {
class Display {
    std::mutex mutex_;
    ANativeWindow* incoming_ = nullptr;
    ANativeWindow* window_ = nullptr;
    bool changed_ = false;
    SwsContext* converter_ = nullptr;
    std::vector<uint8_t> pixels_;
    int width_ = 0, height_ = 0, rotation_ = 0;
public:
    ~Display();
    void offer(ANativeWindow* owned);
    bool update(const AVFrame* frame, int rotation);
    bool present();
};
}
