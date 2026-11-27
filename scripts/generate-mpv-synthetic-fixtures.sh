#!/usr/bin/env bash
# Synthetic patterns and tones only; no third-party video or audio. Run from any directory.
set -euo pipefail
root="$(cd "$(dirname "$0")/.." && pwd)"
out="$root/core/media/src/androidTest/assets/video"
mkdir -p "$out"
# ffmpeg/libx264, libx265, libvpx, and AAC/Vorbis encoders are required.
# Short clips for compatibility and an explicitly scoped device timing probe, not a release benchmark.
make_mp4() {
  local name="$1" dimensions="$2" rate="$3" encoder="$4" pixfmt="$5"
  shift 5
  ffmpeg -hide_banner -loglevel error -y -f lavfi -i "testsrc2=size=${dimensions}:rate=${rate}:duration=4" \
    -f lavfi -i "sine=frequency=440:sample_rate=48000:duration=4" \
    -map 0:v:0 -map 1:a:0 -map_metadata -1 -c:v "$encoder" -pix_fmt "$pixfmt" -threads 2 "$@" \
    -c:a aac -b:a 64k -t 4 -movflags +faststart "$out/$name"
}
make_mp4 synthetic-h264-1220x2712-fps24-17.mp4 1220x2712 145/6 libx264 yuv420p -preset ultrafast -crf 32
make_mp4 synthetic-mpeg4-704x416-fps37-457.mp4 704x416 37457/1000 mpeg4 yuv420p -q:v 4
make_mp4 synthetic-hevc-596x1280-fps42.mp4 596x1280 42 libx265 yuv420p -preset ultrafast -x265-params 'pools=none:frame-threads=2:log-level=error' -crf 32
make_mp4 synthetic-h264-810x1440-fps60.mp4 810x1440 60 libx264 yuv420p -preset ultrafast -crf 32
make_mp4 synthetic-h264-1080x1920-fps60.mp4 1080x1920 60 libx264 yuv420p -preset veryfast -crf 32
make_mp4 synthetic-h264-2160x3840-fps60.mp4 2160x3840 60 libx264 yuv420p -preset ultrafast -crf 34
make_mp4 synthetic-h264-2322x2160-fps60.mp4 2322x2160 60 libx264 yuv420p -preset ultrafast -crf 34
make_mp4 synthetic-h264-10bit-640x360-fps29-97.mp4 640x360 30000/1001 libx264 yuv420p10le -preset ultrafast -crf 32
ffmpeg -hide_banner -loglevel error -y -f lavfi -i 'testsrc2=size=640x360:rate=25:duration=4' \
  -f lavfi -i 'sine=frequency=440:sample_rate=48000:duration=5' \
  -filter:v "setpts='if(lt(N,50),N/(25*TB),(2+(N-50)/(12.5*TB)))'" \
  -fps_mode vfr -map 0:v:0 -map 1:a:0 -map_metadata -1 -c:v libvpx -deadline realtime -cpu-used 8 -threads 2 -b:v 500k \
  -c:a libvorbis -b:a 64k -t 4 "$out/synthetic-vp8-vfr-640x360.webm"

# Small, synthetic regression clips; padded timelines like those from a segment export.
ffmpeg -nostdin -hide_banner -loglevel error -y -f lavfi -i 'testsrc2=size=128x96:rate=15:duration=3' \
  -f lavfi -i 'sine=frequency=440:sample_rate=44100:duration=3' -map 0:v:0 -map 1:a:0 -map_metadata -1 \
  -c:v libx264 -threads 1 -bf 0 -pix_fmt yuv420p -crf 30 -c:a aac -b:a 32k \
  -output_ts_offset 500.315 -use_editlist 1 "$out/synthetic-common-gap.mp4"
thumbs="$root/app/src/androidTest/assets/downloads"
mkdir -p "$thumbs"
ffmpeg -nostdin -hide_banner -loglevel error -y -f lavfi -i 'color=c=0x36a4e2:size=160x96:rate=8:duration=1' \
  -map_metadata -1 -an -c:v libx264 -threads 1 -pix_fmt yuv420p -crf 30 \
  -output_ts_offset 640.31 -use_editlist 1 "$thumbs/offset-blue.mp4"
