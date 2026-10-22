#pragma once
#include <algorithm>
#include <cstdint>
namespace paw {
// All timestamps are monotonic microseconds. The caller owns synchronization.
class Timeline {
    int64_t wall_ = 0, media_ = 0;
    int speed_ = 1000;
    bool running_ = false;
public:
    int64_t position(int64_t now) const {
        return media_ + (running_ ? std::max<int64_t>(0, now - wall_) * speed_ / 1000 : 0);
    }
    void anchor(int64_t media, int64_t now) { media_ = media; wall_ = now; }
    void running(bool value, int64_t now) { media_ = position(now); wall_ = now; running_ = value; }
    void speed(int milli, int64_t now) { media_ = position(now); wall_ = now; speed_ = std::clamp(milli, 500, 2000); }
};
}
