package com.solthariel.pawchan.thread

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.solthariel.pawchan.thread.watch.BindThreadWatcherLifecycle

/** Reply intent, not a composer controller or a mutable thread state. */
data class ThreadReplyRequest(
    val closed: Boolean,
    val insertion: String?,
)

/** Permission handling stays with the app; the foreground watcher stays here. */
data class ThreadFeatureActions(
    val onBack: () -> Unit,
    val onChallenge: (String) -> Unit,
    val onReply: (ThreadReplyRequest) -> Unit,
    val watcherToggle: @Composable (Boolean, () -> Unit) -> (() -> Unit) = { _, toggle -> toggle },
)

@Composable
fun ThreadFeatureScreen(
    session: ThreadSession,
    host: ThreadScreenHost,
    actions: ThreadFeatureActions,
    storage: ThreadPostStorage? = null,
    contentHost: @Composable (@Composable () -> Unit) -> Unit = { it() },
) {
    LifecycleStartEffect(session) {
        session.refreshFavorite()
        // Удержанная сессия показывается сразу; сеть — только если наблюдатель видел новые посты.
        session.reopen()
        onStopOrDispose {}
    }
    BindThreadWatcherLifecycle(session.watcher)
    val state by session.state.collectAsStateWithLifecycle()
    val watcher by session.watcher.state.collectAsStateWithLifecycle()
    val toggle = actions.watcherToggle(watcher.enabled, session::toggleWatcher)
    val reply: (String?) -> Unit = { insertion ->
        actions.onReply(ThreadReplyRequest((state as? ThreadUiState.Content)?.closed != false, insertion))
    }
    ThreadScreen(
        state = state,
        actions =
            ThreadActions(
                onBack = actions.onBack,
                onRefresh = session::refresh,
                onFavoriteToggle = {
                    (session.state.value as? ThreadUiState.Content)?.let { session.setFavorite(!it.isFavorite) }
                },
                onChallenge = actions.onChallenge,
                onWatcherToggle = toggle,
                onReplyTo = reply,
                history = session.history,
            ),
        presentation =
            ThreadPresentation(
                watcherState = watcher,
                reports = session.reports,
                postContext =
                    ThreadPostContext(
                        session.sessionAddress,
                        session.postPreferences,
                        storage,
                        session::revealContentFilter,
                    ),
            ),
        host = host,
        requestPost = session::ensurePost,
        contentHost = contentHost,
    )
}
