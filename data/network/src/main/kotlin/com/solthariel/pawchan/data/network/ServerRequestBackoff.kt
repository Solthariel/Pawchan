package com.solthariel.pawchan.data.network

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/** Per-client, per-host policy, shared by API and content client clones. */
class ServerRequestBackoff(
    private val monotonicMillis: () -> Long = defaultMonotonicClock(),
    private val wallMillis: () -> Long = System::currentTimeMillis,
) {
    private val hosts = HashMap<String, HostState>()

    @Synchronized
    fun remainingMillis(host: String): Long {
        val deadline = hosts[host]?.blockedUntil ?: 0L
        return (deadline - monotonicMillis()).coerceAtLeast(0L)
    }

    /** Zero grants a start; a positive value grants nothing and must be awaited outside a worker. */
    @Synchronized
    fun previewStartDelayMillis(host: String): Long {
        val now = monotonicMillis()
        val state = hosts.getOrPut(host) { HostState() }
        val remaining = (maxOf(state.blockedUntil, state.nextPreviewStart) - now).coerceAtLeast(0L)
        if (remaining > 0L) return remaining
        val spacing = if (now - state.lastLimitedAt < RECOVERY_MILLIS) RECOVERY_SPACING else NORMAL_SPACING
        state.nextPreviewStart = now + spacing
        return 0L
    }

    @Synchronized
    fun rateLimited(
        host: String,
        retryAfter: String?,
        serverDate: String?,
    ) {
        val now = monotonicMillis()
        val state = hosts.getOrPut(host) { HostState() }
        if (now - state.lastLimitedAt >= RECOVERY_MILLIS) state.failures = 0
        state.failures = (state.failures + 1).coerceAtMost(MAX_FAILURES)
        val fallback = (FALLBACK_MILLIS shl (state.failures - 1)).coerceAtMost(MAX_FALLBACK_MILLIS)
        val pause = retryAfterMillis(retryAfter, serverDate, wallMillis()) ?: fallback
        state.blockedUntil = maxOf(state.blockedUntil, now + pause.coerceAtMost(Long.MAX_VALUE - now))
        state.lastLimitedAt = now
    }

    private class HostState {
        var blockedUntil = 0L
        var nextPreviewStart = 0L
        var lastLimitedAt = -RECOVERY_MILLIS
        var failures = 0
    }
}

private fun defaultMonotonicClock(): () -> Long {
    val origin = System.nanoTime()
    return { (System.nanoTime() - origin) / NANOS_PER_MILLISECOND }
}

internal fun retryAfterMillis(
    value: String?,
    serverDate: String?,
    wallNow: Long,
): Long? {
    val text = value?.trim()
    val seconds = text?.toLongOrNull()
    return when {
        text == null -> null
        seconds != null -> retrySecondsToMillis(seconds)
        else -> retryDateToMillis(text, serverDate, wallNow)
    }
}

private fun retrySecondsToMillis(seconds: Long): Long? =
    if (seconds < 0L) {
        null
    } else {
        seconds
            .coerceAtMost(Long.MAX_VALUE / MILLIS_PER_SECOND)
            .times(MILLIS_PER_SECOND)
            .coerceAtLeast(MILLIS_PER_SECOND)
    }

private fun retryDateToMillis(
    text: String,
    serverDate: String?,
    wallNow: Long,
): Long? {
    val timestamp = httpDateMillis(text)
    val base = serverDate?.let(::httpDateMillis) ?: wallNow
    return timestamp?.let { (it - base).coerceAtLeast(MILLIS_PER_SECOND) }
}

private fun httpDateMillis(value: String): Long? =
    try {
        ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().toEpochMilli()
    } catch (_: DateTimeParseException) {
        null
    }

private const val NANOS_PER_MILLISECOND = 1_000_000L
private const val MILLIS_PER_SECOND = 1_000L
private const val NORMAL_SPACING = 250L
private const val RECOVERY_SPACING = 1_000L
private const val RECOVERY_MILLIS = 60_000L
private const val FALLBACK_MILLIS = 5_000L
private const val MAX_FALLBACK_MILLIS = 60_000L
private const val MAX_FAILURES = 5
