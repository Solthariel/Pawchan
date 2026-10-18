package com.solthariel.pawchan.archive

import com.solthariel.pawchan.catalog.CatalogThreadUi
import com.solthariel.pawchan.catalog.toCatalogThreadUi
import com.solthariel.pawchan.domain.archive.ArchivePhase
import com.solthariel.pawchan.domain.workspace.ThreadCopy

internal data class ArchiveCopyUi(
    val copy: ThreadCopy,
    val title: String,
    /** Карточка как в каталоге; строка состояния — загрузка файлов, «Тред умер», ошибка. */
    val card: CatalogThreadUi,
    val pendingMedia: Boolean,
)

internal fun prepareArchiveCopy(copy: ThreadCopy): ArchiveCopyUi {
    val required =
        copy.snapshot.posts
            .flatMap { it.attachments }
            .map { it.remoteUrl.value }
            .toSet()
    val status =
        when (copy.archive.phase) {
            ArchivePhase.LOCAL, ArchivePhase.WATCHING -> null
            ArchivePhase.SOURCE_UNAVAILABLE -> "Тред умер, копия сохранена"
            ArchivePhase.NEEDS_ATTENTION -> "Не удалось обновить"
        }
    val missing =
        required -
            copy.archive.files
                .map { it.url }
                .toSet()
    val expected = required.size
    val complete = expected - missing.size
    val media = if (copy.archive.includeMedia) "файлы $complete/$expected" else "без медиа"
    val note = listOfNotNull(media, status, copy.archive.media.failure).joinToString(" · ")
    return ArchiveCopyUi(
        copy,
        copy.snapshot.subject ?: "/${copy.snapshot.address.board.board.value}/${copy.snapshot.address.thread.value}",
        copy.snapshot.toCatalogThreadUi(note = note),
        copy.archive.includeMedia && (missing.isNotEmpty() || copy.archive.media.failure != null),
    )
}
