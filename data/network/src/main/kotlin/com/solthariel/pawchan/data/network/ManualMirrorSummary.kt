package com.solthariel.pawchan.data.network

import com.solthariel.pawchan.domain.network.MirrorAvailability
import com.solthariel.pawchan.domain.network.MirrorObservation

/** Missing callbacks at the deadline are unsuccessful samples, not zero-millisecond responses. */
internal fun summarizeManualMirror(
    host: String,
    observations: List<MirrorObservation>,
    total: Int,
): MirrorObservation {
    val successful = observations.filter { it.availability == MirrorAvailability.AVAILABLE }
    val times = successful.mapNotNull { it.responseTimeMillis?.takeIf { time -> time >= 0 } }
    return MirrorObservation(
        host = host,
        availability =
            if (successful.isNotEmpty()) {
                MirrorAvailability.AVAILABLE
            } else {
                observations.lastOrNull()?.availability ?: MirrorAvailability.TIMEOUT
            },
        checkedAtMillis = observations.maxOfOrNull { it.checkedAtMillis } ?: System.currentTimeMillis(),
        responseTimeMillis = if (times.isEmpty()) null else (times.sum() / times.size),
        successfulSamples = successful.size,
        totalSamples = total,
    )
}
