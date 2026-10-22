package com.solthariel.pawchan.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.solthariel.pawchan.BuildConfig
import com.solthariel.pawchan.R
import com.solthariel.pawchan.domain.settings.AppSettings
import com.solthariel.pawchan.domain.settings.ThemePreference

/** Что экран знает помимо сохранённых настроек: кэш и панель обновлений. */
internal data class SettingsEnvironment(
    val cacheSize: String?,
    val onClearCache: () -> Unit,
    val updates: @Composable () -> Unit = {},
    val changelog: @Composable () -> Unit = { ChangelogSettings() },
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
        SettingsSection.NETWORK to networkGroups(state.app, update),
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
    val themes = ThemePreference.entries
    return listOf(
        SettingsGroup(
            "Оформление",
            listOf(
                SettingsItem.Choice(
                    stringResource(R.string.theme_title),
                    themes.map { stringResource(it.labelResource) },
                    themes.indexOf(state.theme),
                ) { actions.onTheme(themes[it]) },
                SettingsItem.Choice(
                    "Оформление постов",
                    listOf("Без рамок", "Карточки"),
                    if (app.bubblePosts) 1 else 0,
                ) { selected -> update { it.copy(bubblePosts = selected == 1) } },
                SettingsItem.Toggle(
                    "Скрывать стикеры",
                    "Скрывать вложения стикеров, оставляя текст поста",
                    app.hideStickers,
                ) { on -> update { it.copy(hideStickers = on) } },
                SettingsItem.Scale(
                    "Размер текста",
                    app.textSize,
                    AppSettings.MIN_TEXT_SIZE..AppSettings.MAX_TEXT_SIZE,
                    "sp",
                    preview = true,
                ) { size -> update { it.copy(textSize = size) } },
                SettingsItem.Scale(
                    "Сворачивать длинные посты после",
                    app.collapseLines,
                    AppSettings.MIN_COLLAPSE_LINES..AppSettings.MAX_COLLAPSE_LINES,
                    "строк",
                    preview = false,
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
                SettingsItem.Toggle("Показывать «Аноним»", "Имя по умолчанию в шапке", app.showDefaultName) { on ->
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
                    "Новые посты добавляются, пока тред жив; в фоне — примерно раз в 15 минут",
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
                    "Кнопка звука в плеере запоминает выбор",
                    app.behavior.videoStartMuted,
                ) { on ->
                    update { it.copy(behavior = it.behavior.copy(videoStartMuted = on)) }
                },
            ),
        ),
    )

private fun networkGroups(
    app: AppSettings,
    update: ((AppSettings) -> AppSettings) -> Unit,
): List<SettingsGroup> =
    listOf(
        SettingsGroup("Vocaroo", vocarooDnsItems(app, update)),
        SettingsGroup("Архивач", arhivachDnsItems(app, update)),
        SettingsGroup("GitHub: обновления и список изменений", githubDnsItems(app, update)),
    )

private fun aboutGroups(environment: SettingsEnvironment): List<SettingsGroup> =
    listOf(
        SettingsGroup(
            "Приложение",
            listOf(
                SettingsItem.Action("Версия", BuildConfig.VERSION_NAME),
                SettingsItem.Custom("Список изменений", environment.changelog),
                SettingsItem.Custom("Обновления", environment.updates),
            ),
        ),
    )
