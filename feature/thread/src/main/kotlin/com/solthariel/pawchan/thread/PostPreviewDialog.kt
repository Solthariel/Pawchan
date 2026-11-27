package com.solthariel.pawchan.thread

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.solthariel.pawchan.core.ui.PageLoadingIndicator
import com.solthariel.pawchan.core.ui.PostSeparator
import com.solthariel.pawchan.core.ui.modelLabel
import com.solthariel.pawchan.domain.model.PostId
import com.solthariel.pawchan.domain.model.PostLink
import com.solthariel.pawchan.media.thumbnail.ThumbnailLoading
import com.solthariel.pawchan.media.thumbnail.rememberThumbnailLoading
import com.solthariel.pawchan.feature.thread.R.string as L

@Composable
internal fun PostPreviewDialog(
    target: PostPreviewTarget,
    local: ThreadUiState.Content?,
    appearance: ThreadAppearance,
    bindings: PostPreviewBindings,
    resources: PostPreviewResources,
) {
    if (LocalPreviewLayer.current == null) {
        com.solthariel.pawchan.core.ui.ModalInlineLayer {
            PostPreviewDialogContent(target, local, appearance, bindings, resources)
        }
    } else {
        PostPreviewDialogContent(target, local, appearance, bindings, resources)
    }
}

@Composable
private fun PostPreviewDialogContent(
    target: PostPreviewTarget,
    local: ThreadUiState.Content?,
    appearance: ThreadAppearance,
    bindings: PostPreviewBindings,
    resources: PostPreviewResources,
) {
    val res = LocalResources.current

    val model: PostPreviewViewModel =
        viewModel(
            key = "post-preview",
            factory =
                PostPreviewViewModel.factory(
                    resources.repository,
                    resources.contentFilters,
                    target,
                    local,
                    showHiddenPosts = resources.showHiddenPosts,
                ),
        )
    DisposableEffect(model) { onDispose { model.cancelLoading() } }
    val state by model.state.collectAsStateWithLifecycle()
    var menuPost by remember(target) { mutableStateOf<ThreadPostUi?>(null) }
    val active = (LocalPreviewLayer.current?.depth ?: 0) == 0
    LaunchedEffect(active) { if (!active) menuPost = null }
    LaunchedEffect(target, local) { model.load(target, local) }
    val missing = postPreviewIsMissing(state, target)
    val context = LocalContext.current
    LaunchedEffect(missing, active) {
        if (missing && active) {
            bindings.onClose()
            android.widget.Toast
                .makeText(context, res.getString(L.ui_message_not_found), android.widget.Toast.LENGTH_SHORT)
                .show()
        }
    }
    if (missing) return
    PostPreviewWindow(
        state,
        target,
        PostPreviewPresentation(appearance, resources.thumbnails),
        bindings,
        { model.retry(target, local) },
    ) { menuPost = it }
    menuPost?.takeIf { active }?.let { post ->
        state.content?.let { PreviewPostMenu(post, it, resources.actions, bindings.onReply) { menuPost = null } }
    }
}

@Composable
internal fun PostPreviewWindow(
    state: PostPreviewState,
    target: PostPreviewTarget,
    presentation: PostPreviewPresentation,
    bindings: PostPreviewBindings,
    onRetry: () -> Unit,
    onMenu: (ThreadPostUi) -> Unit,
) {
    val holder = rememberSaveableStateHolder()
    SharedPostPreviewWindow(bindings.onClose) {
        holder.SaveableStateProvider("${target.address}/${target.postId}/${target.replies}") {
            val openThread =
                bindings.onOpenThread?.let { open ->
                    target.address?.let { address -> { open(address, target.postId) } }
                }
            Column {
                Box(Modifier.weight(1f, fill = false)) {
                    PreviewContent(state, presentation, bindings, onRetry, onMenu, target.replies)
                }
                openThread?.let { OpenPreviewThreadButton(it) }
            }
        }
    }
}

@Composable
private fun OpenPreviewThreadButton(onClick: () -> Unit) {
    val res = LocalResources.current

    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).heightIn(min = 56.dp),
        shape = CircleShape,
        colors =
            ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ),
    ) {
        Text(
            res.getString(L.ui_open_thread_1c3f),
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun SharedPostPreviewWindow(
    onClose: () -> Unit,
    content: @Composable () -> Unit,
) {
    PreviewWindowFrame(onClose) {
        PostSelectionHost(Modifier.fillMaxWidth(), content)
    }
}

@Composable
private fun PreviewContent(
    state: PostPreviewState,
    presentation: PostPreviewPresentation,
    bindings: PostPreviewBindings,
    onRetry: () -> Unit,
    onMenu: (ThreadPostUi) -> Unit,
    replies: Boolean,
) {
    val res = LocalResources.current

    val list = rememberLazyListState()
    val thumbnails = rememberThumbnailLoading(presentation.thumbnails, list)
    when {
        state.failure != null -> {
            Column(Modifier.padding(12.dp)) {
                Text(modelLabel(requireNotNull(state.failure)))
                TextButton(onClick = onRetry) { Text(res.getString(L.ui_retry)) }
            }
        }

        state.content == null -> {
            PageLoadingIndicator(
                description = res.getString(L.ui_loading_post_preview),
                modifier = Modifier.padding(12.dp).testTag("preview-loading"),
            )
        }

        state.posts.isEmpty() -> {
            Text(
                res.getString(if (replies) L.ui_no_replies_yet else L.ui_post_is_missing_or_deleted),
                Modifier.padding(12.dp),
            )
        }

        else -> {
            CompositionLocalProvider(
                LocalPostPreview provides bindings.onReference,
                LocalPostLinkAddress provides state.content?.address,
            ) {
                LazyColumn(
                    modifier = Modifier.testTag("preview-list"),
                    state = list,
                    contentPadding =
                        androidx.compose.foundation.layout
                            .PaddingValues(8.dp),
                ) {
                    itemsIndexed(
                        state.posts,
                        key = { _, post -> post.id },
                        contentType = { _, _ -> "preview-post" },
                    ) { index, post ->
                        Column {
                            PreviewPostCard(
                                post,
                                requireNotNull(state.content),
                                thumbnails,
                                presentation.appearance,
                                bindings,
                            ) { onMenu(post) }
                            PostSeparator(index, state.posts.lastIndex)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PreviewPostCard(
    post: ThreadPostUi,
    content: ThreadUiState.Content,
    thumbnails: ThumbnailLoading,
    appearance: ThreadAppearance,
    bindings: PostPreviewBindings,
    onMenu: () -> Unit,
) {
    var revealed by androidx.compose.runtime.saveable
        .rememberSaveable(post.id, post.metadata.contentFilterLabel) { mutableStateOf(false) }
    if (post.metadata.hidden && !revealed) {
        HiddenPostCard(post, onShow = { revealed = true }, onMenu = onMenu)
        return
    }
    ThreadPostCard(
        post,
        thumbnails,
        ThreadAttachmentActions(bindings.onOpen, bindings.onDownload, {}),
        onPostReference = { bindings.onReference(PostLink.Reference(requireNotNull(PostId.parse(it)))) },
        options =
            ThreadPostOptions(
                appearance = appearance,
                replyLabel = content.presentation.replyIndex[post.id]?.label,
                interactions =
                    ThreadPostInteractions(
                        onReplies = { bindings.onReplies(post.id) },
                        onMenu = onMenu,
                    ),
            ),
    )
}
