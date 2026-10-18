package com.solthariel.pawchan.thread

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.solthariel.pawchan.core.ui.PostSeparator
import com.solthariel.pawchan.domain.model.PostId
import com.solthariel.pawchan.domain.model.PostLink
import com.solthariel.pawchan.media.thumbnail.ThumbnailLoading
import com.solthariel.pawchan.media.thumbnail.rememberThumbnailLoading

@Composable
internal fun PostPreviewDialog(
    target: PostPreviewTarget,
    local: ThreadUiState.Content?,
    appearance: ThreadAppearance,
    bindings: PostPreviewBindings,
    resources: PostPreviewResources,
) {
    val model: PostPreviewViewModel =
        viewModel(
            key = "post-preview",
            factory = PostPreviewViewModel.factory(resources.repository, resources.contentFilters, target, local),
        )
    DisposableEffect(model) { onDispose { model.cancelLoading() } }
    val state by model.state.collectAsStateWithLifecycle()
    var menuPost by remember(target) { mutableStateOf<ThreadPostUi?>(null) }
    val active = (LocalPreviewLayer.current?.depth ?: 0) == 0
    LaunchedEffect(active) { if (!active) menuPost = null }
    LaunchedEffect(target, local) { model.load(target, local) }
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
    PreviewWindowFrame(bindings.onClose) {
        PostSelectionHost(Modifier.fillMaxWidth()) {
            holder.SaveableStateProvider("${target.address}/${target.postId}/${target.replies}") {
                val openThread =
                    bindings.onOpenThread?.let { open ->
                        target.address?.let { address -> { open(address, target.postId) } }
                    }
                Column {
                    Box(Modifier.weight(1f, fill = false)) {
                        PreviewContent(state, presentation, bindings, onRetry, onMenu, target.replies)
                    }
                    openThread?.let {
                        TextButton(onClick = it, modifier = Modifier.align(Alignment.End).padding(end = 4.dp)) {
                            Text("Открыть тред")
                        }
                    }
                }
            }
        }
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
    val bubble = com.solthariel.pawchan.core.ui.LocalPostDisplayPreferences.current.bubblePosts
    val list = rememberLazyListState()
    val thumbnails = rememberThumbnailLoading(presentation.thumbnails, list)
    when {
        state.failure != null -> {
            Column(Modifier.padding(12.dp)) {
                Text(requireNotNull(state.failure))
                TextButton(onClick = onRetry) { Text("Повторить") }
            }
        }

        state.content == null -> {
            CircularProgressIndicator(Modifier.padding(12.dp).testTag("preview-loading"))
        }

        state.posts.isEmpty() -> {
            Text(if (replies) "Ответов пока нет" else "Пост отсутствует или удалён", Modifier.padding(12.dp))
        }

        else -> {
            CompositionLocalProvider(
                LocalPostPreview provides bindings.onReference,
                LocalPostLinkAddress provides state.content?.address,
            ) {
                LazyColumn(
                    modifier = Modifier.testTag("preview-list"),
                    state = list,
                    verticalArrangement =
                        Arrangement.spacedBy(
                            if (bubble) 8.dp else 0.dp,
                        ),
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
                framed = com.solthariel.pawchan.core.ui.LocalPostDisplayPreferences.current.bubblePosts,
                replyLabel = content.presentation.replyIndex[post.id]?.label,
                interactions = ThreadPostInteractions(onReplies = { bindings.onReplies(post.id) }, onMenu = onMenu),
            ),
    )
}
