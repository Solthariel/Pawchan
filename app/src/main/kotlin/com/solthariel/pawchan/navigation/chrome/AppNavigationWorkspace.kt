package com.solthariel.pawchan.navigation.chrome

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBarsIgnoringVisibility
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.foundation.layout.systemBarsIgnoringVisibility
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.core.ui.LocalBottomOverlayInset
import com.solthariel.pawchan.core.ui.LocalScrollbarBottomInset

internal val LocalSharedThreadsBar = staticCompositionLocalOf { false }

/**
 * Floating navigation, with content and the edge scrollbar continuing behind it. The bar height is
 * published through [LocalBottomOverlayInset] so lists outside app (thread) can end above the bar.
 * [hideWithKeyboard]: under an open keyboard the bar is removed, as the thread did before.
 */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
internal fun AppNavigationWorkspace(
    actions: AppBottomActions,
    selected: AppBottomDestination?,
    modifier: Modifier = Modifier,
    hideWithKeyboard: Boolean = false,
    floatingTop: Boolean = false,
    content: @Composable () -> Unit,
) {
    val barVisible = !(hideWithKeyboard && WindowInsets.isImeVisible)
    val density = LocalDensity.current
    val statusInset = with(density) { WindowInsets.statusBarsIgnoringVisibility.getTop(density).toDp() }
    val navigationInset = with(density) { WindowInsets.navigationBarsIgnoringVisibility.getBottom(density).toDp() }
    val safeSides =
        if (floatingTop) WindowInsetsSides.Horizontal else WindowInsetsSides.Top + WindowInsetsSides.Horizontal
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .windowInsetsPadding(
                    WindowInsets.systemBarsIgnoringVisibility.only(safeSides),
                ),
    ) {
        CompositionLocalProvider(
            LocalBottomOverlayInset provides
                if (barVisible) APP_BOTTOM_BAR_HEIGHT + navigationInset else navigationInset,
            LocalScrollbarBottomInset provides navigationInset,
            com.solthariel.pawchan.core.ui.LocalFloatingTopEnabled provides floatingTop,
            com.solthariel.pawchan.core.ui.LocalFloatingTopStatusInset provides if (floatingTop) statusInset else 0.dp,
        ) {
            content()
        }
        if (barVisible && !LocalSharedThreadsBar.current) {
            AppBottomBar(
                actions = actions,
                selected = selected,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}
