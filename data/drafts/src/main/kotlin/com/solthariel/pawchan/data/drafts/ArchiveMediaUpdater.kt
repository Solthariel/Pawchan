package com.solthariel.pawchan.data.drafts

import com.solthariel.pawchan.domain.archive.ArchiveMediaState
import com.solthariel.pawchan.domain.archive.ArchivedMedia
import com.solthariel.pawchan.domain.workspace.ThreadCopy
import com.solthariel.pawchan.domain.workspace.WorkspaceRepository
import java.io.IOException

/** Each completed file is committed; a killed worker resumes without losing the media manifest. */
class ArchiveMediaUpdater(
    private val repository: WorkspaceRepository,
    private val media: ArchiveMediaStore,
) {
    suspend fun update(copy: ThreadCopy): Boolean =
        try {
            media.download(copy) { file ->
                mutate(copy) { state -> state.copy(files = state.files.filterNot { it.url == file.url } + file) }
            }
            mutate(copy) { it.copy(failure = null) }
            true
        } catch (failure: IOException) {
            mutate(copy) { it.copy(failure = "Медиа сохранены не полностью: ${failure.message}") }
            false
        }

    private suspend fun mutate(
        copy: ThreadCopy,
        transform: (ArchiveMediaState) -> ArchiveMediaState,
    ) {
        repository.update { state ->
            state.copy(
                copies =
                    state.copies.map {
                        if (it.snapshot.address == copy.snapshot.address && it.savedAt == copy.savedAt &&
                            it.archive.includeMedia
                        ) {
                            it.copy(archive = it.archive.copy(media = transform(it.archive.media)))
                        } else {
                            it
                        }
                    },
            )
        }
    }
}
