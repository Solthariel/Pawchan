package com.solthariel.pawchan.workspace

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavController
import com.solthariel.pawchan.domain.model.DomainFailure
import com.solthariel.pawchan.domain.model.DomainOutcome
import com.solthariel.pawchan.domain.model.PostId
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadSnapshot
import com.solthariel.pawchan.domain.thread.ThreadRepository
import com.solthariel.pawchan.domain.thread.mergeThreadHistory
import com.solthariel.pawchan.navigation.AppRoute
import com.solthariel.pawchan.navigation.toBoardAddress
import com.solthariel.pawchan.navigation.toThreadAddress

internal fun String?.validRestorableRoute(): String? =
    this?.takeIf {
        it == AppRoute.BOARDS || it == AppRoute.LIBRARY || it == AppRoute.SETTINGS ||
            it.matches(Regex("(?:catalog|archive)/[a-zA-Z0-9_-]+/[a-zA-Z0-9_-]+")) ||
            it.matches(Regex("thread/[a-zA-Z0-9_-]+/[a-zA-Z0-9_-]+/[1-9][0-9]*"))
    }

@Composable
internal fun BindWorkspaceRoute(
    navController: NavController,
    workspace: WorkspaceController?,
) {
    LaunchedEffect(navController, workspace) {
        navController.currentBackStackEntryFlow.collect { entry ->
            val route =
                when (entry.destination.route) {
                    AppRoute.THREAD -> entry.arguments?.toThreadAddress()?.let(AppRoute::thread)
                    AppRoute.CATALOG -> entry.arguments?.toBoardAddress()?.let(AppRoute::catalog)
                    AppRoute.ARCHIVE -> entry.arguments?.toBoardAddress()?.let(AppRoute::archive)
                    else -> entry.destination.route.validRestorableRoute()
                }
            if (route != null) workspace?.change { it.copy(lastRoute = route) }
        }
    }
}

internal class WorkspaceThreadRepository(
    private val remote: ThreadRepository,
    private val workspace: WorkspaceController?,
    private val arhivach: (suspend (ThreadAddress, String) -> DomainOutcome<ThreadSnapshot>)? = null,
) : com.solthariel.pawchan.domain.thread.ReportingThreadRepository,
    com.solthariel.pawchan.domain.thread.CachedThreadRepository {
    override suspend fun isThreadDeleted(address: ThreadAddress): Boolean = remote.isThreadDeleted(address)

    override suspend fun cachedThread(address: ThreadAddress): ThreadSnapshot? {
        if (workspace?.offlineAddress == address) return savedCopy(address)?.readOnlySnapshot()
        // Тред из памяти чтения отдаётся сразу, без похода к сохранённым копиям на диске.
        return workspace?.reading?.snapshot(address) ?: savedCopy(address) ?: workspace?.history?.load(address)
    }

    override suspend fun isCachedThreadCurrent(address: ThreadAddress): Boolean {
        val owner = workspace ?: return false
        return owner.offlineAddress != address && owner.reading.snapshot(address) != null
    }

    /**
     * Ответ сервера сливается с историей треда: пропавшие посты остаются удалёнными (как в Dashchan).
     * Без сети показывается сохранённая копия или история.
     */
    override suspend fun loadThread(address: ThreadAddress): DomainOutcome<ThreadSnapshot> {
        val copy = savedCopy(address)
        if (workspace?.offlineAddress == address && copy != null) return DomainOutcome.Success(copy.readOnlySnapshot())
        val history = workspace?.history
        return when (val outcome = remote.loadThread(address)) {
            is DomainOutcome.Success -> {
                val merged = mergeThreadHistory(history?.load(address), outcome.value)
                history?.save(merged)
                DomainOutcome.Success(merged)
            }

            is DomainOutcome.Failure -> {
                val cached = copy ?: history?.load(address)
                when {
                    cached == null -> arhivachCopy(address, outcome)
                    outcome.reason == DomainFailure.NotFound -> outcome
                    else -> DomainOutcome.Success(cached)
                }
            }
        }
    }

    /** Тред удалён и нигде не сохранён локально — пробуем копию на Архиваче, если ссылка известна. */
    private suspend fun arhivachCopy(
        address: ThreadAddress,
        failure: DomainOutcome.Failure,
    ): DomainOutcome<ThreadSnapshot> {
        val link =
            workspace
                ?.state
                ?.value
                ?.marks
                ?.arhivach
                ?.get(address)
        val load = arhivach
        if (failure.reason != DomainFailure.NotFound || link == null || load == null) return failure
        return (load(address, link) as? DomainOutcome.Success) ?: failure
    }

    private fun savedCopy(address: ThreadAddress): ThreadSnapshot? =
        workspace
            ?.state
            ?.value
            ?.copies
            ?.firstOrNull { it.snapshot.address == address }
            ?.snapshot

    override suspend fun reportPost(
        address: ThreadAddress,
        post: PostId,
        comment: String,
    ): com.solthariel.pawchan.domain.posting.PostingOutcome<Unit> {
        val reporter = remote as? com.solthariel.pawchan.domain.thread.ReportingThreadRepository
        return reporter?.reportPost(address, post, comment)
            ?: com.solthariel.pawchan.domain.posting.PostingOutcome.Failure(
                com.solthariel.pawchan.domain.posting.PostingFailure
                    .Rejected("Жалобы не поддерживаются"),
            )
    }
}

internal fun ThreadSnapshot.readOnlySnapshot(): ThreadSnapshot = withFlags(flags.copy(closed = true, archived = true))
