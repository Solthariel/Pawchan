package com.solthariel.pawchan.data.network

import com.solthariel.pawchan.domain.network.MirrorAvailability
import com.solthariel.pawchan.domain.network.MirrorObservation
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.IOException
import java.io.InterruptedIOException
import java.util.concurrent.TimeUnit

/** One probe, including queueing, DNS/TLS and bounded body validation; not ICMP ping. */
internal class MirrorProbeCallback(
    private val host: String,
    private val report: (MirrorObservation) -> Unit,
    private val nanoTime: () -> Long = System::nanoTime,
    private val wallTime: () -> Long = System::currentTimeMillis,
) : Callback {
    private val started = nanoTime()

    override fun onFailure(
        call: Call,
        e: IOException,
    ) {
        val status =
            if (e is InterruptedIOException && !call.isCanceled()) {
                MirrorAvailability.TIMEOUT
            } else {
                MirrorAvailability.NETWORK_ERROR
            }
        // Call timeout can also mark the transport cancelled: always finish the callback.
        // MirrorRecovery owns cancellation/generation and ignores obsolete rounds.
        report(MirrorObservation(host, status, wallTime()))
    }

    override fun onResponse(
        call: Call,
        response: Response,
    ) {
        val status = response.use(::classifyMirrorResponse)
        val elapsed = TimeUnit.NANOSECONDS.toMillis((nanoTime() - started).coerceAtLeast(0))
        report(MirrorObservation(host, status, wallTime(), elapsed))
    }
}
