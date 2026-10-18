package com.solthariel.pawchan.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.solthariel.pawchan.composer.BindComposerDraftLifecycle
import com.solthariel.pawchan.composer.ComposerFieldActions
import com.solthariel.pawchan.composer.NewThreadActions
import com.solthariel.pawchan.composer.NewThreadCaptchaActions
import com.solthariel.pawchan.composer.NewThreadScreen
import com.solthariel.pawchan.composer.NewThreadViewModel
import com.solthariel.pawchan.domain.boards.BoardDirectoryRepository
import com.solthariel.pawchan.domain.boards.BoardPreferencesRepository
import com.solthariel.pawchan.domain.boards.PostingAccessRepository
import com.solthariel.pawchan.domain.catalog.CatalogRepository
import com.solthariel.pawchan.domain.library.ThreadLibraryRepository
import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.posting.PostingRepository
import com.solthariel.pawchan.domain.postpreferences.PostPreferencesRepository
import com.solthariel.pawchan.domain.settings.ThemePreference
import com.solthariel.pawchan.domain.thread.ThreadRepository
import com.solthariel.pawchan.workspace.LocalWorkspace
import kotlinx.coroutines.flow.map

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
