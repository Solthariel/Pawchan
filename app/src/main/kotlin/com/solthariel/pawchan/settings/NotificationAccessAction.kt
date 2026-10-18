package com.solthariel.pawchan.settings

internal enum class NotificationAccessAction { NONE, REQUEST, SETTINGS }

internal fun notificationAccessAction(
    api: Int,
    runtimeGranted: Boolean,
    enabled: Boolean,
    rationale: Boolean,
): NotificationAccessAction =
    when {
        enabled -> NotificationAccessAction.NONE
        api >= NOTIFICATIONS_API && !runtimeGranted && rationale -> NotificationAccessAction.REQUEST
        else -> NotificationAccessAction.SETTINGS
    }

private const val NOTIFICATIONS_API = 33
