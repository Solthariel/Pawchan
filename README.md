<img src="docs/screenshots/icon.png" width="112" align="right" alt="">

# Pawchan 0.1.53

Android-клиент для 2ch с чтением 4chan и Архивача. Поддерживаются Android 11 и новее, архитектура ARM64.
Код версии Android (`versionCode`): **54**.

[![Последний релиз](https://img.shields.io/github/v/release/Solthariel/Pawchan?label=%D1%80%D0%B5%D0%BB%D0%B8%D0%B7)](https://github.com/Solthariel/Pawchan/releases/latest)
[![Загрузки](https://img.shields.io/github/downloads/Solthariel/Pawchan/total?label=%D0%B7%D0%B0%D0%B3%D1%80%D1%83%D0%B7%D0%BA%D0%B8)](https://github.com/Solthariel/Pawchan/releases)
![Android 11+](https://img.shields.io/badge/Android-11%2B-3ddc84?logo=android&logoColor=white)
[![GPL-3.0](https://img.shields.io/badge/%D0%BB%D0%B8%D1%86%D0%B5%D0%BD%D0%B7%D0%B8%D1%8F-GPL--3.0-blue)](LICENSE)

<table>
  <tr>
    <td align="center"><img src="docs/screenshots/board.png" width="250" alt="Доска"><br><sub>Доска</sub></td>
    <td align="center"><img src="docs/screenshots/thread.png" width="250" alt="Тред"><br><sub>Тред</sub></td>
    <td align="center"><img src="docs/screenshots/video.png" width="250" alt="Видео"><br><sub>Видео на libmpv</sub></td>
  </tr>
  <tr>
    <td align="center"><img src="docs/screenshots/threads.png" width="250" alt="Треды"><br><sub>Треды и избранное</sub></td>
    <td align="center"><img src="docs/screenshots/composer.png" width="250" alt="Новый тред"><br><sub>Новый пост</sub></td>
    <td align="center"><img src="docs/screenshots/bottom-bar.png" width="250" alt="Порядок кнопок"><br><sub>Порядок кнопок панели</sub></td>
  </tr>
</table>

## В движении

<table>
  <tr>
    <td align="center"><img src="docs/screenshots/viewer.gif" width="250" alt="Просмотр файлов"><br><sub>Листание файлов и закрытие свайпом</sub></td>
    <td align="center"><img src="docs/screenshots/threadshot.gif" width="250" alt="Тредшот"><br><sub>Тредшот: выделил посты, сохранил картинку</sub></td>
    <td align="center"><img src="docs/screenshots/threadshot-example.png" width="250" alt="Готовый тредшот"><br><sub>Готовый тредшот</sub></td>
  </tr>
</table>

## Что умеет

- **2ch и 4chan**: доски, треды и поиск; на 2ch можно писать, 4chan и Архивач открываются для чтения, тред можно сохранить в Архивач.
- **Просмотр файлов**: листание свайпом, закрытие жестом, загрузка в выбранную папку.
- **Видео на libmpv**: скорость, аппаратное декодирование, картинка в картинке.
- **Тредшот**: выделенные посты сохраняются одной картинкой, пиксель в пиксель как на экране.
- **Раздел «Треды»**: открытые страницы, избранное, ответы, история, сохранённые копии и загрузки.
- **Нижняя панель** с настраиваемым порядком кнопок.
- **Автоскрытие** постов и тредов по своим правилам.
- **Сеть**: свои прокси SOCKS5, HTTP и HTTPS для каждого ресурса, управление куками.
- **Приватность**: блокировка PIN-кодом или биометрией, резервные копии с паролем.
- **Оформление**: системная, светлая, тёмная и AMOLED-тема, 10 языков интерфейса.
- **Обновления** прямо из приложения.

<details>
<summary>Ещё скриншоты</summary>
<br>
<table>
  <tr>
    <td align="center"><img src="docs/screenshots/boards.png" width="250" alt="Список досок"><br><sub>Список досок</sub></td>
    <td align="center"><img src="docs/screenshots/viewer.png" width="250" alt="Просмотрщик"><br><sub>Просмотрщик</sub></td>
    <td align="center"><img src="docs/screenshots/language.png" width="250" alt="Язык интерфейса"><br><sub>Язык интерфейса</sub></td>
  </tr>
</table>
</details>

## Установка

Подписанные APK публикуются в [релизах GitHub](https://github.com/Solthariel/Pawchan/releases).
Требования к установке и обновлению описаны в [руководстве](docs/INSTALL.md).

## Сборка из исходников

Откройте в Android Studio папку, в которой лежит `settings.gradle.kts`.
Для сборки нужны JDK 25, Android SDK Platform 37, NDK 29.0.14206865 и CMake 3.22.1.
[Среда разработки и команды проверки](docs/DEVELOPMENT.md).

## Документация

- [Что нового в приложении](app/src/main/assets/settings-changelog.txt)
- [Как работает автоскрытие](docs/AUTOHIDE.md)
- [Настройка прокси](PROXY-README-RU.txt)
- [Карта проекта и документация](docs/README.md)
- [Подготовка и публикация релиза](docs/RELEASE_PROCESS.md)
- [Как сняты скриншоты](scripts/readme-shots.ps1)
