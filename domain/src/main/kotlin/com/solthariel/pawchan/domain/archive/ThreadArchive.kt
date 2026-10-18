package com.solthariel.pawchan.domain.archive

import com.solthariel.pawchan.domain.model.DomainFailure
import com.solthariel.pawchan.domain.model.DomainOutcome
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadSnapshot

/** LOCAL is never a promise that the server's last post was captured. */
enum class ArchivePhase { LOCAL, WATCHING, SOURCE_UNAVAILABLE, NEEDS_ATTENTION }

data class ArchivedMedia(
    val url: String,
    val file: String,
    val bytes: Long,
)

data class ArchiveMediaState(
    val files: List<ArchivedMedia> = emptyList(),
    val failure: String? = null,
)

/** [sourceUrl] — ссылка на Архивач из прежних версий; новые ссылки хранятся в ThreadMarks.arhivach. */
data class ArchiveState(
    val includeMedia: Boolean = false,
    val sourceUrl: String? = null,
    val checkedAt: Long = 0,
    val phase: ArchivePhase = ArchivePhase.LOCAL,
    val message: String? = null,
    val media: ArchiveMediaState = ArchiveMediaState(),
) {
    val files: List<ArchivedMedia> get() = media.files
}

/** Сторонний архив тредов (Архивач): добавление с капчей, загрузка копии, проверка доступности. */
interface ExternalThreadArchive {
    /** Картинка капчи; каждый вызов — новый код в той же сессии. */
    suspend fun captcha(): DomainOutcome<ByteArray> = DomainOutcome.Failure(DomainFailure.InvalidResponse)

    suspend fun add(
        address: ThreadAddress,
        captcha: String,
    ): ArchiveSubmission

    suspend fun load(
        address: ThreadAddress,
        url: String,
    ): DomainOutcome<ThreadSnapshot>

    suspend fun available(): Boolean = true
}

sealed interface ArchiveSubmission {
    /** Ссылка на копию; [alreadyArchived] — тред был на Архиваче раньше («Тред уже был добавлен ранее»). */
    data class Accepted(
        val url: String,
        val alreadyArchived: Boolean = false,
    ) : ArchiveSubmission

    data class Rejected(
        val message: String,
    ) : ArchiveSubmission

    /** Неверная капча: диалог показывает новую картинку и не закрывается. */
    data class WrongCaptcha(
        val message: String,
    ) : ArchiveSubmission
}

/** Preserve locally captured posts, even if the latest response omits deleted posts. */
fun mergeArchivedThread(
    previous: ThreadSnapshot,
    incoming: ThreadSnapshot,
): ThreadSnapshot {
    require(previous.address == incoming.address)
    val posts =
        (previous.posts.associateBy { it.address.post } + incoming.posts.associateBy { it.address.post })
            .values
            .sortedBy { it.address.post.value }
    return ThreadSnapshot(
        previous.address,
        incoming.subject ?: previous.subject,
        incoming.flags,
        posts,
        incoming.uniquePosters,
        incoming.subjectsEnabled ?: previous.subjectsEnabled,
        incoming.tag ?: previous.tag,
    )
}

/** Compare the archive generation/settings, preserving position changes made during I/O. */
fun com.solthariel.pawchan.domain.workspace.WorkspaceState.replaceArchive(
    expected: com.solthariel.pawchan.domain.workspace.ThreadCopy,
    updated: com.solthariel.pawchan.domain.workspace.ThreadCopy,
): com.solthariel.pawchan.domain.workspace.WorkspaceState =
    copy(
        copies =
            copies.map {
                if (it.snapshot.address == expected.snapshot.address && it.savedAt == expected.savedAt &&
                    it.archive == expected.archive
                ) {
                    updated.copy(position = it.position)
                } else {
                    it
                }
            },
    )
