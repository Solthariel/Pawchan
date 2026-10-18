# Установка Pawchan

Этот архив содержит исходники, без готового APK. Инструкции сборки в Android Studio
на Windows — в [README](../README.md).
Имена APK ниже относятся к результату самостоятельной упаковки.

## Готовый APK

Публичный установочный файл называется `Pawchan-0.1.0.apk`. Это minified
release-сборка с application ID `com.solthariel.pawchan`. Обновление
через `-r` сохраняет локальные данные только при совпадении ключа подписи; при
конфликте с собственной сборкой сначала удалите установленное приложение.

Через Android Debug Bridge:

```text
adb install Pawchan-0.1.0.apk
```

На самом устройстве APK можно открыть из файлового менеджера и один раз разрешить
установку приложений из этого источника. Поддерживается Android 11 (API 30) и новее.

## Ключ release-подписи

Ключ не хранится в репозитории. Создайте его один раз вне каталога проекта:

```powershell
New-Item -ItemType Directory -Force "$HOME\.pawchan"
keytool -genkeypair -v -keystore "$HOME\.pawchan\release.jks" -alias pawchan `
    -keyalg RSA -keysize 4096 -validity 10000
```

Перед упаковкой задайте переменные только в текущей PowerShell-сессии:

```powershell
$env:PAWCHAN_KEYSTORE = "$HOME\.pawchan\release.jks"
$env:PAWCHAN_KEY_ALIAS = "pawchan"
$env:PAWCHAN_STORE_PASSWORD = Read-Host "Store password"
$env:PAWCHAN_KEY_PASSWORD = Read-Host "Key password"
$env:PAWCHAN_UPDATE_MANIFEST_URL = "https://HOST/update/publishing/manifest/data-v1.json"
```

Не добавляйте keystore и пароли в проект, Git или ZIP батча. Для обновления уже
установленного APK всегда используйте тот же ключ.

После упаковки удалите пароли из текущей сессии:

```powershell
Remove-Item Env:PAWCHAN_STORE_PASSWORD
Remove-Item Env:PAWCHAN_KEY_PASSWORD
Remove-Item Env:PAWCHAN_UPDATE_MANIFEST_URL
```

## Самостоятельная сборка пакета

PowerShell:

```powershell
.\scripts\package-release.ps1
```

Linux/macOS:

```sh
./scripts/package-release.sh
```

Сценарий сначала выполняет полный `local-check`, затем создаёт подписанный,
minified release APK, обновляет `update/publishing/manifest/data-v1.json` и
создаёт контрольную сумму APK в каталоге `dist`. Без четырёх переменных подписи
и `PAWCHAN_UPDATE_MANIFEST_URL` упаковка завершается до сборки. Адрес должен
быть абсолютным HTTPS URL и встраивается в публичный APK.

## Адрес обновлений

Для локальной сборки адрес может оставаться пустым. Публичная упаковка требует
его через `PAWCHAN_UPDATE_MANIFEST_URL`. При необходимости адрес можно передать
Gradle напрямую:

```text
-Ppawchan.updateManifestUrl=https://raw.githubusercontent.com/USER/Pawchan/main/update/publishing/manifest/data-v1.json
```

Обычная упаковка подготавливает размер и SHA-256, но оставляет `source` и
`changelog` равными `null`: несуществующие адреса не публикуются. После загрузки
подписанного APK и русского changelog выполните инструмент в режиме публикации:

```text
python update/publishing/tools/update_manifest.py dist/Pawchan-0.1.0.apk --publish --source https://HOST/Pawchan-0.1.0.apk --changelog https://HOST/1.txt
```

Режим `--publish` требует сразу обе абсолютные HTTPS-ссылки.
После публикации APK становится неизменяемым: повторная упаковка того же
`versionCode` проходит только при совпадении SHA-256 и размера. Перенос того же
файла на другой HTTPS-адрес разрешён, а изменённый APK требует нового
`versionCode`.
