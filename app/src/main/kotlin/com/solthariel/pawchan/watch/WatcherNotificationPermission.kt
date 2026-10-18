package com.solthariel.pawchan.watch

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

@Composable
internal fun rememberWatcherNotificationToggle(
    watcherEnabled: Boolean,
    onToggle: () -> Unit,
): () -> Unit {
    val context = LocalContext.current
    val permissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) {
            onToggle()
        }
    return remember(context, watcherEnabled, onToggle, permissionLauncher) {
        {
            val permissionRequired =
                !watcherEnabled &&
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS,
                    ) != PackageManager.PERMISSION_GRANTED
            if (permissionRequired) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                onToggle()
            }
        }
    }
}
