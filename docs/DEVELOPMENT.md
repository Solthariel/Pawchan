# Работа с проектом и проверки

Сборочная среда: JDK 25 (gradle/gradle-daemon-jvm.properties), SDK Platform 37,
NDK 29.0.14206865 и CMake 3.22.1. Приложение рассчитано на Android 11+ и ARM64.
Краткое открытие проекта — в [README](../README.md); обязательные правила —
[AGENTS.md](../AGENTS.md). Все команды ниже выполняются из корня проекта.

## Полная локальная проверка

Windows PowerShell:

```powershell
.\scripts\local-check.ps1
```

Linux/macOS:

```sh
./scripts/local-check.sh
```

Скрипт останавливает daemon, собирает debug/release и запускает qualityCheck.
Только завершение всей цепочки даёт `LOCAL CHECK: PASS`.
Состав и ограничения проверок — в [QUALITY.md](QUALITY.md).

## Проверка на устройстве

Подключить ARM64-устройство с разрешённой ADB-отладкой либо совместимый эмулятор:

```powershell
.\scripts\device-smoke.ps1
```

```sh
./scripts/device-smoke.sh
```

Скрипт запускает connectedDebugAndroidTest для app, data/database, core/ui,
core/media, feature/composer, feature/thread и feature/applock. Наличие compiled AndroidTest APK
не означает исполнение тестов на устройстве. Целевые ручные сценарии выбираются
по изменённой подсистеме, замеры — по [протоколу performance](contracts/PERFORMANCE.md).

## Изоляция приложения при device-тестах

Debug устанавливается как **Pawchan Debug**: `com.solthariel.pawchan.debug`,
тестовый APK — `com.solthariel.pawchan.debug.test`. Release сохраняет прежний
`com.solthariel.pawchan` и обновляет установленный Pawchan с прежним ключом подписи.
У debug отдельные настройки, БД, файлы и PIN; данные release туда не переносятся.

Перед установкой debug или connectedDebugAndroidTest задача
`:app:verifyDebugPackageIsolation` проверяет реальные merged-манифесты: оба
package, instrumentation target и authorities провайдеров. Если они пересекаются
с release, выполнение останавливается до установки тестовым раннером.
Нельзя обходить эту задачу через `-x`, вручную менять applicationId или запускать
старые AndroidTest APK с прежним target. Local `assembleRelease` с debug-подписью
по-прежнему НЕ предназначен для установки поверх официального релиза.

На основном телефоне не запускайте `adb uninstall com.solthariel.pawchan` и не
удаляйте обычный Pawchan ради тестов. Для первого прогона новой изоляции предпочтительно
тестовое устройство. Отдельный package защищает данные release, но тестовый раннер
может очищать или удалять сам **Pawchan Debug**. Для каждого прогона проверяйте
XML-отчёт: нужные классы, число тестов, failures/errors/skipped — одной строки
BUILD SUCCESSFUL недостаточно.

## Быстрая обратная связь

Для треда на Windows есть `scripts/check-thread.ps1`: KtLint, thread unit tests
и thread Detekt. Это не полный LOCAL CHECK. Форматирование — отдельная
изменяющая исходники операция `scripts/format-kotlin.ps1` / `.sh`; проверка
качества не должна незаметно форматировать или подавлять диагностику.

Для документационных изменений проверить ссылки, сохранность перенесённых
требований и отсутствие изменений кода/версии. Одни проверки документации не
дают полного LOCAL CHECK. Нет SDK или устройства — указать ограничение честно.

## Выпуск

[RELEASE.md](RELEASE.md) — чек-лист готовности;
[RELEASE_PROCESS.md](RELEASE_PROCESS.md) — единственная инструкция публикации
и восстановления. Не копируйте команды выпуска в описание каждого мелкого патча.
Документационный коммит сам по себе не требует новой версии APK.

## Пересборка FFmpeg/dav1d и тесты её инструментов

Обычная Android-сборка по-прежнему проверяет и использует `video-ffmpeg/native-prebuilt`.
Для неё не нужны Meson, Ninja, Python или пересборка FFmpeg. Не удаляйте готовые
библиотеки из проекта.

Подготовка исходников и пересборка для сопровождающего реализованы отдельным
Kotlin/JVM-приложением `tools/native-build`. У него собственный Gradle build,
не применяющий Android-плагины. Используется проектный полный JDK 25 или новее, включая `javac`, а не только JRE.
Тесты и статические проверки запускаются без Android SDK, NDK и скачивания
исходников FFmpeg/dav1d; при первом запуске нужны зависимости Gradle из Maven.

```sh
bash gradlew -p tools/native-build check --warning-mode=fail --console=plain
```

