# Лента обновлений приложения

## Данные и потребители

- Публичный источник по умолчанию:
  `https://raw.githubusercontent.com/Solthariel/Pawchan/master/update/feed/data-v1.json`.
- [update/feed](../update/feed/data-v1.json) — опубликованные данные, не шаблон для
  пересборки другого APK с тем же code.
- [update/publishing/manifest](../update/publishing/manifest/data-v1.json) — рабочие
  метаданные упаковки; версия и code определяются [реестром](../update/publishing/versions.json).
- Размер и SHA-256 берутся из настоящего подписанного файла. Адреса не подставляются
  фиктивно. История экрана описана в [CHANGELOG_HISTORY.md](CHANGELOG_HISTORY.md).
- Gradle property `pawchan.updateManifestUrl` и `PAWCHAN_UPDATE_MANIFEST_URL`
  позволяют задать другой источник. Публичный URL должен быть HTTPS.

## Публикация и восстановление

Команды, проверки автора, формат Actions, состав assets и таблица восстановления
находятся только в [RELEASE_PROCESS.md](RELEASE_PROCESS.md). Здесь нет второй
последовательности bump/add/push или альтернативного ручного запуска.

Релиз GitHub и Git-коммит ленты не являются одной транзакцией. Запись в ленте
может отставать после сбоя; это не повод удалять рабочий релиз или менять тег.
Исторические APK могут иметь только APK и SHA; для новых выпусков процесс требует
также отдельный плоский source ZIP и его SHA.

Старое описание одноразовой замены первого выпуска сохранено как
[исторический отчёт](history/UPDATE_FEED_FIRST_RELEASE.md), а не действующее разрешение.
