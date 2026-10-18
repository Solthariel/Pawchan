package com.solthariel.pawchan.core.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun UiToolbarSearchTransition(
    searchVisible: Boolean,
    modifier: Modifier = Modifier,
    label: String = "toolbar-search",
    content: @Composable (Boolean) -> Unit,
) {
    AnimatedContent(
        targetState = searchVisible,
        modifier = modifier,
        transitionSpec = { toolbarSearchTransition(targetState) },
        label = label,
    ) { active ->
        content(active)
    }
}

private fun toolbarSearchTransition(searchVisible: Boolean) =
    if (searchVisible) {
        (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
            slideOutHorizontally { width -> -width } + fadeOut(),
        )
    } else {
        (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
            slideOutHorizontally { width -> width } + fadeOut(),
        )
    }
