package com.solthariel.pawchan.data.network

import com.solthariel.pawchan.domain.network.ForumMirrors
import com.solthariel.pawchan.domain.network.MirrorAvailability
import com.solthariel.pawchan.domain.network.MirrorObservation
import com.solthariel.pawchan.domain.network.MirrorState
import com.solthariel.pawchan.domain.settings.ForumConnectionPreferences
import com.solthariel.pawchan.domain.settings.validMirrorHost
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.Call
import java.util.concurrent.CompletableFuture

/** App-stack owner; one bounded shared round, never one probe fan-out per failed request. */
class MirrorRecovery internal constructor(
    initialHost: String,
    private val probe: (String, (MirrorObservation) -> Unit) -> Call,
    private val closeProbe: () -> Unit,
) : ForumMirrors,
    AutoCloseable {
    private val lock = Any()
    private val mutableState = MutableStateFlow(MirrorState(initialHost))
    override val state = mutableState.asStateFlow()
    private var preferences = ForumConnectionPreferences(initialHost)
    private var round: MirrorCheckRound? = null
    private var lastFailureAt = 0L
    private var closed = false

    fun configure(value: ForumConnectionPreferences) =
        synchronized(lock) {
            require(validMirrorHost(value.preferredHost))
            if (value == preferences || closed) return@synchronized
            cancelRound()
            if (preferences.preferredHost != value.preferredHost) {
                mutableState.value = state.value.copy(activeHost = value.preferredHost)
            }
            preferences = value
            lastFailureAt = 0L
        }

    override fun selectHost(host: String) =
        synchronized(lock) {
            require(validMirrorHost(host))
            if (!closed) {
                cancelRound()
                preferences = preferences.copy(preferredHost = host)
                mutableState.value = state.value.copy(activeHost = host)
                lastFailureAt = 0L
            }
        }

    override fun checkAvailability() =
        synchronized(lock) {
            if (!closed && round == null) startRound(null)
        }

    internal fun recover(failedHost: String): CompletableFuture<String?> =
        synchronized(lock) {
            if (closed || !preferences.automaticFallback) return@synchronized CompletableFuture.completedFuture(null)
            if (state.value.activeHost != failedHost) {
                return@synchronized CompletableFuture.completedFuture(state.value.activeHost)
            }
            round?.let {
                return@synchronized if (it.failedHost == failedHost) {
                    it.result
                } else {
                    CompletableFuture.completedFuture(null)
                }
            }
            if (System.currentTimeMillis() - lastFailureAt < FAILURE_ROUND_COOLDOWN) {
                return@synchronized CompletableFuture.completedFuture(null)
            }
            startRound(failedHost).result
        }

    private fun startRound(failedHost: String?): MirrorCheckRound {
        val hosts = (listOf(state.value.activeHost) + MIRROR_HOSTS).distinct().filter { it != failedHost }
        val check = MirrorCheckRound(failedHost, hosts.size)
        round = check
        mutableState.value = state.value.copy(checking = true)
        hosts.forEach { host -> check.calls.add(probe(host) { observation -> receive(check, observation) }) }
        return check
    }

    private fun receive(
        check: MirrorCheckRound,
        observation: MirrorObservation,
    ) = synchronized(lock) {
        if (round !== check || closed) return@synchronized
        check.observations.add(observation)
        mutableState.value = state.value.copy(observations = check.observations.toList())
        val available = observation.availability == MirrorAvailability.AVAILABLE
        if (check.failedHost != null && available && !check.result.isDone) {
            mutableState.value =
                state.value.copy(activeHost = observation.host, switchSerial = state.value.switchSerial + 1L)
            check.result.complete(observation.host)
        }
        if (check.observations.size == check.expected) {
            check.result.complete(null)
            if (check.failedHost != null) lastFailureAt = System.currentTimeMillis()
            round = null
            mutableState.value = state.value.copy(checking = false)
        }
    }

    private fun cancelRound() {
        round?.let { check ->
            round = null
            check.result.complete(null)
            check.calls.forEach(Call::cancel)
        }
        mutableState.value = state.value.copy(checking = false)
    }

    override fun close() {
        synchronized(lock) {
            closed = true
            cancelRound()
        }
        closeProbe()
    }

    companion object {
        fun create(initialHost: String): MirrorRecovery {
            val probe = MirrorProbe()
            return MirrorRecovery(initialHost, probe::start, probe::close)
        }
    }
}

private class MirrorCheckRound(
    val failedHost: String?,
    val expected: Int,
) {
    val result = CompletableFuture<String?>()
    val calls = mutableListOf<Call>()
    val observations = mutableListOf<MirrorObservation>()
}

private val MIRROR_HOSTS = listOf("2ch.su", "2ch.org", "2ch.life")
private const val FAILURE_ROUND_COOLDOWN = 30_000L
