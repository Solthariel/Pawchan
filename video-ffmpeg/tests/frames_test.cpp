#include "frames.hpp"
#include <cassert>
#include <iostream>
int main(int argc, char** argv) {
    assert(argc == 2);
    AVFormatContext* format = nullptr;
    assert(avformat_open_input(&format, argv[1], nullptr, nullptr) == 0);
    assert(avformat_find_stream_info(format, nullptr) >= 0);
    {
        paw::Decoder video, sound;
        assert(video.open(format, AVMEDIA_TYPE_VIDEO, true));
        assert(sound.open(format, AVMEDIA_TYPE_AUDIO, true));
        paw::Frames frames(format, video, sound);
        int pictures = 0, sounds = 0, yields = 0;
        for (int i = 0; i < 20000; ++i) {
            int v = frames.fill(true), a = frames.fill(false);
            assert(v >= 0 && a >= 0);
            if (!v && !a) break;
            yields += v == 2 || a == 2;
            bool selected = v == 1;
            if (v == 1 && a == 1) {
                selected = video.timestamp(frames.get(true), 0) <= sound.timestamp(frames.get(false), 0) - 100000;
            }
            assert(v == 1 || a == 1);
            if (selected) ++pictures; else ++sounds;
            frames.consume(selected);
        }
        assert(pictures == 2400 && sounds > 0 && yields > 0);
        std::cout << "Silent tail: " << pictures << " video frames, " << sounds << " audio, " << yields << " bounded yields\n";
        // Reset after seek must discard prefetched packets and pending frames.
        frames.reset();
        assert(avformat_seek_file(format, -1, INT64_MIN, 0, 0, AVSEEK_FLAG_BACKWARD) >= 0);
        avcodec_flush_buffers(video.context); avcodec_flush_buffers(sound.context);
        assert(frames.fill(true) == 1);
        assert(video.timestamp(frames.get(true), -1) == 0);
    }
    avformat_close_input(&format);
}
