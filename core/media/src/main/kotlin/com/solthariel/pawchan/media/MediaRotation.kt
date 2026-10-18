package com.solthariel.pawchan.media

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext

/** Переключение портрет/ландшафт, пока открыт просмотрщик; null — экран не умеет поворачиваться. */
internal val LocalMediaRotation = compositionLocalOf<(() -> Unit)?> { null }

/**
 * Поворот действует, пока открыт просмотрщик. При закрытии ориентация снова определяется
 * системой; пересоздание активности из-за самого поворота ориентацию не сбрасывает.
 */
@Composable
internal fun MediaRotationScope(content: @Composable () -> Unit) {
    val activity = LocalContext.current.findActivity()
    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    DisposableEffect(activity) {
        onDispose {
            if (activity != null && !activity.isChangingConfigurations) {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
        }
    }
    val rotate: (() -> Unit)? =
        activity?.let {
            {
                it.requestedOrientation =
                    if (landscape) {
                        ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
                    } else {
                        ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                    }
            }
        }
    CompositionLocalProvider(LocalMediaRotation provides rotate, content = content)
}

private tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
