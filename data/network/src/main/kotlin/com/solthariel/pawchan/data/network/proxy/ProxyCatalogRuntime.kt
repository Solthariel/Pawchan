package com.solthariel.pawchan.data.network.proxy

import com.solthariel.pawchan.domain.proxy.ProxyMode
import com.solthariel.pawchan.domain.proxy.ProxyPreferences
import com.solthariel.pawchan.domain.proxy.ProxyProfile
import com.solthariel.pawchan.domain.proxy.ProxyService
import com.solthariel.pawchan.domain.proxy.SavedProxy
import com.solthariel.pawchan.domain.proxy.proxyPreferencesFromCatalog
import com.solthariel.pawchan.domain.proxy.validProxyCatalog
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class ProxyCatalogState(
    val catalog: List<SavedProxy>,
    val saving: Boolean = false,
    val saveFailed: Boolean = false,
    val needsReset: Boolean = false,
)

/** A single transaction owns all resource assignments and blocks routes during persistence. */
class ProxyCatalogRuntime(
    val services: ServiceProxyRuntime,
    initial: ProxyPreferences,
    private val scope: CoroutineScope,
    private val trust: ProxyTlsTrust = ProxyTlsTrust.system(),
    private val persist: suspend (List<SavedProxy>) -> Unit,
) {
    private val guard = Any()
    private val mutex = Mutex()
    private var version = 0L
    private val mutableState =
        MutableStateFlow(ProxyCatalogState(initial.savedProxies, needsReset = initial.needsReset))
    val state = mutableState.asStateFlow()

    fun save(catalog: List<SavedProxy>): Boolean {
        if (!validProxyCatalog(catalog) || !catalog.all(::validAddress)) return false
        val snapshot = catalog.map { it.copy(services = it.services.toSet()) }
        val target = proxyPreferencesFromCatalog(snapshot)
        val (next, affected) =
            synchronized(guard) {
                version += 1
                val changes =
                    ProxyService.entries
                        .filter { service ->
                            val current = services.states.value.getValue(service)
                            current.profile != target.profile(service) || current.status !in STABLE_STATUSES
                        }.toSet()
                changes.forEach { services.setProfile(it, ProxyProfile(ProxyMode.NEEDS_CONFIGURATION)) }
                mutableState.value = ProxyCatalogState(snapshot, saving = true)
                version to changes
            }
        scope.launch { persistAndActivate(snapshot, next, affected) }
        return true
    }

    /** Probe only the resource explicitly selected in the draft, using an isolated route. */
    suspend fun check(
        proxy: SavedProxy,
        service: ProxyService,
        mirror: String,
    ): ProxyProbeResult {
        val route = if (service in proxy.services) proxyRouteFor(proxy.mode, proxy.address, proxy.credentials) else null
        if (route == null) return ProxyProbeResult.Unavailable
        val router = ScopedProxySelector(null)
        router.update(service, route)
        return ProxyResourceProbe(router, trust).check(service, mirror)
    }

    private suspend fun persistAndActivate(
        catalog: List<SavedProxy>,
        requested: Long,
        affected: Set<ProxyService>,
    ) {
        try {
            mutex.withLock {
                if (!isCurrent(requested)) return
                persist(catalog)
                synchronized(guard) {
                    if (version == requested) {
                        val preferences = proxyPreferencesFromCatalog(catalog)
                        affected.forEach { services.setProfile(it, preferences.profile(it)) }
                        mutableState.value = ProxyCatalogState(catalog)
                    }
                }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            synchronized(guard) {
                if (version == requested) mutableState.value = ProxyCatalogState(catalog, saveFailed = true)
            }
        }
    }

    private fun isCurrent(requested: Long): Boolean = synchronized(guard) { version == requested }
}

private fun validAddress(proxy: SavedProxy): Boolean {
    val route = proxyRouteFor(proxy.mode, proxy.address, proxy.credentials)
    return route != null
}

private val STABLE_STATUSES = setOf(ProxyRuntimeStatus.READY, ProxyRuntimeStatus.DIRECT)
