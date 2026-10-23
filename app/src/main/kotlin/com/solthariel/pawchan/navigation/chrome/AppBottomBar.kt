package com.solthariel.pawchan.navigation.chrome

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
import com.solthariel.pawchan.R
import com.solthariel.pawchan.core.ui.FAST_SCROLL_BAR_WIDTH
import com.solthariel.pawchan.core.ui.FAST_SCROLL_CONTENT_INSET
import com.solthariel.pawchan.core.ui.LocalPostDisplayPreferences
import com.solthariel.pawchan.settings.labelResource

@Composable
internal fun AppBottomBar(
    actions: AppBottomActions,
    selected: AppBottomDestination?,
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
                    .height(APP_BOTTOM_BAR_HEIGHT / 2)
                    .background(MaterialTheme.colorScheme.background),
        )
        AppBottomSurface(
            selected = selected,
            actions = actions,
        )
    }
}

@Composable
private fun AppBottomSurface(
    selected: AppBottomDestination?,
    actions: AppBottomActions,
) {
    val bubble = LocalPostDisplayPreferences.current.bubblePosts
    Box(
        modifier =
            Modifier
                .padding(horizontal = FAST_SCROLL_CONTENT_INSET)
                .fillMaxWidth()
                .height(APP_BOTTOM_BAR_HEIGHT),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = if (bubble) RoundedCornerShape(22.dp) else androidx.compose.ui.graphics.RectangleShape,
            color = MaterialTheme.colorScheme.surface,
            border = if (bubble) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null,
        ) {
            val unread = rememberAppUnreadRepliesBadge()
            Row(modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp)) {
                AppBottomItem(
                    R.string.catalog,
                    R.drawable.ic_nav_catalog,
                    selected == AppBottomDestination.CATALOG,
                    actions.onCatalogClick,
                )
                AppBottomItem(
                    R.string.threads_hub,
                    R.drawable.ic_nav_threads,
                    selected == AppBottomDestination.THREADS,
                    actions.onLibraryClick,
                    unread,
                )
                AppBottomItem(
                    R.string.boards,
                    R.drawable.ic_nav_boards,
                    selected == AppBottomDestination.BOARDS,
                    actions.onBoardsClick,
                )
                AppBottomItem(
                    R.string.settings_short,
                    R.drawable.ic_nav_settings,
                    selected == AppBottomDestination.SETTINGS,
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

@Composable
private fun RowScope.AppBottomItem(
    labelResource: Int,
    iconResource: Int,
    selected: Boolean,
    onClick: () -> Unit,
    badge: Int = 0,
) {
    val colors = appBottomColors(selected)
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
            if (badge > 0) AppBottomBadge(badge)
        }
    }
}

@Composable
private fun appBottomColors(selected: Boolean): AppBottomColors =
    if (selected) {
        AppBottomColors(
            content = MaterialTheme.colorScheme.onSecondaryContainer,
            container = MaterialTheme.colorScheme.secondaryContainer,
        )
    } else {
        AppBottomColors(
            content = MaterialTheme.colorScheme.onSurfaceVariant,
            container = Color.Transparent,
        )
    }

private data class AppBottomColors(
    val content: Color,
    val container: Color,
)

internal data class AppBottomActions(
    val onBoardsClick: () -> Unit,
    val onCatalogClick: () -> Unit,
    val onLibraryClick: () -> Unit,
    val onSettingsClick: () -> Unit,
)

internal enum class AppBottomDestination {
    BOARDS,
    CATALOG,
    THREADS,
    SETTINGS,
}

@Composable
private fun BoxScope.AppBottomBadge(count: Int) {
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

internal val APP_BOTTOM_BAR_HEIGHT = 36.dp
