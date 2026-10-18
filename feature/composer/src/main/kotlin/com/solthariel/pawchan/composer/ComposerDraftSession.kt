package com.solthariel.pawchan.composer

import com.solthariel.pawchan.domain.drafts.ComposerDraftKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.coroutines.coroutineContext

internal class ComposerDraftSession(
    private val scope: CoroutineScope,
    private val coordinator: ComposerDraftCoordinator?,
    private val key: ComposerDraftKey,
    private val actions: ComposerDraftSessionActions,
) {
    private var restoreJob: Job? = null
    private var closeJob: Job? = null
    private var generation = 0
    private var restoreFailed = false
    val restoring: Boolean get() = restoreJob?.isActive == true

    fun restore() {
        val storage = coordinator ?: return
        restoreFailed = false
        val revision = ++generation
        val resetId = actions.read().resetId
        actions.write(actions.read().copy(filesLoading = true))
        val worker =
            scope.launch(start = CoroutineStart.LAZY) {
                try {
                    val result = storage.load(key)
                    if (revision == generation && actions.read().resetId == resetId) {
                        if (result.isSuccess) {
                            actions.write((result.getOrNull() ?: ComposerDraftState()).copy(resetId = resetId))
                        } else {
                            restoreFailed = true
                            actions.write(actions.read().copy(filesLoading = false))
                            actions.fail("Не удалось восстановить черновик. Повторите открытие формы.")
                        }
                    }
                } finally {
                    if (restoreJob === coroutineContext[Job]) restoreJob = null
                }
            }
        restoreJob = worker
        worker.start()
    }

    suspend fun awaitRestore() {
        restoreJob?.join()
    }

    fun changed(
        draft: ComposerDraftState,
        immediate: Boolean = false,
    ) {
        if (!draft.filesLoading) {
            restoreFailed = false
            coordinator?.schedule(key, draft, immediate)
        }
    }

    fun saveNow(draft: ComposerDraftState) {
        if (!restoreFailed && !draft.filesLoading) coordinator?.schedule(key, draft, immediate = true)
    }

    fun clear(draft: ComposerDraftState) {
        restoreFailed = false
        generation++
        restoreJob?.cancel()
        coordinator?.schedule(key, draft, immediate = true)
    }

    fun close(
        draft: ComposerDraftState,
        onClosed: () -> Unit,
    ) {
        if (closeJob?.isActive == true) return
        generation++
        if (coordinator == null || restoring || restoreFailed) {
            restoreJob?.cancel()
            onClosed()
        } else {
            actions.write(draft.copy(filesLoading = true))
            val worker =
                scope.launch(start = CoroutineStart.LAZY) {
                    try {
                        if (coordinator.flush(key, draft)) {
                            onClosed()
                        } else {
                            actions.write(draft.copy(filesLoading = false))
                            actions.fail("Не удалось сохранить черновик. Форма осталась открытой.")
                        }
                    } finally {
                        if (closeJob === coroutineContext[Job]) closeJob = null
                    }
                }
            closeJob = worker
            worker.start()
        }
    }

    suspend fun sent() {
        coordinator?.discard(key)
    }
}

internal class ComposerDraftSessionActions(
    val read: () -> ComposerDraftState,
    val write: (ComposerDraftState) -> Unit,
    val fail: (String) -> Unit,
)