Эта проверка включена в корневой `qualityCheck` через `nativeBuildToolCheck`.
Существующий `Publish signed release` выполняет её через штатную цепочку
`package-release.sh` → `local-check.sh` → `qualityCheck`. Отдельный workflow
не добавляется; правила и команды выпуска остаются в [RELEASE_PROCESS.md](RELEASE_PROCESS.md).
Локальный успех не считается выполнением публикации или GitHub Actions.

Команды из корня проекта:

```sh
# План, без скачивания исходников и запуска компиляторов.
bash gradlew -p tools/native-build planNative --warning-mode=fail

# Подготовка и проверка исходников, без компиляции.
bash gradlew -p tools/native-build prepareNativeSources --warning-mode=fail

# Пересборка: Linux x86_64, NDK 29.0.14206865, Meson, Ninja, make, pkg-config.
# Git нужен только для резервного получения FFmpeg при недоступности архива.
bash gradlew -p tools/native-build rebuildNative \
  -Pnative.ndk=/path/to/android-ndk-r29 -Pnative.abis=arm64-v8a \
  -Pnative.jobs=4 --warning-mode=fail

# Проверка уже полученного bundle.
bash gradlew -p tools/native-build verifyNativeBundle --warning-mode=fail
```

Результат: `video-ffmpeg/build/native-rebuild/bundle`, с собственным manifest,
хешами, лицензиями и отпечатком входов. Рабочие деревья и журналы находятся рядом.
Это НЕ `video-ffmpeg/build/native`, которым управляет обычная Gradle-сборка.
Новый инструмент никогда автоматически не заменяет `native-prebuilt`.
Для обновления поставляемых библиотек после реальной пересборки отдельно
проверяют новый bundle, заменяют комплект вместе с его manifest, затем выполняют
полный LOCAL CHECK, device smoke и проверки плеера на устройстве.

Поддерживаются ABI `arm64-v8a`, `armeabi-v7a`, `x86`, `x86_64`; несколько значений
передаются через запятую. Это возможности инструмента пересборки, а не расширение
поддержки APK: обычный проект по-прежнему ограничен ARM64.

Дополнительные свойства:

- `-Pnative.cache=/path/to/cache` — кеш архивов по SHA-256.
- `-Pnative.offline=true` — не скачивать исходники и не обращаться к Git;
  использовать проверенный кеш/подготовленное дерево либо явно переданные исходники.
  Это не заменяет Gradle `--offline` для зависимостей самого инструмента.
- `-Pnative.ffmpegSource=/path/to/ffmpeg` и `-Pnative.dav1dSource=/path/to/dav1d` —
  предоставленные деревья. Вложенные `.git` исключаются; изменения содержимого
  инвалидируют подготовленную копию. Такие деревья не выдаются за проверенные
  официальные архивы: в bundle записываются фактические хеши деревьев.
- Альтернативно принимаются `ANDROID_NDK_HOME`, `ANDROID_HOME`/`ANDROID_SDK_ROOT`,
  `NATIVE_ABIS`, `EXTERNAL_CORES`, `PAWCHAN_NATIVE_CACHE`,
  `PAWCHAN_FFMPEG_SOURCE_DIR`, `PAWCHAN_DAV1D_SOURCE_DIR`.
  Старые `WEBM_ARCHIVE_CACHE_DIR` и `NEKOCHAN_*_SOURCE_DIR` оставлены как алиасы.
- `bash video-ffmpeg/scripts/build-native.sh ["arm64-v8a x86_64"]` — тонкая
  совместимая точка входа. Старые `shared-prepare.sh`/`shared-build.sh` удалены;
  прямой вызов их прежнего позиционного API больше не поддерживается.

Версии и контрольные суммы заданы в `tools/native-build/native-dependencies.json`.
Пути с пробелами передаются отдельными аргументами процесса, без интерпретации
shell. Внешние/внутренние символьные ссылки и специальные файлы в предоставленных
деревьях и архивах не принимаются; используйте обычные файлы. Это намеренное
ограничение безопасной подготовки. NDK-путь с одиночной кавычкой также отклоняется
из-за формата Meson cross-file.

Проверяются хеши всех файлов bundle, ABI/class ELF, сегменты загрузки и выравнивание
не менее 16 KiB. Это не проверка Android loader, полного набора DT_NEEDED, всех
кодеков или воспроизведения. Тесты конвейера с имитацией компиляторов и тесты
настоящих JVM-процессов явно разделены; они не заменяют NDK-пересборку и телефон.

Происхождение прежних скриптов и сохранённого MOV-исправления описано в
[NOTICE инструмента](../tools/native-build/NOTICE.md). Исторические хеши в старом
`native-prebuilt/manifest.json` не переписываются под новый инструмент.
