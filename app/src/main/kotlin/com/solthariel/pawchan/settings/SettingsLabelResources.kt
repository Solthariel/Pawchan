package com.solthariel.pawchan.settings

import android.content.res.Resources
import com.solthariel.pawchan.R

internal fun Resources.settingsLabel(label: String): String = settingLabels[label]?.let(::getString) ?: label

private val settingLabels =
    mapOf(
        "Sage по умолчанию" to R.string.ui_sage_by_default,
        "Автозапуск при перелистывании" to R.string.ui_autostart_when_swiping,
        "Автоскрытие" to R.string.ui_auto_hide,
        "Без мобильного интернета" to R.string.ui_no_mobile_data,
        "В 2 раза меньше" to R.string.ui_half_as_much,
        "В 4 раза меньше" to R.string.ui_a_quarter_as_much,
        "В 8 раз меньше" to R.string.ui_an_eighth_as_much,
        "В фоне — раз в ~15 минут" to R.string.ui_in_background_about_every_15,
        "Версии компонентов" to R.string.ui_component_versions,
        "Версия" to R.string.ui_version_be04,
        "Версия и обновления" to R.string.ui_version_and_updates,
        "Видео без звука" to R.string.ui_video_without_sound,
        "Включая отслеживаемые треды" to R.string.ui_including_watched_threads,
        "Выводить скрытые посты" to R.string.ui_show_hidden_posts,
        "Выводить скрытые треды" to R.string.ui_show_hidden_threads,
        "Галерея и видео" to R.string.ui_gallery_and_video,
        "Если поддерживается сайтом" to R.string.ui_if_supported_by_the_site,
        "Звук видео, перелистывание, галерея" to R.string.ui_video_sound_page_flipping_gallery,
        "Зеркало сайта" to R.string.ui_site_mirror,
        "Интервал обновления" to R.string.ui_refresh_interval,
        "Интервал проверки" to R.string.ui_check_interval,
        "Интерфейс" to R.string.ui_interface,
        "Использует дополнительный трафик" to R.string.ui_uses_additional_traffic,
        "Исходный" to R.string.ui_original_7f21,
        "Как в системе" to R.string.ui_system_default,
        "Качество JPEG" to R.string.ui_jpeg_quality,
        "Кэш" to R.string.ui_cache,
        "Легче" to R.string.ui_lighter,
        "Лимит и очистка кэша, резервные копии, статистика" to R.string.ui_cache_limit_and_clearing_backups,
        "Лимит кэша" to R.string.ui_cache_limit,
        "Лицензии" to R.string.ui_licenses,
        "Медиа" to R.string.ui_media,
        "Межстрочный интервал" to R.string.ui_line_spacing,
        "На отслеживаемые посты" to R.string.ui_watched_posts,
        "Насыщенность" to R.string.ui_saturation,
        "Не перекодировать" to R.string.ui_do_not_transcode,
        "Не при сворачивании" to R.string.ui_not_on_collapse,
        "Не сворачивать" to R.string.ui_do_not_collapse,
        "О программе" to R.string.ui_about,
        "Обновление" to R.string.ui_update,
        "Обновлять избранное" to R.string.ui_refresh_favorites,
        "Обновлять открытый тред" to R.string.ui_refresh_open_thread,
        "Обновлять сохранённые копии" to R.string.ui_refresh_saved_copies,
        "Общие" to R.string.ui_general,
        "Обычная" to R.string.ui_regular,
        "Обычный" to R.string.ui_normal,
        "Остановить" to R.string.ui_stop,
        "Ответы" to R.string.ui_replies,
        "Открытый тред" to R.string.ui_open_thread,
        "Открытый тред, избранное, ответы и уведомления" to R.string.ui_open_thread_favorites_replies_and,
        "Отображение" to R.string.ui_display,
        "Отправка" to R.string.ui_sending,
        "Оформление" to R.string.ui_appearance,
        "Очистить кэш" to R.string.ui_clear_cache,
        "Очищать кэш при выходе" to R.string.ui_clear_cache_on_exit,
        "Перейти к просмотренному посту" to R.string.ui_go_to_viewed_post,
        "Перекодирование изображений" to R.string.ui_image_transcoding,
        "Плеер запоминает выбор" to R.string.ui_the_player_remembers_the_selection,
        "Плотнее" to R.string.ui_denser,
        "Повторить" to R.string.ui_retry,
        "Подключение" to R.string.ui_connection,
        "Позиция треда после галереи" to R.string.ui_thread_position_after_gallery,
        "Показывать свёрнутые карточки в каталоге" to R.string.ui_show_collapsed_cards_in_catalog,
        "Показывать свёрнутые карточки ответов, скрытых правилами автоскрытия" to
            R.string.ui_show_collapsed_cards_of_replies,
        "Поля формы и обработка вложений по умолчанию" to R.string.ui_default_form_fields_and_attachment,
        "После окончания видео" to R.string.ui_after_video_ends,
        "Последний релиз" to R.string.ui_latest_release,
        "Правила скрытия постов и тредов" to R.string.ui_post_and_thread_hiding_rules,
        "Предзагружать следующее видео" to R.string.ui_preload_next_video,
        "Приложение" to R.string.ui_application,
        "Проверить обновления" to R.string.ui_check_for_updates,
        "Проверять ответы мне" to R.string.ui_check_my_replies,
        "Проверять по Wi-Fi" to R.string.ui_check_over_wi_fi,
        "Проект" to R.string.ui_project,
        "Прозрачность верхней панели" to R.string.ui_top_bar_opacity,
        "Прозрачность нижней панели" to R.string.ui_bottom_bar_opacity,
        "Прозрачность панелей" to R.string.ui_panel_opacity,
        "Прокси для форумов, Vocaroo и GitHub" to R.string.ui_proxy_for_forums_vocaroo_and,
        "Просмотр" to R.string.ui_view,
        "Размер изображения" to R.string.ui_image_size,
        "Размер текста" to R.string.ui_text_size,
        "Режим темы" to R.string.ui_theme_mode,
        "Релизы на GitHub" to R.string.ui_github_releases,
        "Светлый" to R.string.ui_light,
        "Сеть" to R.string.ui_network,
        "Системный" to R.string.ui_system,
        "Скрывать ответы @monkey" to R.string.ui_hide_monkey_replies,
        "Скрывать стикеры" to R.string.ui_hide_stickers,
        "Строк до сворачивания" to R.string.ui_lines_before_collapsing,
        "Текст постов остаётся" to R.string.ui_post_text_remains,
        "Тема, размер текста, значки постов" to R.string.ui_theme_text_size_post_icons,
        "Техническая информация" to R.string.ui_technical_information,
        "Только на Дваче. Вопрос автора и ссылки на посты остаются." to R.string.ui_only_on_the_author_s,
        "Треды и каталог" to R.string.ui_threads_and_catalog,
        "Тёмный" to R.string.ui_dark_masculine,
        "Уведомления об ответах" to R.string.ui_reply_notifications,
        "Увеличенный" to R.string.ui_enlarged,
        "Фоновая проверка" to R.string.ui_background_check,
        "Формат" to R.string.ui_format,
        "Хранилище" to R.string.ui_storage,
        "Цветовая палитра" to R.string.ui_color_palette,
        "Чтение, история и блокировка приложения" to R.string.ui_reading_history_and_app_lock,
        "Шрифт" to R.string.ui_font,
        "Шрифт приложения" to R.string.ui_app_font,
    )

internal fun SettingsItem.localized(resources: Resources): SettingsItem {
    val name = resources.settingsLabel(title)
    return when (this) {
        is SettingsItem.Toggle -> copy(title = name, summary = summary?.let(resources::settingsLabel))
        is SettingsItem.Choice -> copy(title = name, options = options.map(resources::settingsLabel))
        is SettingsItem.Scale -> copy(title = name, endLabel = endLabel?.let(resources::settingsLabel))
        is SettingsItem.Action -> copy(title = name)
        is SettingsItem.Custom -> copy(title = name)
    }
}

internal fun List<SettingsGroup>.localized(resources: Resources): List<SettingsGroup> =
    filter { it.items.isNotEmpty() }.map { group ->
        group.copy(
            title = resources.settingsLabel(group.title),
            items = group.items.map { it.localized(resources) },
        )
    }
