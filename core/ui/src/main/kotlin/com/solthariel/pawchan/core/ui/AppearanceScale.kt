package com.solthariel.pawchan.core.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlin.math.roundToInt

data class AppearanceScale(
    val range: IntRange,
    val default: Int,
    val unit: String,
)

@Composable
fun AppearanceSlider(
    label: String,
    value: Int,
    scale: AppearanceScale,
    onChange: (Int) -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, modifier = Modifier.fillMaxWidth())
        CompactSlider(
            value = value.toFloat(),
            onValueChange = { onChange(it.roundToInt()) },
            valueRange = scale.range.first.toFloat()..scale.range.last.toFloat(),
            steps = scale.range.last - scale.range.first - 1,
        )
    }
}
