package com.solthariel.pawchan.watch

import android.content.Context
import com.solthariel.pawchan.data.drafts.FileThreadHistoryRepository
import com.solthariel.pawchan.data.drafts.FileWorkspaceRepository
import com.solthariel.pawchan.domain.library.ThreadLibraryRepository
import com.solthariel.pawchan.domain.model.DomainOutcome
import com.solthariel.pawchan.domain.thread.ThreadRepository
import com.solthariel.pawchan.domain.thread.mergeThreadHistory
import com.solthariel.pawchan.domain.workspace.withSeen
import java.io.File
import java.io.IOException

internal suspend fun refreshBackgroundFavorites(
    context: Context,
    library: ThreadLibraryRepository,
    repository: ThreadRepository,
): Boolean {
    val entries =
        when (val loaded = library.loadFavorites()) {
            is DomainOutcome.Success -> loaded.value.filter { it.favorite }
            is DomainOutcome.Failure -> return false
        }
    val history = FileThreadHistoryRepository(File(context.filesDir, "threads"))
    val workspace = FileWorkspaceRepository(File(context.filesDir, "workspace"))
    var success = true
    try {
        for (entry in entries) {
            if (repository.isThreadDeleted(entry.address)) continue
            when (val result = repository.loadThread(entry.address)) {
                is DomainOutcome.Success -> {
                    history.save(mergeThreadHistory(history.load(entry.address), result.value))
                    val last = result.value.posts.maxOfOrNull { it.address.post.value }
                    workspace.update { state ->
                        if (last != null && state.marks.seen[entry.address] == null) {
                            state.withSeen(entry.address, last)
                        } else {
                            state
                        }
                    }
                }

                is DomainOutcome.Failure -> {
                    if (result.reason != com.solthariel.pawchan.domain.model.DomainFailure.NotFound) success = false
                }
            }
        }
    } catch (_: IOException) {
        success = false
    }
    return success
}
