#include "input_end.hpp"
#include <cassert>
#include <iostream>
int main() {
    paw::Decoder decoder; decoder.timeBase = AVRational{1, 1000};
    AVFrame* frame = av_frame_alloc();
    frame->width = 2; frame->height = 2; frame->format = AV_PIX_FMT_YUV420P; frame->duration = 40;
    assert(av_frame_get_buffer(frame, 32) == 0);
    paw::InputEnd end;
    assert(end.observe(decoder, frame, 12920000, true, 40000000));
    assert(end.shortened(49894) && end.duration() == 12960);
    assert(end.fallback() && end.fallback() != frame);
    end.reset(); assert(!end.fallback() && !end.shortened(49894));
    assert(end.observe(decoder, frame, 1000000, true, 2000000)); assert(end.fallback());
    assert(end.observe(decoder, frame, 2040000, true, 2000000)); assert(!end.fallback());
    assert(!end.shortened(2100) && !end.shortened(3000)); // ordinary rounding/padding
    frame->sample_rate = 48000; frame->nb_samples = 48000;
    assert(end.observe(decoder, frame, 8000000, false, 0));
    assert(end.duration() == 9000 && !end.shortened(9000)); // unequal track lengths
    assert(!end.shortened(0)); // unknown declared duration is not an early end
    end.reset();
    assert(end.observe(decoder, frame, INT64_MAX - 10, true, -1)); // no signed overflow
    assert(end.duration() > 0 && !end.shortened(49894));
    av_frame_free(&frame);
    std::cout << "InputEnd: fallback, reset, timestamps, padding, audio tail and overflow passed\n";
}
