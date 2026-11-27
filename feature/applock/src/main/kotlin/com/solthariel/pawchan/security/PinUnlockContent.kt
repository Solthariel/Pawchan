package com.solthariel.pawchan.security

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.core.ui.UiIcon
import com.solthariel.pawchan.core.ui.UiIconKind
import com.solthariel.pawchan.feature.applock.R
import com.solthariel.pawchan.feature.applock.R.string as L

/** No protected content, screenshots, or saved PIN digits are used for this background. */
@Composable
internal fun PinUnlockContent(
    state: AppLockState,
    fingerprintAvailable: Boolean,
    onPin: (String) -> Unit,
    onFingerprint: () -> Unit,
) {
    val input = rememberPinUnlockInput(state)
    val digits = input.digits
    Box(Modifier.fillMaxSize().background(UNLOCK_BACKGROUND)) {
        Box(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                Modifier.widthIn(max = 320.dp).fillMaxWidth().testTag("pin-unlock-content"),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                UiIcon(UiIconKind.LOCK, Color.White, Modifier.size(28.dp))
                PinUnlockHeading(digits, state)
                PinUnlockKeypad(
                    digits = digits,
                    state = state,
                    biometricEnabled = state.configuration.biometric && fingerprintAvailable,
                    onKey = { input.press(it, state.ready && !state.busy && state.retrySeconds == 0, onPin) },
                    onFingerprint = onFingerprint,
                )
            }
        }
    }
}

@Composable
private fun PinUnlockHeading(
    digits: String,
    state: AppLockState,
) {
    val res = LocalResources.current

    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            res.getString(L.ui_enter_pin),
            modifier = Modifier.fillMaxWidth(),
            color = Color.White,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        Text(
            "●".repeat(digits.length) + "○".repeat(PIN_LENGTH - digits.length),
            Modifier.semantics {
                contentDescription =
                    res.getString(L.ui_digits_entered_of, digits.length, PIN_LENGTH)
            },
            color = Color.White,
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            when {
                !state.ready -> {
                    res.getString(L.ui_loading)
                }

                state.busy -> {
                    res.getString(L.ui_checking_e17a)
                }

                state.retrySeconds > 0 -> {
                    res.getQuantityString(R.plurals.ui_retry_in_sec, state.retrySeconds, state.retrySeconds)
                }

                else -> {
                    state.error.orEmpty()
                }
            },
            Modifier.fillMaxWidth(),
            color = if (state.error != null && !state.busy && state.ready) UNLOCK_ERROR else Color.White,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            minLines = 2,
            maxLines = 2,
        )
    }
}

private val UNLOCK_BACKGROUND =
    Brush.linearGradient(listOf(Color(UNLOCK_TOP), Color(UNLOCK_MIDDLE), Color(UNLOCK_BOTTOM)))
private val UNLOCK_ERROR = Color(UNLOCK_ERROR_COLOR)

private const val UNLOCK_TOP = 0xFF111B29
private const val UNLOCK_MIDDLE = 0xFF304651
private const val UNLOCK_BOTTOM = 0xFF191D2C
private const val UNLOCK_ERROR_COLOR = 0xFFFFB4AB
