package com.solthariel.pawchan.composer

import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay

internal fun ComposerCaptchaState.remainingSeconds(nowMillis: Long): Long? =
    deadlineElapsedMillis?.let { ((it - nowMillis).coerceAtLeast(0L) + MILLIS_PER_SECOND - 1L) / MILLIS_PER_SECOND }

internal fun ComposerCaptchaState.isExpired(nowMillis: Long): Boolean = remainingSeconds(nowMillis) == 0L

internal fun captchaTimerLabel(seconds: Long): String =
    "${seconds / SECONDS_PER_MINUTE}:${(seconds % SECONDS_PER_MINUTE).toString().padStart(2, '0')}"

@Composable
internal fun rememberCaptchaRemainingSeconds(captcha: ComposerCaptchaState): Long? {
    val owner = LocalLifecycleOwner.current
    val deadline = captcha.deadlineElapsedMillis
    var seconds by remember(deadline) { mutableStateOf(captcha.remainingSeconds(SystemClock.elapsedRealtime())) }
    LaunchedEffect(deadline, owner) {
        if (deadline == null) return@LaunchedEffect
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            do {
                seconds = captcha.remainingSeconds(SystemClock.elapsedRealtime())
                delay(MILLIS_PER_SECOND)
            } while ((seconds ?: 0L) > 0L)
        }
    }
    return seconds
}

private const val MILLIS_PER_SECOND = 1_000L
private const val SECONDS_PER_MINUTE = 60L
