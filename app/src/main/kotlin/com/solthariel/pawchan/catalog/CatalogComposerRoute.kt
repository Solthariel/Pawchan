package com.solthariel.pawchan.catalog

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.solthariel.pawchan.composer.BindComposerDraftLifecycle
import com.solthariel.pawchan.composer.ComposerFieldActions
import com.solthariel.pawchan.composer.ComposerFullScreenHost
import com.solthariel.pawchan.composer.NewThreadActions
import com.solthariel.pawchan.composer.NewThreadCaptchaActions
import com.solthariel.pawchan.composer.NewThreadScreen
import com.solthariel.pawchan.composer.NewThreadViewModel
import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.navigation.AppDependencies
import com.solthariel.pawchan.navigation.CatalogContentRoute
import com.solthariel.pawchan.navigation.CatalogNavigation
import com.solthariel.pawchan.navigation.chrome.AppBottomActions

@Composable
internal fun CatalogRoute(
    board: BoardAddress,
    dependencies: AppDependencies,
    navigation: CatalogNavigation,
    challengeRetryToken: Long,
    bottomActions: AppBottomActions,
) {
    var visible by rememberSaveable(board) { mutableStateOf(false) }
    var handledCreatedThread by rememberSaveable(board) { mutableStateOf<Long?>(null) }
    val factory =
        remember(board, dependencies) {
            NewThreadViewModel.factory(board, dependencies.postingRepository, dependencies.drafts, autoOpen = false)
        }
    val model: NewThreadViewModel = viewModel(key = "new-${board.site.value}-${board.board.value}", factory = factory)
    val state by model.state.collectAsStateWithLifecycle()
    val workspace =
        com.solthariel.pawchan.workspace.LocalWorkspace.current
            ?.controller
    val close = { model.close { visible = false } }
    val underlyingNavigation =
        remember(navigation, model) {
            navigation.copy(onNewThreadClick = {
                model.open()
                visible = true
            })
        }
    BindComposerDraftLifecycle(model::saveDraftNow)
    LaunchedEffect(visible) { if (visible) model.open() }
    LaunchedEffect(state.createdThread) {
        state.createdThread?.let {
            if (handledCreatedThread != it.thread.value) {
                handledCreatedThread = it.thread.value
                model.lastCreatedPost?.let { post -> workspace?.storeOwn(post) }
                visible = false
                navigation.onThreadClick(it)
            }
        }
    }
    ComposerFullScreenHost(
        visible = visible,
        onClose = close,
        content = {
            CatalogContentRoute(
                board,
                dependencies,
                underlyingNavigation,
                challengeRetryToken,
                bottomActions,
                onMediaVisibilityChanged = {},
            )
        },
        form = {
            NewThreadScreen(
                board.board,
                state,
                dependencies.mediaLoaders.imageLoader,
                newThreadActions(
                    model,
                    close,
                    com.solthariel.pawchan.composer.LocalComposerPreferences.current.hiddenFields,
                ),
            )
        },
    )
}

internal fun newThreadActions(
    model: NewThreadViewModel,
    onClose: () -> Unit,
    hidden: Set<com.solthariel.pawchan.domain.settings.ComposerField> = emptySet(),
): NewThreadActions =
    NewThreadActions(
        onClose,
        ComposerFieldActions(model::updateDraft, model::clearAll),
        NewThreadCaptchaActions(model::selectEmoji, model::reloadCaptcha),
        { model.submit(hidden) },
    )
