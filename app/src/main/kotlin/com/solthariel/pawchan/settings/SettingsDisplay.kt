package com.solthariel.pawchan.settings

import com.solthariel.pawchan.domain.settings.AppSettings

/** Content visibility first; the two text controls stay together below the switches. */
internal fun displaySettingsGroup(
    app: AppSettings,
    update: ((AppSettings) -> AppSettings) -> Unit,
): SettingsGroup =
    SettingsGroup(
        "Отображение",
        listOf(
            SettingsItem.Toggle(
                "Скрывать стикеры",
                "Текст постов остаётся",
                app.hideStickers,
            ) { on -> update { it.copy(hideStickers = on) } },
            SettingsItem.Toggle(
                "Скрывать ответы @monkey",
                "Только на Дваче. Вопрос автора и ссылки на посты остаются.",
                app.hideMonkeyReplies,
            ) { on -> update { it.copy(hideMonkeyReplies = on) } },
            SettingsItem.Toggle(
                "Выводить скрытые треды",
                "Показывать свёрнутые карточки в каталоге",
                app.behavior.showHiddenThreads,
            ) { on -> update { it.copy(behavior = it.behavior.copy(showHiddenThreads = on)) } },
            SettingsItem.Toggle(
                "Выводить скрытые посты",
                "Показывать свёрнутые карточки ответов, скрытых правилами автоскрытия",
                app.behavior.showHiddenPosts,
            ) { on -> update { it.copy(behavior = it.behavior.copy(showHiddenPosts = on)) } },
            SettingsItem.Scale(
                "Размер текста",
                app.textSize,
                AppSettings.MIN_TEXT_SIZE..AppSettings.MAX_TEXT_SIZE,
                "sp",
                preview = true,
            ) { size -> update { it.copy(textSize = size) } },
            SettingsItem.Scale(
                "Строк до сворачивания",
                app.collapseLines,
                AppSettings.MIN_COLLAPSE_LINES..AppSettings.NO_COLLAPSE,
                "строк",
                preview = false,
                endLabel = "Не сворачивать",
            ) { lines -> update { it.copy(collapseLines = lines) } },
        ),
    )
