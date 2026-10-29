package com.solthariel.pawchan.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.core.ui.UiChoiceSegments

/** Shared Material segments; stack before labels run out of room at large text sizes. */
@Composable
internal fun FilterOptionGroup(
    title: String,
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit,
) {
    val measurer = rememberTextMeasurer()
    val style = MaterialTheme.typography.labelLarge
    val density = LocalDensity.current
    val widestWord =
        options.flatMap { it.second.split(' ') }.maxOf {
            measurer.measure(AnnotatedString(it), style).size.width
        }
    val minimum = with(density) { widestWord.toDp() } + 32.dp
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge)
        BoxWithConstraints(Modifier.fillMaxWidth().selectableGroup().testTag("filter-options-$title")) {
            if (maxWidth >= minimum * options.size + 4.dp * (options.size - 1)) {
                UiChoiceSegments(
                    options.map { it.second },
                    options.indexOfFirst { it.first == selected },
                    true,
                    wrapLabels = true,
                ) {
                    onSelect(options[it].first)
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    options.forEach { (value, label) ->
                        UiChoiceSegments(listOf(label), if (selected == value) 0 else -1, true, wrapLabels = true) {
                            onSelect(value)
                        }
                    }
                }
            }
        }
    }
}
