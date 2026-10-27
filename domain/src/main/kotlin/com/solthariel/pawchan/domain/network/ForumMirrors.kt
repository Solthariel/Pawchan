package com.solthariel.pawchan.domain.network

import kotlinx.coroutines.flow.StateFlow

/** Availability is observed from this device, never a global uptime claim. */
enum class MirrorAvailability {
    AVAILABLE,
    TIMEOUT,
    SERVER_ERROR,
    VERIFICATION,
    RATE_LIMITED,
    INVALID_RESPONSE,
    NETWORK_ERROR,
}

data class MirrorObservation(
    val host: String,
    val availability: MirrorAvailability,
    val checkedAtMillis: Long,
)

data class MirrorState(
    val activeHost: String,
    val checking: Boolean = false,
    val observations: List<MirrorObservation> = emptyList(),
    val switchSerial: Long = 0L,
)

interface ForumMirrors {
    val state: StateFlow<MirrorState>

    fun selectHost(host: String)

    fun checkAvailability()
}
