package com.solthariel.pawchan.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.core.ui.UiChoiceSegments
import com.solthariel.pawchan.core.ui.UiIconKind
import com.solthariel.pawchan.core.ui.UiToolbarButton
import com.solthariel.pawchan.R.string as L
import com.solthariel.pawchan.core.ui.AppAlertDialog as AlertDialog
import com.solthariel.pawchan.core.ui.ModalDropdownMenu as DropdownMenu

@Composable
internal fun UsagePeriodOptions(
    selected: Int,
    select: (Int) -> Unit,
) {
    val res = LocalResources.current

    Row(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
        UiChoiceSegments(
            listOf(
                res.getString(L.ui_today),
                res.getString(L.ui_month),
                res.getString(L.ui_all_time),
            ),
            selected,
            true,
            wrapLabels = true,
            onSelect = select,
        )
    }
}

@Composable
internal fun UsageStatisticsMenu(
    postCountEnabled: Boolean,
    onPostCount: (Boolean) -> Unit,
    reset: () -> Unit,
) {
    val res = LocalResources.current

    var open by remember { mutableStateOf(false) }
    Box {
        UiToolbarButton(UiIconKind.MORE, res.getString(L.ui_statistics_menu), onClick = { open = true })
        DropdownMenu(open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { Text(res.getString(L.ui_postcount_statistics_on_sending)) },
                trailingIcon = { androidx.compose.material3.Checkbox(postCountEnabled, onCheckedChange = null) },
                onClick = {
                    onPostCount(!postCountEnabled)
                    open = false
                },
            )
            DropdownMenuItem(text = { Text(res.getString(L.ui_reset_statistics)) }, onClick = {
                open = false
                reset()
            })
        }
    }
}

@Composable
internal fun UsageResetDialog(
    close: () -> Unit,
    confirm: () -> Unit,
) {
    val res = LocalResources.current

    AlertDialog(
        onDismissRequest = close,
        title = { Text(res.getString(L.ui_reset_statistics_8e37)) },
        text = { Text(res.getString(L.ui_all_counters_will_be_reset)) },
        confirmButton = { TextButton(onClick = confirm) { Text(res.getString(L.ui_reset)) } },
        dismissButton = { TextButton(onClick = close) { Text(res.getString(L.ui_cancel)) } },
    )
}
