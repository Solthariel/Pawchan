package com.solthariel.pawchan.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.R.string as L

@Composable
internal fun ColorSchemePreview(colors: ColorScheme) {
    val res = LocalResources.current

    MaterialTheme(colorScheme = colors) {
        Surface(
            color = colors.background,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(res.getString(L.ui_preview), style = MaterialTheme.typography.titleMedium)
                Card(colors = CardDefaults.cardColors(containerColor = colors.surfaceContainer)) {
                    Column(Modifier.fillMaxWidth().padding(12.dp)) {
                        Text(res.getString(L.ui_card_example), color = colors.onSurface)
                        Text(res.getString(L.ui_this_is_what_the_text), color = colors.onSurfaceVariant)
                    }
                }
                Button(onClick = {}, enabled = true) { Text(res.getString(L.ui_button_example)) }
            }
        }
    }
}
