package com.solthariel.pawchan.navigation.chrome

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsIgnoringVisibility
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.solthariel.pawchan.R
import com.solthariel.pawchan.core.ui.FAST_SCROLL_BAR_WIDTH
import com.solthariel.pawchan.core.ui.R as CoreUiR

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
internal fun AppBottomBar(
    actions: AppBottomActions,
    selected: AppBottomDestination?,
    modifier: Modifier = Modifier,
) {
    val surface = MaterialTheme.colorScheme.surface
    val surfaceAlpha = bottomBarSurfaceAlpha(LocalBottomBarTransparency.current)
    Box(modifier = modifier.fillMaxWidth()) {
        SystemNavigationBackground(surface.copy(alpha = surfaceAlpha))
        Row(
            modifier =
                Modifier
                    .navigationBarsPadding()
                    // Reserve the same edge on every screen, including those without a scrollbar.
                    // This keeps all five targets stable and leaves the workspace rail interactive.
                    .padding(end = FAST_SCROLL_BAR_WIDTH)
                    .fillMaxWidth()
                    .height(APP_BOTTOM_BAR_HEIGHT)
                    .background(
                        Brush.verticalGradient(
                            0f to surface.copy(alpha = 0f),
                            BOTTOM_FADE_FRACTION to surface.copy(alpha = surfaceAlpha),
                            1f to surface.copy(alpha = surfaceAlpha),
                        ),
                    ).padding(top = BOTTOM_FADE_HEIGHT),
        ) {
            AppBottomItem(
                AppBottomItemState(
                    stringResource(R.string.catalog),
                    R.drawable.ic_nav_catalog,
                    selected == AppBottomDestination.CATALOG,
                ),
                actions.onCatalogClick,
            )
            AppBottomItem(
                AppBottomItemState(
                    stringResource(R.string.threads_hub),
                    R.drawable.ic_nav_threads,
                    selected == AppBottomDestination.THREADS,
                    rememberAppUnreadRepliesBadge(),
                ),
                actions.onLibraryClick,
            )
            AppBottomItem(
                AppBottomItemState(
                    stringResource(R.string.boards),
                    R.drawable.ic_nav_boards,
                    selected == AppBottomDestination.BOARDS,
                ),
                actions.onBoardsClick,
            )
            AppBottomItem(
                AppBottomItemState(
                    stringResource(R.string.settings_short),
                    R.drawable.ic_nav_settings,
                    selected == AppBottomDestination.SETTINGS,
                ),
                actions.onSettingsClick,
            )
            AppBottomItem(
                AppBottomItemState(stringResource(R.string.write), CoreUiR.drawable.ic_rounded_edit_square),
                actions.onWriteClick,
            )
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun BoxScope.SystemNavigationBackground(color: androidx.compose.ui.graphics.Color) {
    Box(
        Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .windowInsetsBottomHeight(WindowInsets.navigationBarsIgnoringVisibility)
            .background(color),
    )
}

internal data class AppBottomActions(
    val onBoardsClick: () -> Unit,
    val onCatalogClick: () -> Unit,
    val onLibraryClick: () -> Unit,
    val onSettingsClick: () -> Unit,
    val onWriteClick: (() -> Unit)? = null,
)

internal enum class AppBottomDestination {
    BOARDS,
    CATALOG,
    THREADS,
    SETTINGS,
}

@Composable
internal fun BoxScope.AppBottomBadge(count: Int) {
    Box(
        Modifier
            .align(Alignment.Center)
            .offset(x = 14.dp, y = (-7).dp)
            .height(14.dp)
            .widthIn(min = 14.dp)
            .background(MaterialTheme.colorScheme.error, CircleShape)
            .padding(horizontal = 4.dp),
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

internal val APP_BOTTOM_BAR_HEIGHT = 56.dp
private val BOTTOM_FADE_HEIGHT = 8.dp
internal val BOTTOM_FADE_FRACTION = BOTTOM_FADE_HEIGHT.value / APP_BOTTOM_BAR_HEIGHT.value
