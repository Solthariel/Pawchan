package com.solthariel.pawchan

import android.content.ContentResolver
import android.net.Uri
import com.solthariel.pawchan.domain.statistics.UsageMetric
import com.solthariel.pawchan.domain.statistics.UsageStatisticsRepository
import java.io.IOException

internal suspend fun recordSavedDownload(
    resolver: ContentResolver,
    uri: Uri,
    identity: String,
    stats: UsageStatisticsRepository,
) {
    val fileIdentity =
        com.solthariel.pawchan.domain.statistics
            .usageMediaIdentity(identity)
    stats.record(UsageMetric.FILES_DOWNLOADED, identity = fileIdentity)
    val size =
        try {
            resolver.openFileDescriptor(uri, "r")?.use { it.statSize } ?: 0L
        } catch (_: IOException) {
            0L
        } catch (_: SecurityException) {
            0L
        }
    if (size > 0) stats.record(UsageMetric.BYTES_DOWNLOADED, size, fileIdentity)
}
