package com.solthariel.pawchan.catalog

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.solthariel.pawchan.R
import com.solthariel.pawchan.core.ui.FAST_SCROLL_BAR_WIDTH
import com.solthariel.pawchan.core.ui.FAST_SCROLL_CONTENT_INSET
import com.solthariel.pawchan.core.ui.LocalPostDisplayPreferences
import com.solthariel.pawchan.settings.labelResource
import com.solthariel.pawchan.threads.unreadReplies
import com.solthariel.pawchan.workspace.LocalWorkspace

@Composable
internal fun CatalogBottomBar(
    actions: CatalogBottomActions,
    selected: CatalogBottomDestination?,
    modifier: Modifier = Modifier,
    edgeScrollbar: Boolean = false,
) {
    Box(modifier = modifier.fillMaxWidth().navigationBarsPadding()) {
        Box(
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(end = if (edgeScrollbar) FAST_SCROLL_BAR_WIDTH else 0.dp)
                    .fillMaxWidth()
                    .height(CATALOG_BOTTOM_BAR_HEIGHT / 2)
                    .background(MaterialTheme.colorScheme.background),
        )
        CatalogBottomSurface(
            selected = selected,
            actions = actions,
        )
    }
}

@Composable
private fun CatalogBottomSurface(
    selected: CatalogBottomDestination?,
    actions: CatalogBottomActions,
) {
    val bubble = LocalPostDisplayPreferences.current.bubblePosts
    Box(
        modifier =
            Modifier
                .padding(horizontal = FAST_SCROLL_CONTENT_INSET)
                .fillMaxWidth()
                .height(CATALOG_BOTTOM_BAR_HEIGHT),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            border = if (bubble) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null,
        ) {
            val unread = rememberUnreadRepliesBadge()
            Row(modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp)) {
                CatalogBottomItem(
                    R.string.catalog,
                    R.drawable.ic_nav_catalog,
                    selected == CatalogBottomDestination.CATALOG,
                    actions.onCatalogClick,
                )
                CatalogBottomItem(
                    R.string.threads_hub,
                    R.drawable.ic_nav_threads,
                    selected == CatalogBottomDestination.THREADS,
                    actions.onLibraryClick,
                    unread,
                )
                CatalogBottomItem(
                    R.string.boards,
                    R.drawable.ic_nav_boards,
                    selected == CatalogBottomDestination.BOARDS,
                    actions.onBoardsClick,
                )
                CatalogBottomItem(
                    R.string.settings_short,
                    R.drawable.ic_nav_settings,
                    selected == CatalogBottomDestination.SETTINGS,
                    actions.onSettingsClick,
                )
            }
        }
        if (!bubble) {
            HorizontalDivider(
                Modifier.align(Alignment.TopCenter).padding(horizontal = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant,
            )
        }
    }
}

/** Бейдж «Тредов» — только непрочитанные ответы вам. */
@Composable
private fun rememberUnreadRepliesBadge(): Int {
    val state =
        LocalWorkspace.current
            ?.controller
            ?.state
            ?.collectAsStateWithLifecycle()
            ?.value ?: return 0
    return remember(state.posts, state.replies, state.marks.dismissedReplies) { unreadReplies(state) }
}

@Composable
private fun RowScope.CatalogBottomItem(
    labelResource: Int,
    iconResource: Int,
    selected: Boolean,
    onClick: () -> Unit,
    badge: Int = 0,
) {
    val colors = catalogBottomColors(selected)
    val label = stringResource(labelResource)
    Box(
        modifier = Modifier.weight(1f).fillMaxHeight(),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp, vertical = 5.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(colors.container)
                    .clickable(onClick = onClick)
                    .semantics { contentDescription = if (badge > 0) "$label, ответов: $badge" else label },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(iconResource),
                contentDescription = null,
                tint = colors.content,
                modifier = Modifier.size(24.dp),
            )
            if (badge > 0) CatalogBottomBadge(badge)
        }
    }
}

@Composable
private fun catalogBottomColors(selected: Boolean): CatalogBottomColors =
    if (selected) {
        CatalogBottomColors(
            content = MaterialTheme.colorScheme.onSecondaryContainer,
            container = MaterialTheme.colorScheme.secondaryContainer,
        )
    } else {
        CatalogBottomColors(
            content = MaterialTheme.colorScheme.onSurfaceVariant,
            container = Color.Transparent,
        )
    }

private data class CatalogBottomColors(
    val content: Color,
    val container: Color,
)

internal data class CatalogBottomActions(
    val onBoardsClick: () -> Unit,
    val onCatalogClick: () -> Unit,
    val onLibraryClick: () -> Unit,
    val onSettingsClick: () -> Unit,
)

internal enum class CatalogBottomDestination {
    BOARDS,
    CATALOG,
    THREADS,
    SETTINGS,
}

@Composable
private fun BoxScope.CatalogBottomBadge(count: Int) {
    Box(
        Modifier
            .align(Alignment.Center)
            .offset(x = 14.dp, y = (-7).dp)
            .height(14.dp)
            .widthIn(min = 14.dp)
            .background(MaterialTheme.colorScheme.error, CircleShape)
            .padding(horizontal = 3.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            if (count > BADGE_LIMIT) "$BADGE_LIMIT+" else count.toString(),
            color = MaterialTheme.colorScheme.onError,
            fontSize = 9.sp,
            lineHeight = 10.sp,
            maxLines = 1,
        )
    }
}

private const val BADGE_LIMIT = 99

internal val CATALOG_BOTTOM_BAR_HEIGHT = 36.dp
