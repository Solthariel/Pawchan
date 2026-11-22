# Проверки видеоплеера

Основной движок — mpv. Его Android-тесты находятся в `core/media/src/androidTest`:
`MpvVideoEngineRuntimeTest`, `MpvSyntheticFixturesRuntimeTest`,
`MpvSessionRestartRuntimeTest`, `MpvSurfaceHandoffRuntimeTest`,
`MpvSurfaceReferencesRuntimeTest` и `VideoSessionReliabilityRuntimeTest`.
Они требуют подключённого устройства arm64 на Android 11 или новее:

```sh
./gradlew :core:media:connectedDebugAndroidTest
./gradlew :video-ffmpeg:testDebugUnitTest
```

`ProgressiveSourceCleanupTest.kt` проверяет, что закрытие источника не ждёт
зависший HTTP-поток, что живой writer не теряет файл раньше времени и что
временный файл удаляется после остановки загрузчиков и освобождения native reader.
`ProgressiveSourceHostTest.java` — дополнительные host-интеграции с OkHttp и
локальным HTTP-сервером для прогрессивной загрузки. Старые host/JNI-тесты
PawPlayback удалены вместе с их реализацией; они не проверяют mpv. Исполнение
синтетических тестов описано в `MPV-SYNTHETIC-TESTS-RU.txt`.
Телефонная проверка не заменяется JVM-тестами или одной лишь сборкой APK.

После изменения плеера на реальном arm64-устройстве проверьте:

- Быстрые переходы между соседними видео и возвращение назад; не должно быть
  пауз из-за ожидания завершения старой HTTP-загрузки.
- 20+ переключений между большими файлами: временные `neko-ffmpeg-*.media`
  исчезают после закрытия mpv, память не растёт без ограничений.
- Перемотка у верхнего/нижнего края 48dp строки, с Accessibility/TalkBack.
- PiP → возврат в просмотрщик с сохранением позиции и звукового состояния.
- Работа на устройстве с `ActivityManager.isLowRamDevice = true`: без
  опережающей загрузки следующего видео.
- Видео H.264 / HEVC / VP8, локальное и медленная/прерванная сеть;
  проверить HW/SW переключение и метаданные после начала воспроизведения.

Это протокол проверки, а не подтверждение её выполнения.
