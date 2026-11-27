package com.solthariel.pawchan.navigation.chrome

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
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
 * On every screen the bar leaves at once when the keyboard opens and fades back only after it has
 * fully closed ([keyboardShown] is injectable for tests). Lists keep their bottom inset, nothing jumps.
 */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
internal fun AppNavigationWorkspace(
    actions: AppBottomActions,
    selected: AppBottomDestination?,
    modifier: Modifier = Modifier,
    floatingTop: Boolean = false,
    keyboardShown: Boolean = keyboardShown(),
    content: @Composable () -> Unit,
) {
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
            LocalBottomOverlayInset provides APP_BOTTOM_BAR_HEIGHT + navigationInset,
            LocalScrollbarBottomInset provides navigationInset,
            com.solthariel.pawchan.core.ui.LocalFloatingTopEnabled provides floatingTop,
            com.solthariel.pawchan.core.ui.LocalFloatingTopStatusInset provides if (floatingTop) statusInset else 0.dp,
        ) {
            content()
        }
        if (!LocalSharedThreadsBar.current) {
            KeyboardAwareBar(keyboardShown, Modifier.align(Alignment.BottomCenter)) {
                AppBottomBar(actions = actions, selected = selected)
            }
        }
    }
}

/** Без анимации ухода: под клавиатурой панели нет и нажать её нельзя; назад — простым появлением. */
@Composable
internal fun KeyboardAwareBar(
    keyboardShown: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    AnimatedVisibility(
        visible = bottomBarVisible(keyboardShown),
        modifier = modifier,
        enter = fadeIn(tween(BAR_RETURN_FADE_MS)),
        exit = ExitTransition.None,
    ) { content() }
}

internal fun bottomBarVisible(keyboardShown: Boolean): Boolean = !keyboardShown

/** Клавиатура открыта или ещё уезжает: пока её высота не ноль, панель не возвращается. */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
internal fun keyboardShown(): Boolean {
    val density = LocalDensity.current
    return WindowInsets.isImeVisible || WindowInsets.ime.getBottom(density) > 0
}

internal const val BAR_RETURN_FADE_MS = 180
