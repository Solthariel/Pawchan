package com.solthariel.pawchan.domain.settings

/** Background intervals obey Android's minimum periodic work interval. */
data class AutomationPreferences(
    val openThread: Boolean = false,
    val openIntervalSeconds: Int = 30,
    val favorites: Boolean = true,
    val replies: Boolean = true,
    val backgroundMinutes: Int = 15,
    val wifiOnly: Boolean = false,
    val notifications: NotificationPreferences = NotificationPreferences(),
) {
    companion object {
        const val DEFAULT_OPEN_SECONDS = 30
        const val MIN_BACKGROUND_MINUTES = 15
        private const val SECONDS_15 = 15
        private const val SECONDS_60 = 60
        private const val SECONDS_120 = 120
        private const val MINUTES_30 = 30
        private const val MINUTES_60 = 60
        private const val MINUTES_180 = 180
        val OPEN_INTERVALS = listOf(SECONDS_15, DEFAULT_OPEN_SECONDS, SECONDS_60, SECONDS_120)
        val BACKGROUND_INTERVALS = listOf(MIN_BACKGROUND_MINUTES, MINUTES_30, MINUTES_60, MINUTES_180)
    }
}

data class NotificationPreferences(
    val replies: Boolean = true,
    val posts: Boolean = true,
)

enum class ImagePreload { NEVER, WIFI, ALWAYS }

enum class DownloadNaming { ORIGINAL, DETAILED }

data class DownloadPreferences(
    val treeUri: String? = null,
    val naming: DownloadNaming = DownloadNaming.ORIGINAL,
)

data class StoragePreferences(
    val cacheMb: Int = DEFAULT_MB,
) {
    companion object {
        const val MIN_MB = 100
        const val DEFAULT_MB = 5_000
        const val MAX_MB = 10_000
    }
}
