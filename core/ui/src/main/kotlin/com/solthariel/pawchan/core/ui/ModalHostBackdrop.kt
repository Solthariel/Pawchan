package com.solthariel.pawchan.core.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath

/** Placement updates the panel bounds before this draw pass, including its very first frame. */
internal fun Modifier.modalHostBackdrop(
    state: ModalLayerState,
    origin: () -> Offset,
): Modifier =
    drawWithContent {
        drawContent()
        val dim = state.pageAlpha()
        if (dim > 0f) {
            val color = Color.Black.copy(alpha = dim)
            val panel = state.panel()
            if (panel == null) {
                drawRect(color)
            } else {
                val offset = -origin()
                val path =
                    Path().apply {
                        addRoundRect(RoundRect(panel.bounds.translate(offset), CornerRadius(panel.radius)))
                        panel.additional?.let { addRect(it.translate(offset)) }
                    }
                clipPath(path, ClipOp.Difference) { drawRect(color) }
            }
        }
    }
