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
core/media, feature/composer и feature/thread. Наличие compiled AndroidTest APK
не означает исполнение тестов на устройстве. Целевые ручные сценарии выбираются
по изменённой подсистеме, замеры — по [протоколу performance](contracts/PERFORMANCE.md).

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
