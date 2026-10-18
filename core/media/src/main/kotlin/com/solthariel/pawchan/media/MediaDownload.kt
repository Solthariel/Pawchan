package com.solthariel.pawchan.media

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.core.media.R
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.coroutines.executeAsync
import java.io.IOException
import java.io.OutputStream

class MediaDownloader(
    private val client: OkHttpClient,
) {
    suspend fun download(
        url: String,
        destination: OutputStream,
    ): MediaDownloadResult =
        withContext(Dispatchers.IO) {
            try {
                client
                    .newCall(
                        Request
                            .Builder()
                            .url(url)
                            .get()
                            .build(),
                    ).executeAsync()
                    .use { response ->
                        if (!response.isSuccessful) {
                            return@withContext response.code.toMediaDownloadFailure()
                        }
                        val body = response.body
                        copyCancellableBytes(body.byteStream(), destination)
                        destination.flush()
                        MediaDownloadResult.Success
                    }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: IOException) {
                MediaDownloadResult.RetryableFailure
            }
        }
}

sealed interface MediaDownloadResult {
    data object Success : MediaDownloadResult

    data object RetryableFailure : MediaDownloadResult

    data object PermanentFailure : MediaDownloadResult
}

sealed interface MediaSaveStatus {
    data object Idle : MediaSaveStatus

    data class Queued(
        val fileName: String,
    ) : MediaSaveStatus

    data class QueuedMany(
        val count: Int,
    ) : MediaSaveStatus

    data object Failed : MediaSaveStatus
}

data class MediaSaveActions(
    val status: MediaSaveStatus,
    val request: (MediaAttachmentUi) -> Unit,
    /** Несколько файлов: одна папка вместо диалога на каждый файл. */
    val requestAll: (List<MediaAttachmentUi>) -> Unit = {},
)

@Composable
fun rememberMediaSaveActions(queue: MediaSaveQueue): MediaSaveActions {
    val context = LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val state = remember(queue, context, scope) { MediaSavePickerState(queue, context, scope) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree(), state::directoryPicked)
    LaunchedEffect(state.needsDirectory) {
        if (state.needsDirectory) {
            state.pickerStarted()
            picker.launch(null)
        }
    }
    MediaSaveCompletionEffect(state.status) { state.status = MediaSaveStatus.Idle }
    return MediaSaveActions(
        state.status,
        request = state::request,
        requestAll = state::requestAll,
    )
}

@Composable
fun MediaSaveStatusText(status: MediaSaveStatus) {
    val text =
        when (status) {
            MediaSaveStatus.Idle -> null
            is MediaSaveStatus.Queued -> stringResource(R.string.media_queued, status.fileName)
            is MediaSaveStatus.QueuedMany -> stringResource(R.string.media_queued_many, status.count)
            MediaSaveStatus.Failed -> stringResource(R.string.media_save_failed)
        }
    if (text != null) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            color =
                if (status is MediaSaveStatus.Failed) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                },
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

@Composable
private fun MediaSaveCompletionEffect(
    status: MediaSaveStatus,
    onReset: () -> Unit,
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    LaunchedEffect(status) {
        when (status) {
            is MediaSaveStatus.Queued -> {
                Toast
                    .makeText(
                        context,
                        resources.getString(R.string.media_queued, status.fileName),
                        Toast.LENGTH_SHORT,
                    ).show()
                kotlinx.coroutines.delay(STATUS_VISIBLE_MILLIS)
                onReset()
            }

            is MediaSaveStatus.QueuedMany -> {
                Toast
                    .makeText(
                        context,
                        resources.getString(R.string.media_queued_many, status.count),
                        Toast.LENGTH_SHORT,
                    ).show()
                kotlinx.coroutines.delay(STATUS_VISIBLE_MILLIS)
                onReset()
            }

            MediaSaveStatus.Failed -> {
                Toast
                    .makeText(
                        context,
                        resources.getString(R.string.media_save_failed),
                        Toast.LENGTH_SHORT,
                    ).show()
                kotlinx.coroutines.delay(STATUS_VISIBLE_MILLIS)
                onReset()
            }

            MediaSaveStatus.Idle -> {
                Unit
            }
        }
    }
}

private fun Int.toMediaDownloadFailure(): MediaDownloadResult =
    if (this == HTTP_TIMEOUT || this == HTTP_TOO_MANY_REQUESTS || this >= HTTP_SERVER_ERROR) {
        MediaDownloadResult.RetryableFailure
    } else {
        MediaDownloadResult.PermanentFailure
    }

internal fun safeMediaFileName(attachment: MediaAttachmentUi): String =
    attachment.fileName
        .substringAfterLast('/')
        .substringAfterLast('\\')
        .trim()
        .takeIf(String::isNotEmpty)
        ?: "media-${attachment.id}"

private const val HTTP_TIMEOUT = 408
private const val HTTP_TOO_MANY_REQUESTS = 429
private const val HTTP_SERVER_ERROR = 500
private const val STATUS_VISIBLE_MILLIS = 3_000L
