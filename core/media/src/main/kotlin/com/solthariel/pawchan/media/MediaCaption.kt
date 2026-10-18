package com.solthariel.pawchan.media

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MediaCaption(
    typeAndSize: String,
    dimensions: String?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = CAPTION_ALPHA))
            .padding(horizontal = 3.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicText(
            text = typeAndSize,
            modifier = Modifier.weight(1f),
            style = CAPTION_STYLE,
            maxLines = 1,
            softWrap = false,
            autoSize = CAPTION_AUTO_SIZE,
        )
        if (dimensions != null) {
            BasicText(
                text = dimensions,
                modifier = Modifier.weight(1f),
                style = CAPTION_STYLE.copy(textAlign = TextAlign.End),
                maxLines = 1,
                softWrap = false,
                autoSize = CAPTION_AUTO_SIZE,
            )
        }
    }
}

private const val CAPTION_ALPHA = 0.68f
private val CAPTION_STYLE = TextStyle(color = Color.White, fontSize = 8.sp, lineHeight = 9.sp)
private val CAPTION_AUTO_SIZE = TextAutoSize.StepBased(minFontSize = 5.sp, maxFontSize = 8.sp, stepSize = 0.25.sp)
