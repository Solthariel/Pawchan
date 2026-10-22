# Проверки нового моста

1. Штатные Android unit tests:
   `./gradlew :video-ffmpeg:testDebugUnitTest`
2. `ProgressiveSourceHostTest.java`: отдельные host-интеграции с настоящим OkHttp
   и локальным HTTP-сервером (206, игнорирование Range, 416 и отмена).
3. `timeline_test.cpp`: монотонные часы, пауза, смена скорости, anchor.
4. `host/display_test.cpp`: реальные swscale/RGBA с тестовым ANativeWindow.
   Проверяет 0/90/180/270°, stride и сохранение кадра при новой поверхности.
5. `host/NativeDecodeHostTest.java`: настоящие FFmpeg 8.1.3, JNI с -Xcheck:jni,
   PublishedInput и decoder pump. Тестовые PcmOutput/ANativeWindow не являются
   Android runtime и не подтверждают AudioTrack, MediaCodec или настоящий PiP.

Host-набор собирается отдельным classpath: вместо production PcmOutput.java
подставляется host/PcmOutput.java. Эти файлы не входят в Android sourceSets.
Нужны JDK 17+, Android android.jar для типов, JUnit 4.13.2 / Hamcrest 1.3,
OkHttp 5.1.0 / Okio и Kotlin stdlib. Для C++ нужны хостовые FFmpeg 8.1.3 shared
libraries (avformat/avcodec/swresample/swscale/avutil), JNI headers и C++17.

NativeDecodeHostTest получает абсолютные пути:
`-Dpaw.hostLibrary=/absolute/path/libpawplayback.so`
`-Dpaw.fixtures=/absolute/path/fixtures`.

Fixtures: fast.mp4 (H.264/AAC, 96×64, 12 fps, 1.2 s, faststart), tail.mp4
(та же запись с moov в конце), sample.webm (VP9/Opus), sample.mkv (FFV1/PCM),
silent.mp4 (без аудио), rotated.mp4 (та же запись, display_rotation 90,
UTF-8 title «Привет 🐾»). Матрица поворота проверяется ffprobe до запуска теста;
одного тега rotate недостаточно в некоторых версиях FFmpeg CLI.

## Chunked A/V interleave regression

`host/InterleaveHostTest.java` exercises original problem files with real FFmpeg/JNI
and an optional finite realtime PCM model in `host/PcmOutput.java`. It verifies
continuous video progress, pause, paused preview, live seek and cancellation.
The model is NOT AudioTrack/MediaCodec and is never packaged in the APK.

Compile the host library with `frames.cpp` as well as the existing sources.
Pass `-Dpaw.problemFiles=/absolute/first.mp4:/absolute/second.mp4` (the platform
path separator) and run `org.junit.runner.JUnitCore
com.solthariel.pawchan.ffmpeg.InterleaveHostTest` with `-Xcheck:jni`.
Original problem files are external inputs, not repository fixtures or attachments.
A video-progress gap above 300 ms fails this desktop regression; this is not a
phone frame-time benchmark. The pre-fix engine must fail the same test.

`frames_test.cpp` verifies bounded demux lookahead when audio ends before video,
and clears prefetched packets/frames on seek. Generate its fixture with:

```sh
ffmpeg -f lavfi -i testsrc2=size=96x64:rate=30:duration=80 \
  -f lavfi -i sine=frequency=440:duration=0.5 \
  -c:v libx264 -preset ultrafast -crf 25 -c:a aac short-audio.mp4
```

Compile with `frames.cpp`, `decoder.cpp`, C++17 and host FFmpeg headers/libraries
(avformat, avcodec, swresample, avutil). Run `frames_test /absolute/short-audio.mp4`.
AddressSanitizer/UndefinedBehaviorSanitizer can be enabled on this standalone test.
