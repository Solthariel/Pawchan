package com.solthariel.pawchan.infrastructure

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.withResumed
import com.solthariel.pawchan.data.preferences.NotificationPromptStore
import kotlinx.coroutines.launch

/** Registered before STARTED; the first-run request is not repeated after a refusal. */
internal class NotificationPermissionController(
    private val activity: ComponentActivity,
    private val store: NotificationPromptStore,
) {
    private val launcher =
        activity.activityResultRegistry.register(
            "pawchan.notification.permission",
            activity,
            ActivityResultContracts.RequestPermission(),
        ) { }

    fun requestOnce() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        activity.lifecycleScope.launch {
            if (store.claimFirstRequest()) {
                activity.lifecycle.withResumed {
                    if (ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED
                    ) {
                        launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }
        }
    }
}
