package com.solthariel.pawchan.settings

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.core.ui.modelLabel
import com.solthariel.pawchan.domain.settings.ThemePalette
import com.solthariel.pawchan.domain.settings.currentPalette
import com.solthariel.pawchan.ui.pawchanColorScheme
import com.solthariel.pawchan.ui.seedColor

@Composable
internal fun PaletteChoiceList(
    state: SettingsUiState,
    onSelect: (ThemePalette) -> Unit,
) {
    val system = pawchanColorScheme(state.theme, state.themeColors.copy(palette = ThemePalette.SYSTEM))
    val available = availableThemePalettes(Build.VERSION.SDK_INT)
    val selected =
        state.themeColors.palette
            .currentPalette()
            .takeIf { it in available } ?: ThemePalette.LAVENDER
    Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        available.forEach { palette ->
            val color =
                when (palette) {
                    ThemePalette.SYSTEM -> system.primary
                    ThemePalette.CUSTOM -> Color(state.themeColors.customSeed or OPAQUE_COLOR_ALPHA)
                    else -> Color(palette.seedColor() or OPAQUE_COLOR_ALPHA)
                }
            PaletteChoiceRow(palette, palette == selected, color) { onSelect(palette) }
        }
    }
}

@Composable
private fun PaletteChoiceRow(
    palette: ThemePalette,
    selected: Boolean,
    color: Color,
    onClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp).selectable(selected, role = Role.RadioButton, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected, onClick = null, modifier = Modifier.size(48.dp))
        Text(
            modelLabel(palette.paletteLabel()),
            Modifier.weight(1f).padding(end = 8.dp),
            style = MaterialTheme.typography.bodyLarge,
        )
        Box(
            Modifier
                .size(24.dp)
                .background(color, CircleShape)
                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
        )
    }
}

private const val OPAQUE_COLOR_ALPHA = -0x1000000
