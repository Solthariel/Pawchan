package com.solthariel.pawchan.security

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Composable
internal fun PinUnlockKeypad(
    digits: String,
    state: AppLockState,
    biometricEnabled: Boolean,
    onChange: (String) -> Unit,
    onFingerprint: () -> Unit,
) {
    val enabled = state.ready && !state.busy && state.retrySeconds == 0
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9")).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                row.forEach { digit ->
                    PinUnlockKey(digit, enabled && digits.length < PIN_LENGTH, { onChange(digits + digit) }) {
                        Text(digit, style = MaterialTheme.typography.headlineMedium)
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            PinUnlockKey("Удалить цифру", enabled && digits.isNotEmpty(), { onChange(digits.dropLast(1)) }) {
                PinDeleteGlyph(Modifier.size(24.dp))
            }
            PinUnlockKey("0", enabled && digits.length < PIN_LENGTH, { onChange(digits + "0") }) {
                Text("0", style = MaterialTheme.typography.headlineMedium)
            }
            PinUnlockBiometric(digits, state, biometricEnabled, onFingerprint)
        }
    }
}

@Composable
private fun PinUnlockBiometric(
    digits: String,
    state: AppLockState,
    biometricEnabled: Boolean,
    onFingerprint: () -> Unit,
) {
    val modifier = Modifier.size(72.dp).testTag("pin-unlock-action")
    if (digits.isEmpty() && biometricEnabled) {
        PinUnlockKey("Отпечаток пальца", state.ready && !state.busy, onFingerprint, modifier) {
            FingerprintGlyph(Modifier.size(30.dp))
        }
    } else {
        Box(modifier)
    }
}

@Composable
private fun PinUnlockKey(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.size(72.dp).semantics { contentDescription = label },
        shape = CircleShape,
        contentPadding = PaddingValues(0.dp),
        colors =
            ButtonDefaults.buttonColors(
                containerColor = Color.White.copy(alpha = 0.13f),
                contentColor = Color.White,
                disabledContainerColor = Color.White.copy(alpha = 0.06f),
                disabledContentColor = Color.White.copy(alpha = 0.38f),
            ),
    ) { content() }
}
