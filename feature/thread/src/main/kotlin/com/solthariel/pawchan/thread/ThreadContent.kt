package com.solthariel.pawchan.thread

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.core.ui.FAST_SCROLL_CONTENT_INSET
import com.solthariel.pawchan.core.ui.LocalBottomOverlayInset
import com.solthariel.pawchan.core.ui.PawchanFastScrollbar
import com.solthariel.pawchan.core.ui.PostSeparator
import com.solthariel.pawchan.media.thumbnail.ThumbnailLoading
import com.solthariel.pawchan.media.thumbnail.rememberThumbnailLoading
import kotlinx.coroutines.launch

@Composable
internal fun ThreadContent(
    content: ThreadUiState.Content,
    list: LazyListState,
    bindings: ThreadContentBindings,
) {
    val bubble = com.solthariel.pawchan.core.ui.LocalPostDisplayPreferences.current.bubblePosts
    val loading = rememberThumbnailLoading(bindings.loader, list)
    val fresh = rememberThreadFreshPosts(content, list)
    val scope = rememberCoroutineScope()
    val smooth = rememberThreadSmoothScroll()
    val reference: (Long) -> Unit = { id ->
        bindings.tools.previews = bindings.tools.previews + PostPreviewTarget(null, id)
    }
    // Плавающая нижняя панель лежит поверх ленты: последний пост докручивается выше неё.
    val overlay = LocalBottomOverlayInset.current
    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = list,
            modifier = Modifier.fillMaxSize(),
            contentPadding =
                PaddingValues(
                    start = FAST_SCROLL_CONTENT_INSET,
                    end = FAST_SCROLL_CONTENT_INSET,
                    bottom = overlay + 8.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(if (bubble) 8.dp else 0.dp),
        ) {
            itemsIndexed(
                content.posts,
                key = { _, post -> post.id },
                contentType = { _, _ -> "thread-post" },
            ) { index, post ->
                Column(Modifier.readingReturnMarker(post.id == bindings.tools.readingReturn?.postId)) {
                    if (post.metadata.hidden) {
                        HiddenPostCard(
                            post,
                            onShow = { bindings.postActions.onShowHidden(post) },
                            onMenu = { bindings.tools.postMenuId = post.id },
                        )
                    } else {
                        ThreadPostItem(post, content, bindings, loading, reference, post.id in fresh.unseen)
                    }
                    PostSeparator(index, content.posts.lastIndex)
                }
            }
        }
        PawchanFastScrollbar(
            state = list,
            inputGesture = loading.gesture,
            markers = content.readingReturnScrollMarkers(bindings.tools.readingReturn),
            mode = com.solthariel.pawchan.core.ui.FastScrollMode.ITEMS,
            modifier = Modifier.align(Alignment.CenterEnd),
        )
        ThreadRefreshNoticeBar(fresh) { id ->
            content.presentation.postPositions[id]?.let { index -> scope.launch { smooth.scrollTo(list, index) } }
        }
    }
}

@Composable
private fun ThreadPostItem(
    post: ThreadPostUi,
    content: ThreadUiState.Content,
    bindings: ThreadContentBindings,
    loading: ThumbnailLoading,
    reference: (Long) -> Unit,
    fresh: Boolean,
) {
    Box {
        ThreadPostCard(
            post,
            loading,
            ThreadAttachmentActions(
                bindings.onOpen,
                bindings.onDownload,
                bindings.onMediaMenu,
            ),
            reference,
            ThreadPostOptions(
                appearance = bindings.appearance,
                selecting = bindings.tools.selecting,
                selected = post.id in bindings.tools.selected,
                changes = post.metadata.changes,
                replyLabel = content.presentation.replyIndex[post.id]?.label,
                interactions =
                    ThreadPostInteractions(
                        onReplies = { bindings.tools.replyPreviewPostId = post.id },
                        onSelect = { bindings.tools.toggleSelection(post.id) },
                        onMenu = { bindings.tools.postMenuId = post.id },
                        onQuote = { text ->
                            bindings.postActions.onQuote(selectedPostQuote(post.id, text))
                        },
                    ),
            ),
        )
        FreshPostHighlight(fresh)
    }
}
