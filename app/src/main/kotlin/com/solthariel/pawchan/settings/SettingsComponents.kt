package com.solthariel.pawchan.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.solthariel.pawchan.core.ui.CompactSlider
import com.solthariel.pawchan.core.ui.UiForwardIndicator
import com.solthariel.pawchan.core.ui.russianPlural
import kotlin.math.roundToInt

/** Группы используют те же отступы и спокойную заливку, что и список досок. */
@Composable
internal fun SettingsGroupCard(group: SettingsGroup) {
    SettingsGroupHeading(group.title)
    SettingsGroupFrame {
        group.items.forEachIndexed { index, item ->
            if (index > 0) HorizontalDivider(Modifier.padding(horizontal = 16.dp))
            SettingsItemRow(item)
        }
    }
}

@Composable
internal fun SettingsItemRow(item: SettingsItem) {
    when (item) {
        is SettingsItem.Toggle -> {
            SettingsRow(
                item.title,
                item.summary,
                onClick = { item.onChange(!item.checked) },
                toggleState = item.checked,
            ) {
                com.solthariel.pawchan.core.ui
                    .CompactSwitch(checked = item.checked)
            }
        }

        is SettingsItem.Choice -> {
            ChoiceRow(item)
        }

        is SettingsItem.Scale -> {
            ScaleRow(item)
        }

        is SettingsItem.Action -> {
            ActionRow(item)
        }

        is SettingsItem.Custom -> {
            item.content()
        }
    }
}

@Composable
internal fun SettingsRow(
    title: String,
    summary: String?,
    titleColor: Color = Color.Unspecified,
    onClick: (() -> Unit)?,
    toggleState: Boolean? = null,
    trailing: @Composable () -> Unit = {},
) {
    Row(
        Modifier
            .fillMaxWidth()
            .then(
                when {
                    toggleState != null -> Modifier.toggleable(toggleState, role = Role.Switch) { onClick?.invoke() }
                    onClick != null -> Modifier.clickable(onClick = onClick)
                    else -> Modifier
                },
            ).heightIn(min = if (summary.isNullOrBlank()) SETTINGS_SINGLE_ROW_HEIGHT else SETTINGS_DOUBLE_ROW_HEIGHT)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = titleColor, style = MaterialTheme.typography.bodyLarge)
            summary?.let {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        trailing()
    }
}

@Composable
private fun ChoiceRow(item: SettingsItem.Choice) {
    var open by remember { mutableStateOf(false) }
    SettingsRow(item.title, item.options.getOrNull(item.selected), onClick = { open = true }) {
        UiForwardIndicator()
    }
    if (open) {
        com.solthariel.pawchan.core.ui.AppAlertDialog(
            onDismissRequest = { open = false },
            title = { Text(item.title) },
            text = {
                Column {
                    item.options.forEachIndexed { index, option ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .selectable(index == item.selected, role = Role.RadioButton) {
                                    item.onSelect(index)
                                    open = false
                                }.heightIn(min = 48.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = index == item.selected, onClick = null, Modifier.size(48.dp))
                            Text(option, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
        )
    }
}

/** Ползунок с живым примером: значение сохраняется, когда палец отпущен. */
@Composable
private fun ScaleRow(item: SettingsItem.Scale) {
    var value by remember(item.value) { mutableFloatStateOf(item.value.toFloat()) }
    val current = value.roundToInt()
    Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(item.title, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            Text(
                settingsScaleLabel(current, item.unit),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        CompactSlider(
            value = value,
            onValueChange = { value = it },
            onValueChangeFinished = { item.onChange(current) },
            valueRange = item.range.first.toFloat()..item.range.last.toFloat(),
            steps = (item.range.last - item.range.first - 1).coerceAtLeast(0),
        )
        if (item.preview) {
            Text(
                "Пример текста поста: так будут выглядеть сообщения в новых тредах.",
                fontSize = current.sp,
                lineHeight = (current * PREVIEW_LINE_HEIGHT).sp,
            )
        }
    }
}

@Composable
private fun ActionRow(item: SettingsItem.Action) {
    SettingsRow(
        item.title,
        item.summary,
        titleColor = if (item.danger) MaterialTheme.colorScheme.error else Color.Unspecified,
        onClick = item.onClick,
    ) {
        if (item.onClick != null && !item.danger) {
            UiForwardIndicator()
        }
    }
}

internal val SETTINGS_SINGLE_ROW_HEIGHT = 56.dp
internal val SETTINGS_DOUBLE_ROW_HEIGHT = 72.dp

private const val PREVIEW_LINE_HEIGHT = 1.35f

internal fun settingsScaleLabel(
    value: Int,
    unit: String,
): String = if (unit == "строк") russianPlural(value, "строки", "строк", "строк") else "$value $unit"
