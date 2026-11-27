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
    <td align="center" width="50%"><img src="docs/screenshots/reading.webp" width="300" alt="Чтение"><br><sub>Доска, поиск, тред и его статистика, видео и картинки, превью ответа</sub></td>
    <td align="center" width="50%"><img src="docs/screenshots/gallery.webp" width="300" alt="Галерея треда"><br><sub>Галерея треда: фильтры и выделение файлов</sub></td>
  </tr>
  <tr>
    <td align="center"><img src="docs/screenshots/editor.webp" width="300" alt="Новый пост и редактор"><br><sub>Новый пост с файлами и редактор картинок</sub></td>
    <td align="center"><img src="docs/screenshots/sections.webp" width="300" alt="Разделы и настройки"><br><sub>Треды, доски 2ch и 4chan, настройки</sub></td>
  </tr>
</table>

## Что умеет

- **2ch и 4chan**: доски, треды и поиск; на 2ch можно писать, 4chan и Архивач открываются для чтения, тред можно сохранить в Архивач.
- **Просмотр файлов**: листание свайпом, закрытие жестом, загрузка в выбранную папку.
- **Видео на libmpv**: скорость, аппаратное декодирование, картинка в картинке.
- **Тредшот**: выделенные посты сохраняются одной картинкой, пиксель в пиксель как на экране.
- **Новый пост**: файлы из системного выбора или вставкой из буфера обмена, встроенный редактор картинок: кадрирование, поворот, рисование и замазка.
- **Статистика треда**: посты, файлы, их общий размер и число участников.
- **Раздел «Треды»**: открытые страницы, избранное, ответы, история, сохранённые копии и загрузки.
- **Нижняя панель** с настраиваемым порядком кнопок.
- **Автоскрытие** постов и тредов по своим правилам.
- **Сеть**: свои прокси SOCKS5, HTTP и HTTPS для каждого ресурса, управление куками.
- **Приватность**: блокировка PIN-кодом или биометрией, резервные копии с паролем.
- **Оформление**: системная, светлая, тёмная и AMOLED-тема, 10 языков интерфейса.
- **Обновления** прямо из приложения.

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
