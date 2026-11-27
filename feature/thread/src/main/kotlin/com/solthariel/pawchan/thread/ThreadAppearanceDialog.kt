package com.solthariel.pawchan.thread

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.core.ui.AppDialog
import com.solthariel.pawchan.core.ui.AppDialogStyle
import com.solthariel.pawchan.core.ui.AppearanceScale
import com.solthariel.pawchan.core.ui.AppearanceSlider
import com.solthariel.pawchan.core.ui.NO_POST_COLLAPSE
import com.solthariel.pawchan.feature.thread.R
import com.solthariel.pawchan.feature.thread.R.string as L

@Composable
internal fun ThreadAppearanceDialog(
    value: ThreadAppearance,
    onChange: (ThreadAppearance) -> Unit,
    onClose: () -> Unit,
) {
    val res = LocalResources.current

    AppDialog(onClose) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(AppDialogStyle.contentPadding),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(res.getString(L.ui_appearance_0983), style = MaterialTheme.typography.titleLarge)
            AppearanceSlider(
                res.getString(L.ui_post_text_sp, value.textSize),
                value.textSize,
                AppearanceScale(MIN_TEXT_SIZE..MAX_TEXT_SIZE, DEFAULT_THREAD_TEXT_SIZE, "sp"),
            ) { onChange(value.copy(textSize = it)) }
            AppearanceSlider(
                if (value.maxLines == NO_POST_COLLAPSE) {
                    res.getString(L.ui_do_not_collapse)
                } else {
                    res.getQuantityString(R.plurals.ui_collapse_after_lines, value.maxLines, value.maxLines)
                },
                value.maxLines,
                AppearanceScale(MIN_POST_LINES..NO_POST_COLLAPSE, DEFAULT_THREAD_MAX_LINES, "строк"),
            ) { onChange(value.copy(maxLines = it)) }
            TextButton(onClick = { onChange(ThreadAppearance()) }) { Text(res.getString(L.ui_default)) }
        }
    }
}

private const val MIN_TEXT_SIZE = 6
private const val MAX_TEXT_SIZE = 18
private const val MIN_POST_LINES = 10
