#pragma once
#include "transport.hpp"
extern "C" {
#include <libavutil/time.h>
}
namespace paw {
// END means the call returned (including failure), not that it succeeded.
class TimingScope {
    Input& input_;
    int stage_;
    int64_t started_;
public:
    TimingScope(Input& input, int stage) : input_(input), stage_(stage),
        started_(input.tracing() ? av_gettime_relative() : 0) {}
    ~TimingScope() {
        if (input_.tracing()) input_.timing(stage_, av_gettime_relative() - started_);
    }
};
}
