#pragma once
#include <stdint.h>
#include <vector>
struct ANativeWindow { int references = 1, width = 0, height = 0, stride = 0, posted = 0; std::vector<uint32_t> pixels; };
struct ANativeWindow_Buffer { int32_t width, height, stride, format; void* bits; uint32_t reserved[6]; };
#define WINDOW_FORMAT_RGBA_8888 1
inline void ANativeWindow_release(ANativeWindow* window) { if (--window->references == 0) delete window; }
inline int ANativeWindow_setBuffersGeometry(ANativeWindow* window, int32_t width, int32_t height, int32_t) {
    window->width = width; window->height = height; window->stride = width + 1;
    window->pixels.assign(window->stride * height, 0); return 0;
}
inline int ANativeWindow_lock(ANativeWindow* window, ANativeWindow_Buffer* target, void*) {
    target->width = window->width; target->height = window->height; target->stride = window->stride;
    target->bits = window->pixels.data(); return 0;
}
inline int ANativeWindow_unlockAndPost(ANativeWindow* window) { ++window->posted; return 0; }
