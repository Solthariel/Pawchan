# Видеоплеер: карта ресурсов и рисков

## Владельцы

- [video-ffmpeg](../../video-ffmpeg) — JNI-мост libmpv и прогрессивный источник.
  Native-вызовы mpv выполняются на собственном worker-потоке.
- [core/media](../../core/media) — viewer session, playback/recovery и PiP handoff.
  App предоставляет borrowed HTTP/loaders и право показа; viewer их не закрывает.
- Сеть остаётся у ProgressiveSource с borrowed OkHttpClient. URL/cookies/заголовки
  не передаются в native; mpv stream callback читает подтверждённые диапазоны session-owned файла.
- Сессия, переданная в PiP, принадлежит PiP, не закрывается viewer. Непринятый
  handoff освобождается по таймауту. Подробные обязательства —
  [контракт модулей](../contracts/MODULE_BOUNDARIES.md).

## Поведение, которое нельзя потерять при рефакторинге

Completion относится к поколению seek; устаревшее событие не останавливает новый
переход. Ошибка смены скорости не выдаёт отклонённую скорость за принятую.
Audio focus и noisy receiver принадлежат сессии при viewer/PiP/viewer; silent
video не запрашивает audio focus и не меняет сохранённый mute.

Блокировка приложения отзывает право PiP: поверхность отсоединяется, звук
останавливается, позиция сохраняется, task PiP закрывается. Immediate означает
немедленный отзыв при уходе основного Activity; ненулевая задержка разрешает
PiP лишь до deadline. Screen-off закрывает PiP. Это не основание ослаблять PIN.

## Что остаётся риском

Режим AUTO может использовать программный декодер; кнопка показывает фактический
режим, когда он известен. Плавность, расход энергии и совместимость зависят от устройства.

На устройстве проверять codecs/fallback, seek около EOF и при недокачанном файле,
повторные seek, pause/play, 0.5–2x, A/V sync, Bluetooth/audio focus, PiP и отзыв
доступа. JVM-модель AudioTrack не заменяет настоящее AudioTrack/MediaCodec.
Метрики — по [протоколу](../contracts/PERFORMANCE.md), команды —
[DEVELOPMENT.md](../DEVELOPMENT.md).

История реализации и прежние результаты: [замена слоя](../history/PLAYER_ENGINE_REPLACEMENT.md),
[runtime-коррекции](../history/PLAYER_RUNTIME_FIX.md). Лицензии и сведения о
происхождении библиотек остаются в исходниках без изменений.
