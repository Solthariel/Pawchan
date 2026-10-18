package com.solthariel.pawchan.settings

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.solthariel.pawchan.domain.settings.AutomationPreferences

@Composable
internal fun NotificationSettings(
    value: AutomationPreferences,
    change: ((AutomationPreferences) -> AutomationPreferences) -> Unit,
) {
    val context = LocalContext.current
    val activity = LocalActivity.current
    var enabled by remember { mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled()) }
    val permission =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
            enabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    LifecycleResumeEffect(context) {
        enabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
        onPauseOrDispose {}
    }
    val openSettings = {
        context.startActivity(
            Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName),
        )
    }
    val allow = {
        val runtimeGranted =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) == PackageManager.PERMISSION_GRANTED
        val rationale =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                activity?.let {
                    ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.POST_NOTIFICATIONS)
                } == true
        val action = notificationAccessAction(Build.VERSION.SDK_INT, runtimeGranted, enabled, rationale)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && action == NotificationAccessAction.REQUEST) {
            permission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            openSettings()
        }
    }
    Column {
        SettingsItemRow(
            SettingsItem.Toggle("Уведомления об ответах", null, enabled && value.notifications.replies) { on ->
                change { it.copy(notifications = it.notifications.copy(replies = on)) }
                if (on && !enabled) allow()
            },
        )
        HorizontalDivider(Modifier.padding(horizontal = 16.dp))
        SettingsItemRow(
            SettingsItem.Toggle("Уведомления о новых постах", null, enabled && value.notifications.posts) { on ->
                change { it.copy(notifications = it.notifications.copy(posts = on)) }
                if (on && !enabled) allow()
            },
        )
        if (!enabled) {
            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
            SettingsItemRow(SettingsItem.Action("Разрешить уведомления", null, onClick = { allow() }))
        }
    }
}
