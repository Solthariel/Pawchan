package com.solthariel.pawchan.media

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.solthariel.pawchan.core.media.R
import com.solthariel.pawchan.core.ui.TransientNotice
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
    val pickerPlayback: MediaPickerPlayback? = null,
)

@Composable
fun rememberMediaSaveActions(queue: MediaSaveQueue): MediaSaveActions {
    val context = LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val state = remember(queue, context, scope) { MediaSavePickerState(queue, context, scope) }
    val playback = remember(state) { MediaPickerPlayback() }
    val picker =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            state.directoryPicked(uri)
            playback.completed()
        }
    LaunchedEffect(state.needsDirectory) {
        if (state.needsDirectory) {
            state.pickerStarted()
            playback.started()
            picker.launch(null)
        }
    }
    val noticeVersion = state.noticeVersion
    MediaSaveCompletionEffect(state.status, noticeVersion) {
        if (state.noticeVersion == noticeVersion) state.status = MediaSaveStatus.Idle
    }
    return MediaSaveActions(
        state.status,
        request = state::request,
        requestAll = state::requestAll,
        pickerPlayback = playback,
    )
}

@Composable
private fun MediaSaveCompletionEffect(
    status: MediaSaveStatus,
    noticeVersion: Long,
    onReset: () -> Unit,
) {
    val text =
        when (status) {
            MediaSaveStatus.Idle -> null
            is MediaSaveStatus.Queued -> null
            is MediaSaveStatus.QueuedMany -> null
            MediaSaveStatus.Failed -> stringResource(R.string.media_save_failed)
        }
    androidx.compose.runtime.LaunchedEffect(status, noticeVersion) {
        if (status is MediaSaveStatus.Queued || status is MediaSaveStatus.QueuedMany) {
            kotlinx.coroutines.delay(QUEUE_RESET_DELAY_MILLIS)
            onReset()
        }
    }
    TransientNotice(text, eventKey = noticeVersion, onDismiss = onReset)
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

private const val QUEUE_RESET_DELAY_MILLIS = 200L
