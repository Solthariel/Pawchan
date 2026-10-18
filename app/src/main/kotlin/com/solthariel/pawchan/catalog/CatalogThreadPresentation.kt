package com.solthariel.pawchan.catalog

import com.solthariel.pawchan.core.ui.visiblePostSubject
import com.solthariel.pawchan.domain.filter.ContentFilterPlace
import com.solthariel.pawchan.domain.filter.ContentFilterSample
import com.solthariel.pawchan.domain.model.CatalogSnapshot
import com.solthariel.pawchan.domain.model.CatalogThread
import com.solthariel.pawchan.domain.model.MediaAttachment
import com.solthariel.pawchan.media.toMediaAttachmentUi
import com.solthariel.pawchan.media.withMediaOwner

internal fun CatalogSnapshot.toCatalogUiState(nowEpochSeconds: Long): CatalogUiState.Content =
    CatalogUiState.Content(
        boardTitle = board.title,
        boardSpeed = boardSpeed,
        contentFilters =
            CatalogFilterPresentation(
                samples =
                    threads.map { thread ->
                        ContentFilterSample(
                            thread.address.board,
                            thread.subject,
                            thread.content.document.plainText,
                            false,
                            ContentFilterPlace.CATALOG,
                            thread.address.thread.value,
                        )
                    },
            ),
        threads = threads.map { it.toCatalogThreadUi(board.subjectsEnabled, nowEpochSeconds) },
    )

private fun CatalogThread.toCatalogThreadUi(
    subjectsEnabled: Boolean?,
    nowEpochSeconds: Long,
): CatalogThreadUi =
    CatalogThreadUi(
        address = address,
        content =
            CatalogThreadContentUi(
                subject = subject,
                text = CatalogThreadTextUi(content.document.toCatalogPreview(), content.document.plainText),
                author = content.metadata.name,
                date = content.metadata.date,
                sage = content.metadata.sage,
                displaySubject = displaySubject(subjectsEnabled),
            ),
        attachments = attachments.map(MediaAttachment::toMediaAttachmentUi).withMediaOwner(address.thread.value),
        flags = CatalogThreadFlagsUi(pinned = flags.pinned, closed = flags.closed, endless = flags.endless),
        statistics =
            CatalogThreadStatisticsUi(
                replies = statistics.replies,
                files = statistics.files,
                knownMediaBytes = statistics.knownMediaBytes,
                mediaBytesComplete = statistics.mediaBytesComplete,
            ),
        age = catalogThreadAge(createdAtEpochSeconds, nowEpochSeconds),
    )

private fun CatalogThread.displaySubject(subjectsEnabled: Boolean?): String? =
    when (subjectsEnabled) {
        false -> null
        true -> subject
        null -> visiblePostSubject(subject, content.document.plainText)
    }
