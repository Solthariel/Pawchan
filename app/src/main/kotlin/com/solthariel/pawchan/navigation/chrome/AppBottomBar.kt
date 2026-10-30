package com.solthariel.pawchan.navigation.chrome

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.R
import com.solthariel.pawchan.core.ui.FAST_SCROLL_BAR_WIDTH
import com.solthariel.pawchan.core.ui.FAST_SCROLL_CONTENT_INSET
import com.solthariel.pawchan.core.ui.LocalPostDisplayPreferences

@Composable
internal fun AppBottomBar(
    actions: AppBottomActions,
    selected: AppBottomDestination?,
    modifier: Modifier = Modifier,
    edgeScrollbar: Boolean = false,
) {
    val unread = rememberAppUnreadRepliesBadge()
    Box(modifier.fillMaxWidth().navigationBarsPadding()) {
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(end = if (edgeScrollbar) FAST_SCROLL_BAR_WIDTH else 0.dp)
                .fillMaxWidth()
                .height(APP_BOTTOM_BAR_HEIGHT / 2)
                .background(MaterialTheme.colorScheme.background),
        )
        AppBottomNavigation(actions, selected, unread)
    }
}

@Composable
private fun AppBottomNavigation(
    actions: AppBottomActions,
    selected: AppBottomDestination?,
    unread: Int,
) {
    val bubble = LocalPostDisplayPreferences.current.bubblePosts
    val shape = if (bubble) MaterialTheme.shapes.extraLarge else RectangleShape
    val colors = MaterialTheme.colorScheme
    NavigationBar(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = FAST_SCROLL_CONTENT_INSET)
                .height(APP_BOTTOM_BAR_HEIGHT)
                .clip(shape)
                .then(if (bubble) Modifier.border(1.dp, colors.outlineVariant, shape) else Modifier),
        windowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = colors.surfaceContainer,
        tonalElevation = 0.dp,
    ) {
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

@Composable
private fun RowScope.AppBottomItem(
    labelResource: Int,
    iconResource: Int,
    selected: Boolean,
    onClick: () -> Unit,
    badge: Int = 0,
) {
    val label = stringResource(labelResource)
    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        modifier =
            Modifier.semantics {
                contentDescription = if (badge > 0) "$label, ответов: $badge" else label
            },
        icon = {
            BadgedBox(
                badge = { if (badge > 0) Badge { Text(if (badge > MAX_VISIBLE_BADGE) "99+" else badge.toString()) } },
            ) {
                Icon(painterResource(iconResource), null, Modifier.size(24.dp))
            }
        },
        label = { Text(label, maxLines = 1) },
    )
}

internal data class AppBottomActions(
    val onBoardsClick: () -> Unit,
    val onCatalogClick: () -> Unit,
    val onLibraryClick: () -> Unit,
    val onSettingsClick: () -> Unit,
)

internal enum class AppBottomDestination { BOARDS, CATALOG, THREADS, SETTINGS }

internal val APP_BOTTOM_BAR_HEIGHT = 80.dp

private const val MAX_VISIBLE_BADGE = 99
