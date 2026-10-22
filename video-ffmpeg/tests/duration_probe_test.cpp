// Standalone white-box timestamp tests, not part of the Android shared library.
#include "duration_probe.cpp"
#include <cassert>
#include <iostream>
int main() {
    AVFormatContext* format = avformat_alloc_context();
    AVStream* stream = avformat_new_stream(format, nullptr);
    assert(stream);
    stream->codecpar->codec_type = AVMEDIA_TYPE_VIDEO;
    stream->time_base = AVRational{1, 1000};
    format->duration = 49894000;
    assert(paw::uncertain(format));
    AVPacket* packet = av_packet_alloc();
    assert(packet);
    packet->stream_index = 0; packet->pts = 12920; packet->duration = 40;
    assert(paw::packetEnd(format, packet, 0) == 12960);
    assert(paw::packetEnd(format, packet, 2000000) == 10960);
    assert(paw::packetEnd(format, packet, -2000000) == 14960);
    packet->dts = 9000; packet->pts = AV_NOPTS_VALUE;
    assert(paw::packetEnd(format, packet, 0) == 9040);
    packet->dts = AV_NOPTS_VALUE;
    assert(paw::packetEnd(format, packet, 0) == 0);
    packet->pts = INT64_MAX - 1;
    assert(paw::packetEnd(format, packet, 0) == 0);
    packet->pts = 12920; stream->disposition = AV_DISPOSITION_ATTACHED_PIC;
    assert(paw::packetEnd(format, packet, 0) == 0);
    stream->disposition = 0; stream->duration = 49894;
    assert(!paw::uncertain(format));
    stream->duration = 12960;
    assert(paw::uncertain(format));
    packet->stream_index = -1;
    assert(paw::packetEnd(format, packet, 0) == 0);
    av_packet_free(&packet); avformat_free_context(format);
    std::cout << "DurationProbe timestamp origin, missing PTS, overflow, attached picture and eligibility passed\n";
}
