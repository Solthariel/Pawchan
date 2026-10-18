package com.solthariel.pawchan.settings

import androidx.compose.runtime.Composable
import com.solthariel.pawchan.domain.settings.AppSettings
import com.solthariel.pawchan.domain.settings.AttachmentDefaults
import com.solthariel.pawchan.domain.settings.ClientPreferences
import com.solthariel.pawchan.domain.settings.ComposerField
import com.solthariel.pawchan.domain.settings.UploadNameMode

internal fun AppSettings.withClient(transform: (ClientPreferences) -> ClientPreferences): AppSettings =
    copy(behavior = behavior.copy(client = transform(behavior.client)))

@Composable
internal fun postingGroups(
    app: AppSettings,
    update: ((AppSettings) -> AppSettings) -> Unit,
): List<SettingsGroup> {
    val preferences = app.behavior.client.composer
    val files = preferences.attachments
    val changeFiles: ((AttachmentDefaults) -> AttachmentDefaults) -> Unit = { transform ->
        update { current ->
            current.withClient { client ->
                client.copy(composer = client.composer.copy(attachments = transform(client.composer.attachments)))
            }
        }
    }
    return listOf(
        SettingsGroup(
            "Показывать поля формы",
            ComposerField.entries.map { field ->
                SettingsItem.Toggle(
                    field.fieldLabel(),
                    null,
                    field !in preferences.hiddenFields,
                ) { visible ->
                    update { current ->
                        current.withClient {
                            val fields = it.composer.hiddenFields
                            val selected = if (visible) fields - field else fields + field
                            it.copy(composer = it.composer.copy(hiddenFields = selected))
                        }
                    }
                }
            },
        ),
        SettingsGroup(
            "Новые вложения по умолчанию",
            listOf(
                SettingsItem.Toggle("Изменять хэш", null, files.uniqueHash) { on ->
                    changeFiles { it.copy(uniqueHash = on) }
                },
                SettingsItem.Toggle(
                    "Удалять метаданные",
                    "JPEG и PNG; ориентация сохраняется",
                    files.removeMetadata,
                ) { on ->
                    changeFiles {
                        it.copy(removeMetadata = on)
                    }
                },
                SettingsItem.Toggle("Спойлер", "Применяется при отправке на поддерживаемый сайт", files.spoiler) { on ->
                    changeFiles {
                        it.copy(spoiler = on)
                    }
                },
            ),
        ),
        uploadNamingGroup(files, changeFiles),
        imageEncodingGroup(files, changeFiles),
    )
}

private fun ComposerField.fieldLabel(): String =
    when (this) {
        ComposerField.NAME -> "Имя"
        ComposerField.EMAIL -> "Почта"
        ComposerField.SUBJECT -> "Тема нового треда"
        ComposerField.TAG -> "Тег нового треда"
    }

@Composable
private fun uploadNamingGroup(
    files: AttachmentDefaults,
    change: ((AttachmentDefaults) -> AttachmentDefaults) -> Unit,
): SettingsGroup =
    SettingsGroup(
        "Имя отправляемого файла",
        buildList {
            add(
                SettingsItem.Choice(
                    "Имя вложения",
                    listOf("Исходное", "Убрать исходное имя", "Случайное", "Своё"),
                    files.naming.mode.ordinal,
                ) { index ->
                    change { it.copy(naming = it.naming.copy(mode = UploadNameMode.entries[index])) }
                },
            )
            if (files.naming.mode == UploadNameMode.CUSTOM) {
                add(
                    SettingsItem.Custom("Своё имя файла") {
                        UploadNameSetting(
                            files.naming.customBase,
                        ) { name -> change { it.copy(naming = it.naming.copy(customBase = name)) } }
                    },
                )
            }
        },
    )
