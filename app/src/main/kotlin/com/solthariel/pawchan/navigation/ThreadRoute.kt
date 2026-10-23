package com.solthariel.pawchan.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.solthariel.pawchan.composer.BindComposerDraftLifecycle
import com.solthariel.pawchan.composer.ComposerFieldActions
import com.solthariel.pawchan.composer.ReplyActions
import com.solthariel.pawchan.composer.ReplyComposerController
import com.solthariel.pawchan.composer.ReplyComposerViewModel
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.infrastructure.thread.AppThreadPostStorage
import com.solthariel.pawchan.infrastructure.thread.AppThreadWorkspaceEvents
import com.solthariel.pawchan.infrastructure.thread.rememberAppThreadScreenHost
import com.solthariel.pawchan.navigation.chrome.AppBottomActions
import com.solthariel.pawchan.navigation.chrome.AppNavigationWorkspace
import com.solthariel.pawchan.thread.ThreadFeatureActions
import com.solthariel.pawchan.thread.ThreadFeatureScreen
import com.solthariel.pawchan.thread.ThreadPersistence
import com.solthariel.pawchan.thread.ThreadSession
import com.solthariel.pawchan.watch.rememberWatcherNotificationToggle
import com.solthariel.pawchan.workspace.LocalWorkspace
import com.solthariel.pawchan.workspace.WorkspaceController
import com.solthariel.pawchan.workspace.WorkspaceThreadRepository

@Composable
internal fun ThreadRoute(
    address: ThreadAddress,
    dependencies: AppDependencies,
    onBack: () -> Unit,
    onChallenge: (String) -> Unit,
    challengeRetryToken: Long,
    bottomActions: AppBottomActions,
) {
    // Та же обёртка, что в каталоге: панель плавает поверх ленты, посты уходят под неё.
    AppNavigationWorkspace(actions = bottomActions, selected = null, hideWithKeyboard = true) {
        ThreadRouteContent(address, dependencies, onBack, onChallenge, challengeRetryToken)
    }
}

@Composable
private fun ThreadRouteContent(
    address: ThreadAddress,
    dependencies: AppDependencies,
    onBack: () -> Unit,
    onChallenge: (String) -> Unit,
    challengeRetryToken: Long,
) {
    val workspace = LocalWorkspace.current?.controller
    val storage = remember(workspace) { workspace?.let(::AppThreadPostStorage) }
    val model = rememberThreadSession(address, dependencies)
    val reply = rememberThreadReply(address, dependencies, model)
    BindThreadOfflineMode(model, workspace?.offlineAddress)
    BindPendingReply(address, workspace, reply)
    BindComposerDraftLifecycle(reply::saveDraftNow)
    LaunchedEffect(challengeRetryToken) {
        if (challengeRetryToken > 0L) model.refresh()
    }
    val replyState by reply.state.collectAsStateWithLifecycle()
    ThreadFeatureScreen(
        session = model,
        host = rememberAppThreadScreenHost(dependencies.mediaLoaders, workspace, dependencies.appSettings),
        actions =
            ThreadFeatureActions(
                onBack = onBack,
                onChallenge = onChallenge,
                onReply = { request -> reply.open(request.closed, request.insertion) },
                watcherToggle = { enabled, toggle -> rememberWatcherNotificationToggle(enabled, toggle) },
            ),
        storage = storage,
        contentHost = { content ->
            ThreadReplyHost(
                address.board.board,
                replyState,
                dependencies.mediaLoaders,
                replyActions(reply, com.solthariel.pawchan.composer.LocalComposerPreferences.current.hiddenFields),
                content,
            )
        },
    )
}

@Composable
private fun rememberThreadSession(
    address: ThreadAddress,
    dependencies: AppDependencies,
): ThreadSession {
    val binding = LocalWorkspace.current
    val workspace = binding?.controller
    val arhivach = binding?.archives?.arhivach
    val factory =
        remember(address, dependencies, workspace, arhivach) {
            ThreadSession.factory(
                address,
                WorkspaceThreadRepository(dependencies.threadRepository, workspace, arhivach?.let { it::load }),
                ThreadPersistence(
                    dependencies.library,
                    dependencies.watches,
                    dependencies.postPreferences,
                    workspace?.let(::AppThreadWorkspaceEvents),
                    workspace?.history,
                    dependencies.appSettings,
                    dependencies.filterStatistics,
                ),
            )
        }
    val owner = rememberThreadSessionOwner(address, checkNotNull(LocalViewModelStoreOwner.current))
    return viewModel(viewModelStoreOwner = owner, key = threadSessionKey("thread", address), factory = factory)
}

@Composable
private fun rememberThreadReply(
    address: ThreadAddress,
    dependencies: AppDependencies,
    thread: ThreadSession,
): ReplyComposerController {
    val factory =
        remember(address, dependencies, thread) {
            ReplyComposerViewModel.factory(
                address,
                dependencies.postingRepository,
                thread::onPostSubmitted,
                dependencies.drafts,
            )
        }
    // Черновик ответа живёт вместе с удерживаемой сессией треда.
    val owner = rememberThreadSessionOwner(address, checkNotNull(LocalViewModelStoreOwner.current))
    val model: ReplyComposerViewModel =
        viewModel(viewModelStoreOwner = owner, key = threadSessionKey("reply", address), factory = factory)
    return model.controller
}

@Composable
private fun BindPendingReply(
    address: ThreadAddress,
    workspace: WorkspaceController?,
    reply: ReplyComposerController,
) {
    val pendingReply = workspace?.pendingReply?.collectAsStateWithLifecycle()?.value
    LaunchedEffect(pendingReply, reply) {
        if (pendingReply?.address == address) {
            reply.open(false, pendingReply.text)
            workspace?.pendingReply?.value = null
        }
    }
}

private fun replyActions(
    reply: ReplyComposerController,
    hidden: Set<com.solthariel.pawchan.domain.settings.ComposerField>,
): ReplyActions =
    ReplyActions(
        fields = ComposerFieldActions(reply::updateDraft, reply::clearAll),
        onEmojiSelected = reply::selectEmoji,
        onReloadCaptcha = reply::reloadCaptcha,
        onSubmit = { reply.submit(hidden) },
        onClose = reply::close,
    )

@Composable
private fun BindThreadOfflineMode(
    viewModel: ThreadSession,
    offline: ThreadAddress?,
) {
    var previousOffline by remember(viewModel) { mutableStateOf(offline) }
    LaunchedEffect(viewModel, offline) {
        if (previousOffline != offline) {
            previousOffline = offline
            viewModel.refresh()
        }
    }
}
