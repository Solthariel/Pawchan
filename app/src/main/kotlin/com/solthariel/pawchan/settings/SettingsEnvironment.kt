package com.solthariel.pawchan.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalResources
import com.solthariel.pawchan.domain.settings.AppSettings
import com.solthariel.pawchan.R.string as L

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
    val res = LocalResources.current

    val update = actions.onUpdate
    return linkedMapOf(
        SettingsSection.GENERAL to readingGroups(state.app, update) + generalNavigationGroups(state.app, update) +
            listOf(
                SettingsGroup(
                    res.getString(L.ui_security),
                    listOf(
                        SettingsItem.Custom(res.getString(L.ui_app_lock)) {
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
                    res.getString(L.ui_rules),
                    listOf(
                        SettingsItem.Action(
                            res.getString(L.ui_auto_hide_rules),
                            res.getString(L.ui_phrases_and_fragments_in_order),
                        ),
                    ),
                ),
            ),
        SettingsSection.NETWORK to
            listOf(
                SettingsGroup(
                    res.getString(L.ui_proxy),
                    listOf(SettingsItem.Action(res.getString(L.ui_proxy), null, onClick = environment.openProxy)),
                ),
            ),
        SettingsSection.STORAGE to storageGroups(state.app, update, environment) +
            listOf(
                SettingsGroup(
                    res.getString(L.ui_backups),
                    listOf(SettingsItem.Custom(res.getString(L.ui_data_backup)) { ClientBackupSettings() }),
                ),
                SettingsGroup(
                    res.getString(L.ui_statistics),
                    listOf(
                        SettingsItem.Action(res.getString(L.ui_statistics), null, onClick = actions.onStatistics),
                    ),
                ),
            ),
        SettingsSection.ABOUT to aboutGroups(environment),
    ).mapValues { (_, groups) -> groups.localized(res) }
}

@Composable
private fun interfaceGroups(
    state: SettingsUiState,
    actions: SettingsActions,
    update: ((AppSettings) -> AppSettings) -> Unit,
): List<SettingsGroup> {
    val res = LocalResources.current

    val app = state.app
    return listOf(
        interfaceLanguageGroup(),
        themeSettingsGroup(state, actions),
        typographySettingsGroup(app, update),
        bottomNavigationSettingsGroup(app, update),
        bottomBarOrderGroup(app, update),
        keyboardSettingsGroup(app, update),
        displaySettingsGroup(app, update),
        SettingsGroup(
            res.getString(L.ui_post_header),
            listOf(
                SettingsItem.Toggle(
                    res.getString(L.ui_post_badges),
                    res.getString(L.ui_op_sage_sticky_closed),
                    app.postMarkers,
                ) { on ->
                    update { it.copy(postMarkers = on) }
                },
                SettingsItem.Toggle(res.getString(L.ui_show_anonymous), null, app.showDefaultName) { on ->
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
