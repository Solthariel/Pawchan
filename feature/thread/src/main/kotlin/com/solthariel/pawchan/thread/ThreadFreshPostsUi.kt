package com.solthariel.pawchan.thread

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.core.ui.CompactStatusNotice
import com.solthariel.pawchan.core.ui.LocalBottomOverlayInset
import com.solthariel.pawchan.core.ui.NoticeAction
import com.solthariel.pawchan.core.ui.postContainerShape
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Следит за списком постов и за тем, какие новые посты уже показались на экране. */
@Composable
internal fun rememberThreadFreshPosts(
    content: ThreadUiState.Content,
    list: LazyListState,
): ThreadFreshPosts {
    val fresh = remember(content.address) { ThreadFreshPosts() }
    LaunchedEffect(fresh, content.posts) { fresh.update(content.posts) }
    LaunchedEffect(fresh, list) {
        val scheduled = mutableSetOf<Long>()
        coroutineScope {
            snapshotFlow { list.layoutInfo.visibleItemsInfo.mapNotNull { it.key as? Long } }
                .collect { visible ->
                    visible.filter { it in fresh.unseen && scheduled.add(it) }.forEach { id ->
                        launch {
                            delay(SEEN_DELAY_MS)
                            fresh.seen(id)
                            scheduled.remove(id)
                        }
                    }
                }
        }
    }
    return fresh
}

/**
 * Подсветка нового поста поверх карточки; гаснет, когда пост увидели. Прозрачность читается только при
 * отрисовке, поэтому анимация не пересобирает карточку и её содержимое на каждом кадре.
 */
@Composable
internal fun BoxScope.FreshPostHighlight(fresh: Boolean) {
    val alpha = animateFloatAsState(if (fresh) HIGHLIGHT_ALPHA else 0f, tween(HIGHLIGHT_FADE_MS), label = "fresh-post")
    val color = MaterialTheme.colorScheme.primary
    val shape = postContainerShape()
    Box(
        Modifier
            .matchParentSize()
            .drawWithCache {
                val outline = shape.createOutline(size, layoutDirection, this)
                onDrawBehind {
                    val current = alpha.value
                    if (current > 0f) drawOutline(outline, color.copy(alpha = current))
                }
            },
    )
}

/**
 * Плашка о новых постах использует общий слой уведомлений поверх ленты,
 * не отнимает место у постов. Действие «Показать» ведёт к первому новому посту.
 */
@Composable
internal fun BoxScope.ThreadRefreshNoticeBar(
    fresh: ThreadFreshPosts,
    onShow: (Long) -> Unit,
) {
    val notice = fresh.notice.value
    CompactStatusNotice(
        notice?.message,
        eventKey = notice,
        action =
            notice?.firstNew?.let { id ->
                NoticeAction("Показать") {
                    fresh.dismiss()
                    onShow(id)
                }
            },
        bottomInset = LocalBottomOverlayInset.current + 12.dp,
        onDismiss = fresh::dismiss,
    )
}

private const val SEEN_DELAY_MS = 1_500L
private const val HIGHLIGHT_ALPHA = 0.12f
private const val HIGHLIGHT_FADE_MS = 900
