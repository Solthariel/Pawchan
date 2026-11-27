package com.solthariel.pawchan.settings

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.core.ui.UiIcon
import com.solthariel.pawchan.core.ui.UiIconKind
import com.solthariel.pawchan.R.string as L

/** Строка списка с порядком: подпись и стрелки ↑↓; у краёв стрелка неактивна. */
@Composable
internal fun ReorderRow(
    title: String,
    index: Int,
    count: Int,
    onMove: (Int) -> Unit,
    leading: @Composable () -> Unit = {},
    trailing: @Composable () -> Unit = {},
) {
    val res = LocalResources.current
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
        leading()
        Text(title, Modifier.weight(1f).padding(start = 12.dp), style = MaterialTheme.typography.bodyLarge)
        trailing()
        MoveButton(UiIconKind.UP, res.getString(L.ui_move_up_named, title), index > 0) { onMove(-1) }
        MoveButton(UiIconKind.DOWN, res.getString(L.ui_move_down_named, title), index < count - 1) { onMove(1) }
    }
}

@Composable
private fun MoveButton(
    icon: UiIconKind,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val tint = MaterialTheme.colorScheme.onSurfaceVariant
    val described = Modifier.semantics { contentDescription = description }
    IconButton(onClick = onClick, enabled = enabled, modifier = described) {
        UiIcon(icon, if (enabled) tint else tint.copy(alpha = DISABLED_ARROW_ALPHA), Modifier.size(24.dp))
    }
}

/** Сдвиг элемента на [delta] позиций; за край списка не уходит. */
internal fun <T> List<T>.moved(
    index: Int,
    delta: Int,
): List<T> {
    val target = index + delta
    if (index !in indices || target !in indices) return this
    return toMutableList().apply { add(target, removeAt(index)) }
}

private const val DISABLED_ARROW_ALPHA = 0.38f
