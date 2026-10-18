package com.solthariel.pawchan.catalog

import com.solthariel.pawchan.core.ui.RefreshFailure
import com.solthariel.pawchan.core.ui.formatByteSizeForUi
import com.solthariel.pawchan.core.ui.visiblePostSubject
import com.solthariel.pawchan.domain.filter.ContentFilterHit
import com.solthariel.pawchan.domain.model.CatalogSnapshot
import com.solthariel.pawchan.domain.model.DomainFailure
import com.solthariel.pawchan.domain.model.DomainOutcome
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.isRateLimited
import com.solthariel.pawchan.media.MediaAttachmentUi

internal sealed interface CatalogUiState {
    data object Loading : CatalogUiState

    data class Content(
        val boardTitle: String,
        val boardSpeed: Int,
        val threads: List<CatalogThreadUi>,
        val isRefreshing: Boolean = false,
        val refreshFailure: RefreshFailure? = null,
        val viewerMediaGroups: Map<Long, List<MediaAttachmentUi>> = emptyMap(),
        val contentFilters: CatalogFilterPresentation = CatalogFilterPresentation(),
    ) : CatalogUiState {
        val autoHiddenThreads: List<CatalogThreadUi> get() = contentFilters.hiddenThreads
        val filtersRevealed: Boolean get() = contentFilters.revealed
        val allThreadsFiltered: Boolean get() = contentFilters.allThreadsFiltered
        val filterSamples: List<com.solthariel.pawchan.domain.filter.ContentFilterSample> get() = contentFilters.samples
    }

    data class Error(
        val reason: CatalogError,
        val challengeUrl: String? = null,
    ) : CatalogUiState
}

internal data class CatalogFilterPresentation(
    val hiddenThreads: List<CatalogThreadUi> = emptyList(),
    val revealed: Boolean = false,
    val allThreadsFiltered: Boolean = false,
    val samples: List<com.solthariel.pawchan.domain.filter.ContentFilterSample> = emptyList(),
    val hits: List<ContentFilterHit> = emptyList(),
)

internal data class CatalogThreadUi(
    val address: ThreadAddress,
    val content: CatalogThreadContentUi,
    val attachments: List<MediaAttachmentUi>,
    val flags: CatalogThreadFlagsUi,
    val statistics: CatalogThreadStatisticsUi,
    val hidden: Boolean = false,
    val age: CatalogThreadAgeUi = CatalogThreadAgeUi(),
) {
    val mediaDisplay =
        com.solthariel.pawchan.media
            .PostMediaDisplay(attachments)

    val note: String?
        get() = content.note

    val subject: String?
        get() = content.subject
    val author: String?
        get() = content.author
    val date: String?
        get() = content.date
    val sage: Boolean
        get() = content.sage
    val pinned: Boolean
        get() = flags.pinned
    val closed: Boolean
        get() = flags.closed
    val endless: Boolean
        get() = flags.endless
    val replies: Int
        get() = statistics.replies
    val files: Int
        get() = statistics.files
    val knownMediaBytes: Long
        get() = statistics.knownMediaBytes
    val metadataText: String
        get() = statistics.displayText
    val mediaBytesComplete: Boolean
        get() = statistics.mediaBytesComplete
    val id: Long
        get() = address.thread.value

    val comment: String
        get() = content.commentPreview

    val preview: MediaAttachmentUi?
        get() = attachments.firstOrNull()

    val displaySubject: String?
        get() = content.displaySubject

    val hasMultipleAttachments: Boolean
        get() = attachments.size > 1
}

internal data class CatalogThreadContentUi(
    val subject: String?,
    val text: CatalogThreadTextUi,
    val author: String?,
    val date: String?,
    val sage: Boolean,
    val displaySubject: String? = visiblePostSubject(subject, text.preview),
    /** Строка состояния над счётчиками (сохранённые копии: «файлы 28/81», «Тред умер»); null — нет. */
    val note: String? = null,
) {
    val commentPreview: String get() = text.preview
    val filterText: String get() = text.plainText
}

