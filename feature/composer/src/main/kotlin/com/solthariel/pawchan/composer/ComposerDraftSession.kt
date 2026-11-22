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
    var restoreFailed = false
        private set
    val restoring: Boolean get() = restoreJob?.isActive == true

    fun restore() {
        val storage = coordinator ?: return
        restoreFailed = false
        val revision = ++generation
        val initial = actions.read()
        val resetId = initial.resetId
        actions.write(initial.copy(activity = ComposerDraftActivity.RESTORING))
        val worker =
            scope.launch(start = CoroutineStart.LAZY) {
                try {
                    val result = storage.load(key)
                    if (revision == generation && actions.read().resetId == resetId) {
                        if (result.isSuccess) {
                            actions.restored(result.getOrNull() != null)
                            actions.write((result.getOrNull() ?: initial).withResetId(resetId))
                        } else {
                            restoreFailed = true
                            actions.write(actions.read().copy(activity = ComposerDraftActivity.RESTORE_FAILED))
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
        if (!restoreFailed && !draft.inputBlocked) {
            coordinator?.schedule(key, actions.forStorage(draft), immediate)
        }
    }

    fun saveNow(draft: ComposerDraftState) {
        if (!restoreFailed && !draft.inputBlocked) {
            coordinator?.schedule(key, actions.forStorage(draft), immediate = true)
        }
    }

    fun clear(draft: ComposerDraftState) {
        if (restoreFailed) return
        generation++
        restoreJob?.cancel()
        coordinator?.schedule(key, actions.forStorage(draft), immediate = true)
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
            actions.write(draft.copy(activity = ComposerDraftActivity.CLOSING))
            val worker =
                scope.launch(start = CoroutineStart.LAZY) {
                    try {
                        if (coordinator.flush(key, actions.forStorage(draft))) {
                            onClosed()
                        } else {
                            actions.write(draft.copy(activity = ComposerDraftActivity.IDLE))
                            actions.fail("Не удалось сохранить черновик. Форма осталась открытой.")
                        }
                    } finally {
                        if (closeJob === coroutineContext[Job]) {
                            closeJob = null
                            val current = actions.read()
                            if (current.closing) actions.write(current.copy(activity = ComposerDraftActivity.IDLE))
                        }
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
    val forStorage: (ComposerDraftState) -> ComposerDraftState = { it },
    val restored: (Boolean) -> Unit = {},
)
