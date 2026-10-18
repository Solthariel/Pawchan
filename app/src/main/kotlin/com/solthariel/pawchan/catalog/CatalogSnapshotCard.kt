package com.solthariel.pawchan.catalog

import com.solthariel.pawchan.core.ui.visiblePostSubject
import com.solthariel.pawchan.domain.model.MediaAttachment
import com.solthariel.pawchan.domain.model.ThreadAddress
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
    val comment =
        op
            ?.content
            ?.document
            ?.toCatalogPreview()
            .orEmpty()
    return CatalogThreadUi(
        address = address,
        content =
            CatalogThreadContentUi(
                subject = subject,
                text = CatalogThreadTextUi(comment),
                author = op?.content?.metadata?.name,
                date = op?.content?.metadata?.date,
                sage = op?.content?.metadata?.sage == true,
                displaySubject = if (subjectsEnabled == false) null else visiblePostSubject(subject, comment),
                note = note,
            ),
        attachments =
            op
                ?.attachments
                .orEmpty()
                .map(MediaAttachment::toMediaAttachmentUi)
                .withMediaOwner(address.thread.value),
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

/** Тред, о котором известен только заголовок (ещё не открывался): карточка без превью и счётчиков. */
internal fun placeholderCatalogThread(
    address: ThreadAddress,
    subject: String?,
): CatalogThreadUi =
    CatalogThreadUi(
        address = address,
        content =
            CatalogThreadContentUi(
                subject = subject,
                text = CatalogThreadTextUi(""),
                author = null,
                date = null,
                sage = false,
                displaySubject = subject ?: "Тред №${address.thread.value}",
            ),
        attachments = emptyList(),
        flags = CatalogThreadFlagsUi(pinned = false, closed = false, endless = false),
        statistics = CatalogThreadStatisticsUi(0, 0, 0L, mediaBytesComplete = true),
    )
