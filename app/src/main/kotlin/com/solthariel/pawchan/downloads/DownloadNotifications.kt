package com.solthariel.pawchan.downloads

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.solthariel.pawchan.MainActivity
import com.solthariel.pawchan.R

internal class DownloadNotifications(
    private val context: Context,
) {
    fun update(
        id: String,
        name: String,
        status: DownloadStatus,
    ) {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled() ||
            (
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                    PackageManager.PERMISSION_GRANTED
            )
        ) {
            return
        }
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.cancel("media-downloads-group", NOTIFICATION_ID)
        manager.createNotificationChannel(NotificationChannel(CHANNEL, "Загрузки", NotificationManager.IMPORTANCE_LOW))
        val text =
            when (status) {
                DownloadStatus.QUEUED -> "Ожидание загрузки"
                DownloadStatus.RUNNING -> "Загрузка"
                DownloadStatus.WAITING -> "Ожидание повторной попытки"
                DownloadStatus.SUCCEEDED -> "Сохранено"
                DownloadStatus.FAILED -> "Не удалось сохранить"
                DownloadStatus.CANCELLED -> "Загрузка отменена"
            }
        val pending = status in setOf(DownloadStatus.QUEUED, DownloadStatus.RUNNING, DownloadStatus.WAITING)
        val intent =
            PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        val notification =
            NotificationCompat
                .Builder(context, CHANNEL)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(name)
                .setContentText(text)
                .setOnlyAlertOnce(true)
                .setOngoing(pending)
                .setAutoCancel(!pending)
                .setTimeoutAfter(notificationTimeoutMillis(status))
                .setContentIntent(intent)
                .setProgress(0, 0, pending)
                .build()
        NotificationManagerCompat.from(context).notify(id, NOTIFICATION_ID, notification)
    }

    fun clear(id: String) {
        NotificationManagerCompat.from(context).cancel(id, NOTIFICATION_ID)
    }
}

private const val CHANNEL = "media-downloads"
private const val NOTIFICATION_ID = 1401

internal fun notificationTimeoutMillis(status: DownloadStatus): Long =
    if (status == DownloadStatus.SUCCEEDED) SUCCESS_TIMEOUT_MILLIS else 0L

private const val SUCCESS_TIMEOUT_MILLIS = 5_000L
