package com.solthariel.pawchan.core.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

/** Insets shared by the normal and archive thread lists. */
@Composable
fun threadPostListPadding(): PaddingValues =
    PaddingValues(
        start = FAST_SCROLL_CONTENT_INSET,
        top = LocalTopOverlayInset.current,
        end = FAST_SCROLL_CONTENT_INSET,
        bottom = LocalBottomOverlayInset.current + 8.dp,
    )
