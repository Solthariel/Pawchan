package com.solthariel.pawchan.composer

import com.solthariel.pawchan.domain.drafts.ComposerDraftKey
import com.solthariel.pawchan.domain.drafts.ComposerDraftRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import kotlin.coroutines.coroutineContext

/** Owned by AppStorageViewModel; calls/maps are Main-confined. Work survives route disposal, not app destruction. */
class ComposerDraftCoordinator internal constructor(
    private val scope: CoroutineScope,
    private val repository: ComposerDraftRepository,
    private val nowMillis: () -> Long = { System.nanoTime() / NANOS_PER_MILLISECOND },
    private val restoreDispatcher: kotlinx.coroutines.CoroutineDispatcher = Dispatchers.IO,
) {
    private val pending = mutableMapOf<ComposerDraftKey, Job>()
    private val dirtySince = mutableMapOf<ComposerDraftKey, Long>()
    private val notice = MutableStateFlow<ComposerDraftNotice?>(null)
    internal val notices = notice.asStateFlow()
    internal val pendingWrites: Int get() = pending.size

    internal fun schedule(
        key: ComposerDraftKey,
        draft: ComposerDraftState,
        immediate: Boolean = false,
    ) {
        val first = dirtySince.getOrPut(key, nowMillis)
        pending.remove(key)?.cancel()
        val worker =
            scope.launch(start = CoroutineStart.LAZY) {
                try {
                    val remaining = (MAX_SAVE_DELAY - (nowMillis() - first)).coerceAtLeast(0)
                    delay(if (immediate) 0 else minOf(SAVE_DELAY, remaining))
                    if (write(key, draft)) dirtySince.remove(key)
                } finally {
                    if (pending[key] === coroutineContext[Job]) pending.remove(key)
                }
            }
        pending[key] = worker
        worker.start()
    }

    internal suspend fun load(key: ComposerDraftKey): Result<ComposerDraftState?> =
        try {
            val payload = repository.load(key)
            Result.success(withContext(restoreDispatcher) { payload?.toComposerDraft() })
        } catch (failure: IOException) {
            notice.value =
                ComposerDraftNotice(
                    "Не удалось восстановить черновик. Сохранённые данные не удалены.",
                    failure,
                )
            Result.failure(failure)
        } catch (failure: IllegalArgumentException) {
            notice.value =
                ComposerDraftNotice(
                    "Не удалось восстановить черновик. Сохранённые данные не удалены.",
                    failure,
                )
            Result.failure(failure)
        }

    internal suspend fun flush(
        key: ComposerDraftKey,
        draft: ComposerDraftState,
    ): Boolean {
        cancelPending(key)
        return write(key, draft)
    }

    internal suspend fun discard(key: ComposerDraftKey): Boolean {
        cancelPending(key)
        return try {
            repository.remove(key)
            true
        } catch (failure: IOException) {
            notice.value = ComposerDraftNotice("Не удалось очистить сохранённый черновик.", failure)
            false
        }
    }

    private suspend fun cancelPending(key: ComposerDraftKey) {
        pending.remove(key)?.cancelAndJoin()
        dirtySince.remove(key)
    }

    private suspend fun write(
        key: ComposerDraftKey,
        draft: ComposerDraftState,
    ): Boolean =
        try {
            repository.save(key, draft.toStoredDraft())
            true
        } catch (failure: IOException) {
            notice.value = ComposerDraftNotice("Не удалось сохранить черновик. Проверьте свободное место.", failure)
            false
        } catch (failure: IllegalArgumentException) {
            notice.value = ComposerDraftNotice("Черновик превышает безопасный размер хранилища.", failure)
            false
        }

    companion object {
        fun create(
            scope: CoroutineScope,
            repository: ComposerDraftRepository,
        ): ComposerDraftCoordinator = ComposerDraftCoordinator(scope, repository)
    }
}

private const val SAVE_DELAY = 2000L
private const val MAX_SAVE_DELAY = 10000L
private const val NANOS_PER_MILLISECOND = 1000000L

internal data class ComposerDraftNotice(
    val message: String,
    val cause: Exception,
)
