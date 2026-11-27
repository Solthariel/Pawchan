package com.solthariel.pawchan.data.network.proxy

import com.solthariel.pawchan.domain.proxy.ProxyMode
import com.solthariel.pawchan.domain.proxy.ProxyPreferences
import com.solthariel.pawchan.domain.proxy.ProxyProfile
import com.solthariel.pawchan.domain.proxy.ProxyService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private fun validProfile(profile: ProxyProfile): Boolean =
    when (profile.mode) {
        ProxyMode.DIRECT, ProxyMode.NEEDS_CONFIGURATION -> true
        ProxyMode.SOCKS5, ProxyMode.HTTP, ProxyMode.HTTPS -> proxyRouteFor(profile) != null
    }

/** Independent service state; unavailable settings never open a direct route. */
enum class ProxyRuntimeStatus { DIRECT, SAVING, NEEDS_CONFIGURATION, READY, INVALID_ADDRESS, SAVE_FAILED }

data class ProxyRuntimeState(
    val profile: ProxyProfile,
    val status: ProxyRuntimeStatus,
)

/** Own this for the lifetime of the network stack. All initial routes are blocked synchronously. */
class ServiceProxyRuntime(
    private val router: ScopedProxySelector,
    initial: ProxyPreferences,
    private val scope: CoroutineScope,
    private val persist: suspend (ProxyService, ProxyProfile) -> Unit,
) {
    private val guard = Any()
    private val versions = mutableMapOf<ProxyService, Long>()
    private val jobs = mutableMapOf<ProxyService, Job>()
    private val saves = ProxyService.entries.associateWith { Mutex() }
    private val mutableStates =
        MutableStateFlow(
            ProxyService.entries.associateWith { service ->
                val profile = initial.profile(service)
                if (profile.mode != ProxyMode.DIRECT) router.update(service, ProxyRoute.Unavailable)
                ProxyRuntimeState(
                    profile,
                    if (profile.mode ==
                        ProxyMode.DIRECT
                    ) {
                        ProxyRuntimeStatus.DIRECT
                    } else {
                        ProxyRuntimeStatus.SAVING
                    },
                )
            },
        )
    val states: StateFlow<Map<ProxyService, ProxyRuntimeState>> = mutableStates.asStateFlow()

    init {
        ProxyService.entries.forEach { service -> startSaved(service, initial.profile(service)) }
    }

    /** Returns false for invalid input; no network state or stored data is changed. */
    fun setProfile(
        service: ProxyService,
        profile: ProxyProfile,
    ): Boolean {
        if (!validProfile(profile)) return false
        val version =
            synchronized(guard) {
                val next = (versions[service] ?: 0) + 1
                versions[service] = next
                router.update(service, ProxyRoute.Unavailable)
                jobs.remove(service)?.cancel()
                setState(service, ProxyRuntimeState(profile, ProxyRuntimeStatus.SAVING))
                next
            }
        val job =
            scope.launch {
                try {
                    val saved =
                        saves.getValue(service).withLock {
                            if (!current(service, version)) {
                                false
                            } else {
                                persist(service, profile)
                                current(service, version)
                            }
                        }
                    if (saved) activate(service, profile, version)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    updateIfCurrent(service, version, ProxyRuntimeState(profile, ProxyRuntimeStatus.SAVE_FAILED))
                }
            }
        registerJob(service, version, job)
        return true
    }

    private fun startSaved(
        service: ProxyService,
        profile: ProxyProfile,
    ) {
        val version =
            synchronized(guard) {
                val next = (versions[service] ?: 0) + 1
                versions[service] = next
                next
            }
        val job = scope.launch { activate(service, profile, version) }
        registerJob(service, version, job)
    }

    private suspend fun activate(
        service: ProxyService,
        profile: ProxyProfile,
        version: Long,
    ) {
        if (!current(service, version)) return
        when (profile.mode) {
            ProxyMode.DIRECT -> {
                applyRoute(service, version, ProxyRoute.Direct, ProxyRuntimeState(profile, ProxyRuntimeStatus.DIRECT))
            }

            ProxyMode.NEEDS_CONFIGURATION -> {
                updateIfCurrent(service, version, ProxyRuntimeState(profile, ProxyRuntimeStatus.NEEDS_CONFIGURATION))
            }

            ProxyMode.SOCKS5, ProxyMode.HTTP, ProxyMode.HTTPS -> {
                val route = proxyRouteFor(profile)
                if (route == null) {
                    updateIfCurrent(service, version, ProxyRuntimeState(profile, ProxyRuntimeStatus.INVALID_ADDRESS))
                } else {
                    applyRoute(service, version, route, ProxyRuntimeState(profile, ProxyRuntimeStatus.READY))
                }
            }
        }
    }

    private fun applyRoute(
        service: ProxyService,
        version: Long,
        route: ProxyRoute,
        state: ProxyRuntimeState,
    ) = synchronized(guard) {
        if (versions[service] == version) {
            router.update(service, route)
            setState(service, state)
        }
    }

    private fun registerJob(
        service: ProxyService,
        version: Long,
        job: Job,
    ) = synchronized(guard) {
        if (versions[service] == version) jobs[service] = job else job.cancel()
    }

    private fun current(
        service: ProxyService,
        version: Long,
    ): Boolean = synchronized(guard) { versions[service] == version }

    private fun updateIfCurrent(
        service: ProxyService,
        version: Long,
        state: ProxyRuntimeState,
    ) = synchronized(guard) {
        if (versions[service] == version) setState(service, state)
    }

    private fun setState(
        service: ProxyService,
        state: ProxyRuntimeState,
    ) {
        mutableStates.update { it + (service to state) }
    }
}
