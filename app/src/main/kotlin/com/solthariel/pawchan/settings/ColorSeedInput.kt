package com.solthariel.pawchan.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.domain.settings.DEFAULT_THEME_SEED
import kotlin.math.roundToInt

@Composable
internal fun ColorSeedInput(
    hex: String,
    onChange: (String) -> Unit,
) {
    val parsed = parseSeedHex(hex)
    val keyboard = LocalSoftwareKeyboardController.current
    Column {
        OutlinedTextField(
            value = hex,
            onValueChange = onChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Цвет HEX") },
            supportingText = { Text("Шесть цифр: #RRGGBB") },
            singleLine = true,
            isError = parsed == null,
            keyboardOptions =
                KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                    autoCorrectEnabled = false,
                    keyboardType = KeyboardType.Ascii,
                    imeAction = ImeAction.Done,
                ),
            keyboardActions = KeyboardActions(onDone = { keyboard?.hide() }),
        )
        val seed = parsed ?: DEFAULT_THEME_SEED
        listOf("Красный" to RED_SHIFT, "Зелёный" to GREEN_SHIFT, "Синий" to 0).forEach { (label, shift) ->
            ColorChannelSlider(label, (seed shr shift) and CHANNEL_MAX) { channel ->
                val replaced = (seed and (CHANNEL_MAX shl shift).inv()) or (channel shl shift)
                onChange(seedHex(replaced))
            }
        }
    }
}

@Composable
private fun ColorChannelSlider(
    label: String,
    value: Int,
    onChange: (Int) -> Unit,
) {
    Column(Modifier.padding(top = 8.dp)) {
        Row {
            Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            Text(value.toString(), style = MaterialTheme.typography.bodyMedium)
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onChange(it.roundToInt()) },
            valueRange = 0f..CHANNEL_MAX.toFloat(),
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = label },
        )
    }
}

private const val CHANNEL_MAX = 255
private const val RED_SHIFT = 16
private const val GREEN_SHIFT = 8
