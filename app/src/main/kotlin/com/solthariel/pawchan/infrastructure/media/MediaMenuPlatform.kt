package com.solthariel.pawchan.infrastructure.media

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.FileProvider
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import com.solthariel.pawchan.R
import com.solthariel.pawchan.domain.model.MediaKind
import com.solthariel.pawchan.infrastructure.MediaLoaders
import com.solthariel.pawchan.media.ImageSearchEngine
import com.solthariel.pawchan.media.MediaAttachmentUi
import com.solthariel.pawchan.media.MediaContextMenu
import com.solthariel.pawchan.media.MediaMenuActions
import com.solthariel.pawchan.media.MediaMenuRequest
import com.solthariel.pawchan.media.MediaSaveActions
import com.solthariel.pawchan.workspace.LocalWorkspace
import com.solthariel.pawchan.workspace.addFavoriteMedia
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

internal class MediaMenuPlatform(
    private val context: Context,
    private val client: OkHttpClient,
) {
    fun copyLink(attachment: MediaAttachmentUi) {
        copyClip(ClipData.newPlainText(attachment.fileName, attachment.originalUrl))
    }

    fun search(
        attachment: MediaAttachmentUi,
        engine: ImageSearchEngine,
    ) {
        val sourceUrl = attachment.thumbnailUrl ?: attachment.originalUrl
        val base =
            when (engine) {
                ImageSearchEngine.GOOGLE -> "https://lens.google.com/uploadbyurl"
                ImageSearchEngine.YANDEX -> "https://yandex.ru/images/search"
            }
        val uri =
            base
                .toUri()
                .buildUpon()
                .appendQueryParameter("url", sourceUrl)
                .apply {
                    if (engine == ImageSearchEngine.YANDEX) appendQueryParameter("rpt", "imageview")
                }.build()
        context.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    suspend fun copyImage(attachment: MediaAttachmentUi): Boolean {
        try {
            val request =
                Request
                    .Builder()
                    .url(attachment.originalUrl)
                    .header("User-Agent", MEDIA_USER_AGENT)
                    .header("Accept", "image/*,*/*;q=0.8")
                    .header("Referer", DVACH_REFERER)
                    .build()
            val file = clipboardFile(attachment.fileName)
            writeClipboardImage(client, request, file)
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.media", file)
            copyClip(ClipData.newUri(context.contentResolver, attachment.fileName, uri))
            return true
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            return false
        }
    }

    private fun clipboardFile(name: String): File {
        val safe = name.substringAfterLast('/').replace(UNSAFE_FILE_NAME, "_").ifBlank { "image" }
        return File(context.cacheDir.resolve("media_clipboard"), safe)
    }

    private fun copyClip(clip: ClipData) {
        context.getSystemService<ClipboardManager>()?.setPrimaryClip(clip)
    }
}

/** Куда меню файла умеет переходить с этого экрана; null — пункта нет. */
internal data class MediaMenuTargets(
    val goToPost: ((MediaAttachmentUi) -> Unit)? = null,
    val openThread: ((MediaAttachmentUi) -> Unit)? = null,
)

@Composable
internal fun MediaContextMenuHost(
    request: MediaMenuRequest?,
    loaders: MediaLoaders,
    save: MediaSaveActions,
    onNotice: (String) -> Unit,
    targets: MediaMenuTargets = MediaMenuTargets(),
    onClose: () -> Unit,
) {
    // Screen-owned scope survives dismissing the menu, but not leaving its host.
    val scope = rememberCoroutineScope()
    if (request == null) return
    val attachment = request.attachment
    val context = LocalContext.current
    val platform = remember(context, loaders.contentClient) { MediaMenuPlatform(context, loaders.contentClient) }
    val linkCopied = stringResource(R.string.media_link_copied)
    val imageCopied = stringResource(R.string.media_image_copied)
    val imageCopyFailed = stringResource(R.string.media_image_copy_failed)
    val workspace = LocalWorkspace.current
    MediaContextMenu(
        request = request,
        actions =
            MediaMenuActions(
                onDownload = {
                    onClose()
                    save.request(attachment)
                },
                onCopyLink = {
                    platform.copyLink(attachment)
                    onNotice(linkCopied)
                    onClose()
                },
                onCopyImage =
                    if (attachment.kind == MediaKind.IMAGE) {
                        {
                            onClose()
                            scope.launch {
                                onNotice(if (platform.copyImage(attachment)) imageCopied else imageCopyFailed)
                            }
                        }
                    } else {
                        null
                    },
                onSearch = {
                    platform.search(attachment, it)
                    onClose()
                },
                onFavorite = { workspace?.controller?.addFavoriteMedia(listOf(attachment)) },
                onGoToPost = targets.goToPost?.let { goTo -> { goTo(attachment) } },
                onOpenThread = targets.openThread?.let { open -> { open(attachment) } },
            ),
        onClose = onClose,
    )
}

private val UNSAFE_FILE_NAME = Regex("[^A-Za-z0-9._-]")
private const val DVACH_REFERER = "https://2ch.su/"
private const val MEDIA_USER_AGENT =
    "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 " +
        "Chrome/124 Mobile Safari/537.36"
