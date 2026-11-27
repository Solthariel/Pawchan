package com.solthariel.pawchan.catalog

import com.solthariel.pawchan.core.ui.visiblePostSubject
import com.solthariel.pawchan.domain.model.MediaAttachment
import com.solthariel.pawchan.domain.model.ThreadSnapshot
import com.solthariel.pawchan.media.toMediaAttachmentUi
import com.solthariel.pawchan.media.withMediaOwner

/** Карточка каталога из сохранённого треда (история, копия): ОП-пост, счётчики по всем постам. */
internal fun ThreadSnapshot.toCatalogThreadUi(
    nowEpochSeconds: Long = currentEpochSeconds(),
    note: String? = null,
): CatalogThreadUi {
    val op = posts.firstOrNull()
    val files = posts.flatMap { it.attachments }
    val text = op?.content?.document?.toCatalogText() ?: CatalogThreadTextUi("")
    return CatalogThreadUi(
        address = address,
        content =
            CatalogThreadContentUi(
                subject = subject,
                text = text,
                author = op?.content?.metadata?.name,
                date = op?.content?.metadata?.date,
                sage = op?.content?.metadata?.sage == true,
                displaySubject = if (subjectsEnabled == false) null else visiblePostSubject(subject, text.preview),
                note = note,
            ),
        attachments =
            op
                ?.attachments
                .orEmpty()
                .map(MediaAttachment::toMediaAttachmentUi)
                .withMediaOwner(address.thread.value, threadAddress = address),
        flags = CatalogThreadFlagsUi(pinned = flags.pinned, closed = flags.closed, endless = flags.endless),
        statistics =
            CatalogThreadStatisticsUi(
                replies = (posts.size - 1).coerceAtLeast(0),
                files = files.size,
                knownMediaBytes = files.sumOf { it.byteSize?.value ?: 0L },
                mediaBytesComplete = files.all { it.byteSize != null },
            ),
        age = catalogThreadAge(op?.content?.details?.timestamp, nowEpochSeconds),
    )
}
