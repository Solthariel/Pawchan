package com.solthariel.pawchan.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.solthariel.pawchan.composer.BindComposerDraftLifecycle
import com.solthariel.pawchan.composer.ComposerFieldActions
import com.solthariel.pawchan.composer.NewThreadActions
import com.solthariel.pawchan.composer.NewThreadCaptchaActions
import com.solthariel.pawchan.composer.NewThreadScreen
import com.solthariel.pawchan.composer.NewThreadViewModel
import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.workspace.LocalWorkspace

@Composable
internal fun NewThreadRoute(
    board: BoardAddress,
    dependencies: AppDependencies,
    onBack: () -> Unit,
    onCreated: (ThreadAddress) -> Unit,
) {
    val factory =
        remember(board, dependencies.postingRepository) {
            NewThreadViewModel.factory(board, dependencies.postingRepository, dependencies.drafts)
        }
    val viewModel: NewThreadViewModel = viewModel(factory = factory)
    val state by viewModel.state.collectAsStateWithLifecycle()
    BindComposerDraftLifecycle(viewModel::saveDraftNow)
    LaunchedEffect(viewModel) { viewModel.open() }
    val workspace = LocalWorkspace.current?.controller
    LaunchedEffect(state.createdThread) {
        viewModel.state.value.createdThread?.let {
            viewModel.lastCreatedPost?.let { post -> workspace?.storeOwn(post) }
            onCreated(it)
        }
    }
    val hiddenFields = com.solthariel.pawchan.composer.LocalComposerPreferences.current.hiddenFields
    NewThreadScreen(
        board = board.board,
        state = state,
        imageLoader = dependencies.mediaLoaders.imageLoader,
        actions =
            NewThreadActions(
                onBack = { viewModel.close(onBack) },
                fields =
                    ComposerFieldActions(
                        onChange = viewModel::updateDraft,
                        onClearAll = viewModel::clearAll,
                    ),
                captcha =
                    NewThreadCaptchaActions(
                        onEmojiSelected = viewModel::selectEmoji,
                        onReloadCaptcha = viewModel::reloadCaptcha,
                    ),
                onSubmit = { viewModel.submit(hiddenFields) },
            ),
    )
}
