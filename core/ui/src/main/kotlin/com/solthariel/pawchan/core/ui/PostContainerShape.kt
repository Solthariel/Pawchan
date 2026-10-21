package com.solthariel.pawchan.core.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/** The post surface, interaction clipping and state overlays share one outline. */
fun postContainerShape(bubble: Boolean): Shape = if (bubble) BubblePostShape else RectangleShape

@Composable
fun postContainerShape(): Shape = postContainerShape(LocalPostDisplayPreferences.current.bubblePosts)

private val BubblePostShape = RoundedCornerShape(14.dp)
