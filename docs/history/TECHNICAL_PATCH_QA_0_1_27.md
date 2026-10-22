# Проверки технического патча 0.1.27

> Исторический снимок конкретного патча/проверки, не инструкция для текущего выпуска.
> Числа PASS/FAIL относятся только к указанной в отчёте базе и среде.
> Старые команды применения/публикации не выполнять; см. [карту действующих документов](../README.md).

## Уточнение после проверки CI

Записанный ниже PASS Detekt относился к локальному запуску без
`--build-upon-default-config`; он не подтверждал соответствие полному набору правил CI.
В CI публикация 0.1.28 остановилась на MaxLineLength. Исправление и повторная проверка
с точными флагами CI описаны в `LOCK_COMPOSER_PATCH_QA.md`. Это не изменение правил
и не новый результат Android LOCAL CHECK.

## Выполнено (исторический отчёт)

- 31 новый регрессионный JVM-тест: 6 streaming, 8 download store,
  10 HTTP transport и 7 ViewModel.
- Все 47 JVM-тестов :update в host-harness: PASS, 15 последовательных прогонов.
- 24 JVM-теста истории/метаданных (включая все 18 из двух реорганизованных классов): PASS.
- Итого 71 различный JVM-тест; повторы не считаются новыми тестами.
- Python unittest: 12 tests update/publishing/tests и 42 tests
  update/publishing/tools/tests: PASS (54 теста).
- Host Kotlin/Compose compilation изменённого :update и его тестов с -Werror: PASS.
- Detekt src/main + src/test всех Kotlin production-модулей: PASS после устранения
  двух существовавших превышений в тестовых классах. Конфигурация не смягчена.
- KtLint всего репозитория, включая build scripts: PASS.
- Неизменённые VerifySourcePolicy и VerifyReleaseMetadata исполнены в отдельном
  Gradle validation project на текущих исходниках: PASS. Это не полный qualityCheck.
- git diff --check: PASS.
- Сохранность количества существующих тестов и assert-вызовов при переносе fixtures: PASS.
- Проверки LF/CRLF применения, обратного check, повторного применения и ZIP integrity
  записаны в комплекте поставки.

## Команды

```sh
./scripts/local-check.sh
python3 -m unittest discover -s update/publishing/tests -v
python3 -m unittest discover -s update/publishing/tools/tests -v
```

Полный LOCAL CHECK запускался, но остановился на :app:assembleDebug:
SDK location not found. Android SDK/NDK и устройство в этой среде отсутствуют.
Проверки не отключались ради успешного отчёта.

Host-harness использует реальные Kotlin, Compose, OkHttp, coroutines, JUnit и код
:update. Для Android resources создан R;
Android framework представлен compile-time android.jar. В дополнительной компиляции
тестов метаданных единственная незадействованная здесь video session boundary заменена
фикстурой, EXIF и Coil взяты настоящие. Реальный PackageManager verifier, системная
установка, Android UI и native playback в host-тестах не исполнялись. Эти временные
компиляторные фикстуры и сторонние тестовые библиотеки в исходный ZIP не включены.

## Не выполнено / статус

- Полная Gradle debug/release сборка, Android Lint и qualityCheck не завершены.
- Нет Android runtime, установки и проверки APK/signing на устройстве.
- Нет release performance measurements; изменение не касается списка/текста/hot path,
  performance PASS не заявляется.
- Windows PowerShell installer не запускался в Windows; Git-патч проверяется
  на LF и CRLF worktree.

Статус поставки: **подготовлено для проверки, не полностью проверено**.
Версия остаётся 0.1.27; нет commit/push в пользовательский репозиторий и нет публикации.
