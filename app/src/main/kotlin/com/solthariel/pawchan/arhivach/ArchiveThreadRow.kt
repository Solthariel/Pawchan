package com.solthariel.pawchan.arhivach

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.solthariel.pawchan.media.MediaAttachmentUi
import com.solthariel.pawchan.media.thumbnail.ThumbnailLoading
import com.solthariel.pawchan.threads.ThreadListCard
import com.solthariel.pawchan.threads.ThreadListCardActions
import com.solthariel.pawchan.threads.ThreadListCardContentUi
import com.solthariel.pawchan.threads.ThreadListCardUi
import com.solthariel.pawchan.threads.ThreadListCountsUi
import com.solthariel.pawchan.threads.ThreadListStatusUi

@Composable
internal fun ArchiveThreadRow(
    thread: ArchiveThreadCardUi,
    thumbnailLoading: ThumbnailLoading,
    onClick: () -> Unit,
    onOpenMedia: (List<MediaAttachmentUi>, MediaAttachmentUi) -> Unit,
    modifier: Modifier = Modifier,
) {
    val card = remember(thread) { thread.toThreadListCardUi() }
    ThreadListCard(
        card = card,
        thumbnailLoading = thumbnailLoading,
        actions = ThreadListCardActions(onClick = onClick, onOpenMedia = onOpenMedia),
        modifier = modifier,
    )
}

private fun ArchiveThreadCardUi.toThreadListCardUi(): ThreadListCardUi =
    ThreadListCardUi(
        content =
            ThreadListCardContentUi(
                subject = summary.title,
                comment = summary.metadata.previewText,
                note = null,
                media = media,
            ),
        counts =
            ThreadListCountsUi(
                repliesText = summary.postCount.toString(),
                repliesDescription = "Постов: ${summary.postCount}",
                filesText = summary.media.size.toString(),
                filesDescription = "Файлов: ${summary.media.size}",
                sizeText = "",
            ),
        status =
            ThreadListStatusUi(
                pinned = false,
                closed = false,
                endless = false,
                additional =
                    listOfNotNull(
                        summary.metadata.status
                            .toArchiveStatus()
                            ?.listMarker,
                    ),
            ),
        ageText = formatArchiveThreadAge(summary.metadata.date),
    )
