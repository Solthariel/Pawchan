package com.solthariel.pawchan.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalResources
import com.solthariel.pawchan.domain.settings.AppSettings
import com.solthariel.pawchan.domain.settings.AttachmentDefaults
import com.solthariel.pawchan.domain.settings.ClientPreferences
import com.solthariel.pawchan.domain.settings.ComposerField
import com.solthariel.pawchan.domain.settings.UploadNameMode
import com.solthariel.pawchan.R.string as L

internal fun AppSettings.withClient(transform: (ClientPreferences) -> ClientPreferences): AppSettings =
    copy(behavior = behavior.copy(client = transform(behavior.client)))

@Composable
internal fun postingGroups(
    app: AppSettings,
    update: ((AppSettings) -> AppSettings) -> Unit,
): List<SettingsGroup> {
    val res = LocalResources.current

    val preferences = app.behavior.client.composer
    val files = preferences.attachments
    val changeFiles = attachmentChange(update)
    return listOf(
        SettingsGroup(
            res.getString(L.ui_show_form_fields),
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
        sageDefaultsGroup(preferences.defaultSage, update),
        SettingsGroup(
            res.getString(L.ui_new_attachments_by_default),
            listOf(
                SettingsItem.Toggle(res.getString(L.ui_change_hash), null, files.uniqueHash) { on ->
                    changeFiles { it.copy(uniqueHash = on) }
                },
                SettingsItem.Toggle(
                    res.getString(L.ui_delete_metadata),
                    res.getString(L.ui_jpeg_png_without_rotating_the),
                    files.removeMetadata,
                ) { on ->
                    changeFiles {
                        it.copy(removeMetadata = on)
                    }
                },
                SettingsItem.Toggle(
                    res.getString(L.ui_spoiler),
                    res.getString(L.ui_if_supported_by_the_site),
                    files.spoiler,
                ) { on ->
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

private fun sageDefaultsGroup(
    enabled: Boolean,
    update: ((AppSettings) -> AppSettings) -> Unit,
): SettingsGroup =
    SettingsGroup(
        "Ответы",
        listOf(
            SettingsItem.Toggle("Sage по умолчанию", "Если поддерживается сайтом", enabled) { on ->
                update { current ->
                    current.withClient { it.copy(composer = it.composer.copy(defaultSage = on)) }
                }
            },
        ),
    )

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
    run {
        val res = LocalResources.current
        SettingsGroup(
            res.getString(L.ui_name_of_file_being_sent),
            buildList {
                add(
                    SettingsItem.Choice(
                        res.getString(L.ui_attachment_name),
                        listOf(
                            res.getString(L.ui_original),
                            res.getString(L.ui_remove_original_name),
                            res.getString(L.ui_random),
                            res.getString(L.ui_custom),
                        ),
                        files.naming.mode.ordinal,
                    ) { index ->
                        change { it.copy(naming = it.naming.copy(mode = UploadNameMode.entries[index])) }
                    },
                )
                if (files.naming.mode == UploadNameMode.CUSTOM) {
                    add(
                        SettingsItem.Custom(res.getString(L.ui_custom_filename)) {
                            UploadNameSetting(
                                files.naming.customBase,
                            ) { name -> change { it.copy(naming = it.naming.copy(customBase = name)) } }
                        },
                    )
                }
            },
        )
    }

private typealias AttachmentChange = ((AttachmentDefaults) -> AttachmentDefaults) -> Unit

private fun attachmentChange(update: ((AppSettings) -> AppSettings) -> Unit): AttachmentChange =
    { transform ->
        update { current ->
            current.withClient { client ->
                client.copy(composer = client.composer.copy(attachments = transform(client.composer.attachments)))
            }
        }
    }
