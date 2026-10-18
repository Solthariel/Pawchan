package com.solthariel.pawchan.media

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp

class AttachmentDialogContent(
    val header: @Composable () -> Unit,
    val footer: @Composable () -> Unit,
    val media: @Composable () -> Unit,
    val aspectRatio: Float,
    val previewHeightLimit: Dp = Dp.Unspecified,
)
