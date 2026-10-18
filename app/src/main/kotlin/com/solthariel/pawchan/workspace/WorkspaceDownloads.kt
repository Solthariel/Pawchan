package com.solthariel.pawchan.workspace

import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.work.WorkManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal data class DownloadEntry(
    val id: java.util.UUID,
    val name: String,
    val status: String,
    val destination: String?,
    val completed: Boolean,
)

internal class DownloadsViewModel(
    manager: WorkManager,
) : ViewModel() {
    private val mutableState = MutableStateFlow<List<DownloadEntry>>(emptyList())
    val state = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            manager.getWorkInfosByTagFlow("neko-media").collect { work ->
                mutableState.value =
                    work.map { item ->
                        DownloadEntry(
                            item.id,
                            item.tags.firstOrNull { it.startsWith("media-name:") }?.removePrefix("media-name:")
                                ?: "Медиа",
                            when (item.state) {
                                androidx.work.WorkInfo.State.SUCCEEDED -> "Скачано"
                                androidx.work.WorkInfo.State.FAILED -> "Ошибка"
                                androidx.work.WorkInfo.State.CANCELLED -> "Отменено"
                                androidx.work.WorkInfo.State.RUNNING -> "Скачивается"
                                else -> "В очереди"
                            },
                            item.tags
                                .firstOrNull {
                                    it.startsWith(
                                        "media-destination:",
                                    )
                                }?.removePrefix("media-destination:"),
                            item.state == androidx.work.WorkInfo.State.SUCCEEDED,
                        )
                    }
            }
        }
    }

    companion object {
        fun factory(manager: WorkManager) = viewModelFactory { initializer { DownloadsViewModel(manager) } }
    }
}

@Composable
internal fun WorkspaceDownloads() {
    val context = LocalContext.current
    val manager = remember(context) { WorkManager.getInstance(context.applicationContext) }
    val model: DownloadsViewModel = viewModel(factory = DownloadsViewModel.factory(manager))
    val entries by model.state.collectAsStateWithLifecycle()
    var error by remember { mutableStateOf<String?>(null) }
    Column {
        if (entries.isEmpty()) Text("Загрузок пока нет")
        error?.let { Text(it) }
        entries.forEach { entry ->
            Row {
                Text("${entry.name}: ${entry.status}")
                if (entry.completed && entry.destination != null) {
                    TextButton(onClick = {
                        try {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW)
                                    .setDataAndType(entry.destination.toUri(), "*/*")
                                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),
                            )
                        } catch (
                            _: android.content.ActivityNotFoundException,
                        ) {
                            error = "Нет приложения для открытия файла"
                        } catch (
                            _: SecurityException,
                        ) {
                            error = "Нет доступа к сохранённому файлу"
                        }
                    }) { Text("Открыть") }
                }
                if (!entry.completed) TextButton(onClick = { manager.cancelWorkById(entry.id) }) { Text("Отменить") }
            }
        }
    }
}
