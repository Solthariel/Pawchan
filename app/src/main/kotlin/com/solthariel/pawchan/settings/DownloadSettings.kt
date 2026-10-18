package com.solthariel.pawchan.settings

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.domain.settings.AppSettings
import com.solthariel.pawchan.domain.settings.DownloadNaming
import com.solthariel.pawchan.domain.settings.ImagePreload

@Composable
internal fun downloadGroups(
    app: AppSettings,
    update: ((AppSettings) -> AppSettings) -> Unit,
): List<SettingsGroup> {
    val value = app.behavior.client
    return listOf(
        SettingsGroup(
            "Скачивание и предзагрузка",
            listOf(
                SettingsItem.Choice(
                    "Предзагрузка ближайших изображений",
                    listOf("Выключена", "Только Wi-Fi", "Любая сеть"),
                    value.media.preloadImages.ordinal,
                ) { index ->
                    update { current ->
                        current.withClient { client ->
                            client.copy(media = client.media.copy(preloadImages = ImagePreload.entries[index]))
                        }
                    }
                },
                SettingsItem.Choice(
                    "Имя скачиваемого файла",
                    listOf("Исходное", "Подробное: сайт, доска, тред, пост и имя"),
                    value.downloads.naming.ordinal,
                ) { index ->
                    update { current ->
                        current.withClient { client ->
                            client.copy(downloads = client.downloads.copy(naming = DownloadNaming.entries[index]))
                        }
                    }
                },
                SettingsItem.Custom("Папка загрузок") { DownloadDirectory(app, update) },
            ),
        ),
    )
}

@Composable
private fun DownloadDirectory(
    app: AppSettings,
    update: ((AppSettings) -> AppSettings) -> Unit,
) {
    val context = LocalContext.current
    var error by remember { mutableStateOf<String?>(null) }
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            if (uri != null) {
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                    )
                    update { current ->
                        current.withClient { it.copy(downloads = it.downloads.copy(treeUri = uri.toString())) }
                    }
                } catch (_: SecurityException) {
                    error = "Не удалось сохранить доступ к папке"
                }
            }
        }
    SettingsItemRow(
        SettingsItem.Action(
            if (app.behavior.client.downloads.treeUri == null) "Выбрать папку загрузок" else "Папка загрузок",
            app.behavior.client.downloads.treeUri
                ?.let(::downloadDirectoryLabel) ?: "Не выбрана",
            onClick = { launcher.launch(null) },
        ),
    )
    error?.let { Text(it, Modifier.padding(horizontal = 16.dp), style = MaterialTheme.typography.bodyMedium) }
}
