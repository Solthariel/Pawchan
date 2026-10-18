package com.solthariel.pawchan.domain.archive

import com.solthariel.pawchan.domain.model.DomainFailure
import com.solthariel.pawchan.domain.model.DomainOutcome
import com.solthariel.pawchan.domain.thread.ThreadRepository
import com.solthariel.pawchan.domain.workspace.ThreadCopy
import com.solthariel.pawchan.domain.workspace.WorkspaceRepository

class ArchiveSynchronizer(
    private val workspace: WorkspaceRepository,
    private val live: ThreadRepository,
    private val external: ExternalThreadArchive,
    private val now: () -> Long,
) {
    /** Обновляет все сохранённые копии, кроме умерших тредов; включается общей настройкой. */
    suspend fun run(): Boolean {
        var success = true
        workspace.load().copies.filter { it.archive.phase != ArchivePhase.SOURCE_UNAVAILABLE }.forEach { copy ->
            if (now() - copy.archive.checkedAt >=
                if (copy.archive.message == null) MIN_ARCHIVE_CHECK_MILLIS else ARCHIVE_RETRY_MILLIS
            ) {
                if (!update(copy)) success = false
            }
        }
        return success
    }

    suspend fun update(
        copy: ThreadCopy,
        externalOnly: Boolean = false,
    ): Boolean {
        val liveResult =
            if (externalOnly) {
                DomainOutcome.Failure(DomainFailure.NotFound)
            } else {
                live.loadThread(copy.snapshot.address)
            }
        val link = workspace.load().marks.arhivach[copy.snapshot.address] ?: copy.archive.sourceUrl
        val outcome =
            if (liveResult is DomainOutcome.Failure && link != null) {
                external.load(copy.snapshot.address, link)
            } else {
                liveResult
            }
        val changed = copy.afterCheck(outcome, now())
        workspace.update { it.replaceArchive(copy, changed) }
        return outcome is DomainOutcome.Success
    }
}

private fun ThreadCopy.afterCheck(
    outcome: DomainOutcome<com.solthariel.pawchan.domain.model.ThreadSnapshot>,
    time: Long,
): ThreadCopy =
    when (outcome) {
        is DomainOutcome.Success -> {
            copy(
                snapshot = mergeArchivedThread(snapshot, outcome.value),
                archive =
                    archive.copy(
                        checkedAt = time,
                        phase = ArchivePhase.LOCAL,
                        message = null,
                    ),
            )
        }

        is DomainOutcome.Failure -> {
            copy(
                archive =
                    archive.copy(
                        checkedAt = time,
                        phase =
                            if (outcome.reason ==
                                DomainFailure.NotFound
                            ) {
                                ArchivePhase.SOURCE_UNAVAILABLE
                            } else {
                                ArchivePhase.NEEDS_ATTENTION
                            },
                        message = "Обновление не удалось: ${outcome.reason}. Локальная копия сохранена.",
                    ),
            )
        }
    }

private const val MIN_ARCHIVE_CHECK_MILLIS = 15_000L

private const val ARCHIVE_RETRY_MILLIS = 15 * 60_000L
