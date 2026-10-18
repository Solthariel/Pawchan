package com.solthariel.pawchan.thread

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.core.ui.LocalBottomOverlayInset
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
    LaunchedEffect(fresh.notice.value) {
        if (fresh.notice.value != null) {
            delay(NOTICE_DURATION_MS)
            fresh.dismiss()
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
    Box(
        Modifier
            .matchParentSize()
            .drawBehind {
                val current = alpha.value
                if (current > 0f) {
                    drawRoundRect(color.copy(alpha = current), cornerRadius = CornerRadius(HIGHLIGHT_CORNER.toPx()))
                }
            },
    )
}

/**
 * Плашка о новых постах — как в Dashchan: небольшая «таблетка» поверх ленты над нижней панелью,
 * не отнимает место у постов. Нажатие в любом месте ведёт к первому новому посту.
 */
@Composable
internal fun BoxScope.ThreadRefreshNoticeBar(
    fresh: ThreadFreshPosts,
    onShow: (Long) -> Unit,
) {
    val notice = fresh.notice.value
    // Последняя показанная плашка нужна, чтобы она не пропала мгновенно, а ушла с анимацией.
    var shown by remember { mutableStateOf(notice) }
    if (notice != null) shown = notice
    AnimatedVisibility(
        visible = notice != null,
        modifier =
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = LocalBottomOverlayInset.current + NOTICE_BOTTOM_GAP),
        enter = fadeIn(tween(NOTICE_ANIMATION_MS)) + slideInVertically(tween(NOTICE_ANIMATION_MS)) { it / 2 },
        exit = fadeOut(tween(NOTICE_ANIMATION_MS)) + slideOutVertically(tween(NOTICE_ANIMATION_MS)) { it / 2 },
    ) {
        shown?.let { current ->
            NoticePill(current) {
                fresh.dismiss()
                current.firstNew?.let(onShow)
            }
        }
    }
}

@Composable
private fun NoticePill(
    notice: ThreadRefreshNotice,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shadowElevation = 6.dp,
    ) {
        Row(
            Modifier.height(NOTICE_HEIGHT).padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                notice.message,
                Modifier.widthIn(max = NOTICE_TEXT_MAX_WIDTH),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (notice.firstNew != null) {
                VerticalDivider(
                    Modifier.padding(horizontal = 12.dp).height(16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
                Text(
                    "Показать",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

private val NOTICE_HEIGHT = 40.dp
private val NOTICE_BOTTOM_GAP = 12.dp
private val NOTICE_TEXT_MAX_WIDTH = 240.dp
private const val NOTICE_ANIMATION_MS = 200
private const val SEEN_DELAY_MS = 1_500L
private const val NOTICE_DURATION_MS = 6_000L
private const val HIGHLIGHT_ALPHA = 0.12f
private const val HIGHLIGHT_FADE_MS = 900
private val HIGHLIGHT_CORNER = 14.dp
