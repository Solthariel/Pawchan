package com.solthariel.pawchan.composer

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.core.ui.UiOptionRow
import kotlin.math.roundToInt
import com.solthariel.pawchan.feature.composer.R.string as L

@Composable
internal fun AttachmentQualityControl(
    quality: Int,
    enabled: Boolean,
    onQualityChange: (Int) -> Unit,
) {
    val res = LocalResources.current

    UiOptionRow(res.getString(L.ui_quality)) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Slider(
                value = quality.coerceIn(MIN_QUALITY, MAX_QUALITY).toFloat(),
                onValueChange = { onQualityChange(it.roundToInt()) },
                valueRange = MIN_QUALITY.toFloat()..MAX_QUALITY.toFloat(),
                steps = QUALITY_SLIDER_STEPS,
                enabled = enabled,
                modifier =
                    Modifier.weight(1f).semantics {
                        contentDescription = res.getString(L.ui_jpeg_quality)
                        stateDescription = "$quality%"
                    },
            )
            Text("$quality%", modifier = Modifier.width(48.dp), textAlign = TextAlign.End)
        }
    }
}

private const val MIN_QUALITY = 1
private const val MAX_QUALITY = 100
private const val QUALITY_SLIDER_STEPS = MAX_QUALITY - MIN_QUALITY - 1