/** Display preview and parsed source text stay separate, without losing the original OP. */
internal data class CatalogThreadTextUi(
    val preview: String,
    val plainText: String = preview,
)

internal data class CatalogThreadFlagsUi(
    val pinned: Boolean,
    val closed: Boolean,
    val endless: Boolean,
    val autoHide: CatalogAutoHideReason? = null,
)

internal data class CatalogThreadStatisticsUi(
    val replies: Int,
    val files: Int,
    val knownMediaBytes: Long,
    val mediaBytesComplete: Boolean,
    val displayText: String = catalogMetadataText(replies, files, knownMediaBytes),
) {
    val repliesText: String = replies.toString()
    val filesText: String = files.toString()
    val repliesDescription: String = "Ответов: $replies"
    val filesDescription: String = "Файлов: $files"
    val sizeText: String =
        if (knownMediaBytes > 0L) {
            formatByteSizeForUi(knownMediaBytes)
        } else {
            ""
        }
}

internal enum class CatalogError {
    NETWORK,
    TIMEOUT,
    SECURE_CONNECTION,
    NOT_FOUND,
    INVALID_RESPONSE,
    SERVER,
    RATE_LIMITED,
    PROTECTION,
    STORAGE,
}

internal fun DomainOutcome<CatalogSnapshot>.toUiState(nowEpochSeconds: Long = currentEpochSeconds()): CatalogUiState =
    when (this) {
        is DomainOutcome.Success -> {
            value.toCatalogUiState(nowEpochSeconds)
        }

        is DomainOutcome.Failure -> {
            reason.toCatalogErrorState()
        }
    }

internal fun com.solthariel.pawchan.domain.model.PostDocument.toCatalogPreview(): String {
    var previousWasLineBreak = false
    return buildString {
        runs.forEach { run ->
            run.text.forEach { character ->
                val normalizedCharacter = if (character == '\r') '\n' else character
                if (normalizedCharacter != '\n' || !previousWasLineBreak) {
                    append(normalizedCharacter)
                }
                previousWasLineBreak = normalizedCharacter == '\n'
            }
        }
    }.trimEnd()
}

private fun DomainFailure.toCatalogErrorState(): CatalogUiState.Error =
    when (this) {
        DomainFailure.NetworkUnavailable -> {
            CatalogUiState.Error(CatalogError.NETWORK)
        }

        DomainFailure.NetworkTimeout -> {
            CatalogUiState.Error(CatalogError.TIMEOUT)
        }

        DomainFailure.SecureConnectionFailed -> {
            CatalogUiState.Error(CatalogError.SECURE_CONNECTION)
        }

        DomainFailure.NotFound -> {
            CatalogUiState.Error(CatalogError.NOT_FOUND)
        }

        DomainFailure.StorageUnavailable -> {
            CatalogUiState.Error(CatalogError.STORAGE)
        }

        DomainFailure.InvalidResponse -> {
            CatalogUiState.Error(CatalogError.INVALID_RESPONSE)
        }

        is DomainFailure.HttpStatus -> {
            CatalogUiState.Error(
                if (isRateLimited) CatalogError.RATE_LIMITED else CatalogError.SERVER,
            )
        }

        is DomainFailure.ProtectionChallenge -> {
            CatalogUiState.Error(CatalogError.PROTECTION, verificationUrl.value)
        }
    }

private fun catalogMetadataText(
    replies: Int,
    files: Int,
    mediaBytes: Long,
): String =
    buildString {
        append("Ответов: ")
        append(replies)
        append("   Файлов: ")
        append(files)
        if (mediaBytes > 0L) {
            append("   ")
            append(formatByteSizeForUi(mediaBytes))
        }
    }

internal data class CatalogAutoHideReason(
    val ruleId: String,
    val label: String,
)
