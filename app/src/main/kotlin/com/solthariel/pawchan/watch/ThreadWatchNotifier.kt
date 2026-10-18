package com.solthariel.pawchan.watch

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
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import com.solthariel.pawchan.MainActivity
import com.solthariel.pawchan.R
import com.solthariel.pawchan.domain.watch.ThreadWatchSubscription
import com.solthariel.pawchan.navigation.AppRoute
import com.solthariel.pawchan.navigation.saveableKey

internal class ThreadWatchNotifier(
    private val context: Context,
    private val replies: Boolean = false,
) : ThreadWatchEventSink {
    override fun onNewPosts(
        subscription: ThreadWatchSubscription,
        count: Int,
    ) {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val address = subscription.address
        val title =
            subscription.subject
                ?: context.getString(R.string.thread_number, address.thread.value)
        val text =
            context.resources.getQuantityString(
                R.plurals.watcher_notification_posts,
                count,
                count,
            )
        val notification =
            NotificationCompat
                .Builder(context, if (replies) REPLIES_CHANNEL else CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(if (replies) "Новых ответов на ваши посты: $count" else text)
                .setGroup(GROUP_KEY)
                .setAutoCancel(true)
                .setContentIntent(threadPendingIntent(subscription))
                .build()
        NotificationManagerCompat
            .from(context)
            .notify(notificationId(subscription), notification)
    }

    private fun threadPendingIntent(subscription: ThreadWatchSubscription): PendingIntent {
        val intent =
            Intent(context, MainActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                data = AppRoute.threadDeepLink(subscription.address).toUri()
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        return PendingIntent.getActivity(
            context,
            notificationId(subscription),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun notificationId(subscription: ThreadWatchSubscription): Int =
        (subscription.address.saveableKey + if (replies) ":replies" else ":posts").hashCode()

    companion object {
        private const val REPLIES_CHANNEL = "thread-replies"
        private const val CHANNEL_ID = "thread-watch"
        private const val GROUP_KEY = "thread-watch"

        fun createChannel(context: Context) {
            val channel =
                NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.watcher_channel_name),
                    NotificationManager.IMPORTANCE_DEFAULT,
                ).apply {
                    description = context.getString(R.string.watcher_channel_description)
                }
            context.getSystemService<NotificationManager>()?.apply {
                createNotificationChannel(channel)
                createNotificationChannel(
                    NotificationChannel(
                        REPLIES_CHANNEL,
                        "Ответы на ваши посты",
                        NotificationManager.IMPORTANCE_DEFAULT,
                    ),
                )
            }
        }
    }
}
