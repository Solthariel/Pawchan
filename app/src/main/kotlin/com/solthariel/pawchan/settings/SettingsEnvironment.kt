package com.solthariel.pawchan.settings

import androidx.compose.runtime.Composable
import com.solthariel.pawchan.domain.settings.AppSettings

/** Что экран знает помимо сохранённых настроек: кэш и панель обновлений. */
internal data class SettingsEnvironment(
    val cacheSize: String?,
    val onClearCache: () -> Unit,
    val updates: @Composable () -> Unit = {},
    val changelog: @Composable () -> Unit = { ChangelogSettings(showVersion = true) },
    val openProxy: () -> Unit = {},
)

@Composable
internal fun settingsContent(
    state: SettingsUiState,
    actions: SettingsActions,
    environment: SettingsEnvironment,
): Map<SettingsSection, List<SettingsGroup>> {
    val update = actions.onUpdate
    return linkedMapOf(
        SettingsSection.GENERAL to readingGroups(state.app, update) + generalNavigationGroups(state.app, update) +
            listOf(
                SettingsGroup(
                    "Безопасность",
                    listOf(
                        SettingsItem.Custom("Блокировка приложения") {
                            com.solthariel.pawchan.security
                                .AppLockSettings()
                        },
                    ),
                ),
            ),
        SettingsSection.INTERFACE to interfaceGroups(state, actions, update),
        SettingsSection.THREADS to
            threadGroups(state.app, update) +
            automationGroups(state.app, update),
        SettingsSection.MEDIA to mediaGroups(
            state.app,
            update,
        ) + mediaBehaviorGroups(state.app, update) + downloadGroups(state.app, update),
        SettingsSection.POSTING to postingGroups(state.app, update),
        SettingsSection.AUTOHIDE to
            listOf(
                SettingsGroup(
                    "Правила",
                    listOf(SettingsItem.Action("Правила автоскрытия", "Фразы и фрагменты по порядку")),
                ),
            ),
        SettingsSection.NETWORK to
            listOf(
                SettingsGroup("Прокси", listOf(SettingsItem.Action("Прокси", null, onClick = environment.openProxy))),
            ),
        SettingsSection.STORAGE to storageGroups(state.app, update, environment) +
            listOf(
                SettingsGroup(
                    "Резервные копии",
                    listOf(SettingsItem.Custom("Резервное копирование данных") { ClientBackupSettings() }),
                ),
                SettingsGroup(
                    "Статистика",
                    listOf(SettingsItem.Action("Статистика", null, onClick = actions.onStatistics)),
                ),
            ),
        SettingsSection.ABOUT to aboutGroups(environment),
    ).mapValues { (_, groups) ->
        groups.mapNotNull { group ->
            val items = group.items
            if (items.isEmpty()) null else group.copy(items = items)
        }
    }
}

@Composable
private fun interfaceGroups(
    state: SettingsUiState,
    actions: SettingsActions,
    update: ((AppSettings) -> AppSettings) -> Unit,
): List<SettingsGroup> {
    val app = state.app
    return listOf(
        themeSettingsGroup(state, actions),
        typographySettingsGroup(app, update),
        bottomNavigationSettingsGroup(app, update),
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
        ),
        hiddenThreadsSettingsGroup(app, update),
        SettingsGroup(
            "Шапка поста",
            listOf(
                SettingsItem.Toggle("Значки постов", "ОП, сажа, закреп, закрыт…", app.postMarkers) { on ->
                    update { it.copy(postMarkers = on) }
                },
                SettingsItem.Toggle("Показывать «Аноним»", null, app.showDefaultName) { on ->
                    update { it.copy(showDefaultName = on) }
                },
            ),
        ),
    )
}

private fun threadGroups(
    app: AppSettings,
    update: ((AppSettings) -> AppSettings) -> Unit,
): List<SettingsGroup> =
    listOf(
        SettingsGroup(
            "Обновление",
            listOf(
                SettingsItem.Toggle(
                    "Обновлять сохранённые копии",
                    "В фоне — раз в ~15 минут",
                    app.behavior.updateSavedCopies,
                ) { on ->
                    update { it.copy(behavior = it.behavior.copy(updateSavedCopies = on)) }
                },
            ),
        ),
    )

private fun mediaGroups(
    app: AppSettings,
    update: ((AppSettings) -> AppSettings) -> Unit,
): List<SettingsGroup> =
    listOf(
        SettingsGroup(
            "Просмотр",
            listOf(
                SettingsItem.Toggle(
                    "Видео без звука",
                    "Плеер запоминает выбор",
                    app.behavior.videoStartMuted,
                ) { on ->
                    update { it.copy(behavior = it.behavior.copy(videoStartMuted = on)) }
                },
            ),
        ),
    )

internal fun aboutGroups(environment: SettingsEnvironment): List<SettingsGroup> =
    listOf(
        SettingsGroup(
            "Приложение",
            listOf(
                SettingsItem.Custom("Версия", environment.changelog),
                SettingsItem.Custom("Проверить обновления", environment.updates),
            ),
        ),
        SettingsGroup(
            "Проект",
            listOf(
                SettingsItem.Custom("Релизы на GitHub") {
                    AboutLink("Релизы на GitHub", "Последний релиз", RELEASE_URL)
                },
            ),
        ),
        SettingsGroup(
            "Техническая информация",
            listOf(
                SettingsItem.Custom("Версии компонентов") { AboutVersions() },
                SettingsItem.Custom("Лицензии") { AboutLicenses() },
            ),
        ),
    )
