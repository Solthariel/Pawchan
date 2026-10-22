#include "display.hpp"
#include <cassert>
#include <cstring>
static void rotation(int angle, const std::vector<uint32_t>& expected, int width, int height) {
    auto* first = new ANativeWindow(); ++first->references;
    auto* second = new ANativeWindow(); ++second->references;
    {
        paw::Display output;
        AVFrame* frame = av_frame_alloc();
        frame->width = 3; frame->height = 2; frame->format = AV_PIX_FMT_RGBA;
        assert(av_frame_get_buffer(frame, 16) == 0);
        const uint32_t pixels[] = {0xff000001,0xff000002,0xff000003,0xff000004,0xff000005,0xff000006};
        std::memcpy(frame->data[0], pixels, 12); std::memcpy(frame->data[0] + frame->linesize[0], pixels + 3, 12);
        output.offer(first); assert(output.update(frame, angle)); assert(output.present());
        assert(first->width == width && first->height == height && first->posted == 1);
        for (int y=0; y<height; ++y) for (int x=0; x<width; ++x)
            assert(first->pixels[y * first->stride+x] == expected[y*width+x]);
        // New surface while paused: retained pixels, no seek and no second decode.
        output.offer(second); assert(output.present());
        assert(first->references == 1 && second->posted == 1);
        for (int y=0; y<height; ++y) for (int x=0; x<width; ++x)
            assert(second->pixels[y * second->stride+x] == expected[y*width+x]);
        output.offer(nullptr); assert(output.present()); assert(second->references == 1);
        av_frame_free(&frame);
    }
    ANativeWindow_release(first); ANativeWindow_release(second);
}
int main() {
    rotation(0,{0xff000001,0xff000002,0xff000003,0xff000004,0xff000005,0xff000006},3,2);
    rotation(90,{0xff000004,0xff000001,0xff000005,0xff000002,0xff000006,0xff000003},2,3);
    rotation(180,{0xff000006,0xff000005,0xff000004,0xff000003,0xff000002,0xff000001},3,2);
    rotation(270,{0xff000003,0xff000006,0xff000002,0xff000005,0xff000001,0xff000004},2,3);
}
