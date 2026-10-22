package com.solthariel.pawchan.security

/** Five free mistakes; all later mistakes wait ten seconds, until successful authentication. */
internal object PinAttemptPolicy {
    const val DELAY_MILLIS = 10_000L
    private const val FREE_ATTEMPTS = 5

    fun nextFailures(
        previous: Int,
        valid: Boolean,
    ): Int = if (valid) 0 else previous.coerceIn(0, FREE_ATTEMPTS) + 1

    fun shouldDelay(failures: Int): Boolean = failures > FREE_ATTEMPTS

    fun remaining(
        now: Long,
        deadline: Long,
    ): Long = (deadline - now).takeIf { it in 1..DELAY_MILLIS } ?: 0L
}
