# Pawchan

Android-клиент 2ch. Полный проект для Android Studio; готовый APK в архив не включён.

## Сборка

1. Распакуйте архив в новую папку и откройте её в Android Studio.
2. Дождитесь Gradle Sync. Через SDK Manager установите Android SDK Platform 37,
   NDK **29.0.14206865** и CMake **3.22.1**, если IDE запросит их.
3. Запустите приложение кнопкой Run. Поддерживаются Android 11 и новее, **arm64-v8a**.

Gradle использует JDK 25 согласно `gradle/gradle-daemon-jvm.properties`.
Готовые FFmpeg **8.1.3** и dav1d **1.5.4** включены в `video-ffmpeg/native-prebuilt`.
Для обычной сборки на Windows не нужны Bash, WSL, Ubuntu или Python.
Не удаляйте `gradle/wrapper` и `video-ffmpeg/native-prebuilt`: они нужны для сборки.

## Проверки

Windows:

```powershell
.\scripts\local-check.ps1
```

Linux/macOS:

```sh
./scripts/local-check.sh
```

Проверка на подключённом ARM64-устройстве: `scripts/device-smoke.ps1` или
`scripts/device-smoke.sh`. Сборка не заменяет проверку интерфейса на телефоне.

## Версии и изменения

Начальная версия — `0.1.0`. Номер сборки используется только внутри приложения. Правила SemVer и подготовка следующего
выпуска: [система выпусков](update/publishing/README.md).

Подготовка версии и фактический статус проверок:
[проверка выпуска](docs/RELEASE.md).

## Структура

- `app` — экраны приложения, навигация и связывание компонентов.
- `domain` — модели и бизнес-контракты.
- `data` — сеть, база данных, настройки и черновики.
- `core/ui`, `core/media` — общие элементы интерфейса и медиапросмотр.
- `feature/thread`, `feature/composer` — тред и отправка сообщений.
- `video-ffmpeg` — видеоплеер и native-зависимости.
- `update` — обновления приложения.
- `scripts` — сборка и проверки.

[Архитектура](docs/ARCHITECTURE.md) · [Проверки качества](docs/QUALITY.md) ·
[Установка и подпись](docs/INSTALL.md)

Исторические отчёты, логи и описания прошлых исправлений удалены.
Папки `.gradle`, `.kotlin`, `.idea`, `build` и `local.properties` создаются локально
инструментами разработки; в исходный ZIP они не включены.
