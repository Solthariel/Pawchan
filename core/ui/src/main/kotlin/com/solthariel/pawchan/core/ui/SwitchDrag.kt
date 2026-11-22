package com.solthariel.pawchan.core.ui

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

internal data class SwitchDrag(
    val modifier: Modifier,
    val position: Float?,
)

@Composable
internal fun rememberSwitchDrag(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
): SwitchDrag {
    var position by remember { mutableStateOf<Float?>(null) }
    val currentChecked = rememberUpdatedState(checked)
    val currentChange = rememberUpdatedState(onCheckedChange)
    val travel = with(LocalDensity.current) { 16.dp.toPx() }
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val input =
        if (onCheckedChange == null) {
            Modifier
        } else {
            Modifier.pointerInput(travel, rtl) {
                detectHorizontalDragGestures(
                    onDragStart = { position = if (currentChecked.value) 1f else 0f },
                    onDragCancel = { position = null },
                    onDragEnd = {
                        val target = switchDragChecked(position ?: if (currentChecked.value) 1f else 0f)
                        position = null
                        if (target != currentChecked.value) currentChange.value?.invoke(target)
                    },
                    onHorizontalDrag = { change, amount ->
                        change.consume()
                        val delta = if (rtl) -amount else amount
                        position = ((position ?: 0f) + delta / travel).coerceIn(0f, 1f)
                    },
                )
            }
        }
    return SwitchDrag(input, position)
}

internal fun switchDragChecked(position: Float): Boolean = position >= SWITCH_MIDPOINT

private const val SWITCH_MIDPOINT = .5f
