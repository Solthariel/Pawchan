package com.solthariel.pawchan

import com.solthariel.pawchan.data.network.proxy.ScopedProxySelector
import com.solthariel.pawchan.data.network.proxy.prepareProxyRoutes
import com.solthariel.pawchan.data.preferences.DataStoreSettingsRepository
import com.solthariel.pawchan.domain.proxy.ProxyService
import kotlinx.coroutines.flow.first

/** An isolated worker must prepare its own routes before creating any network clients. */
internal suspend fun workerProxyRoutes(
    settings: DataStoreSettingsRepository,
    mirror: String,
    services: Set<ProxyService>,
): ScopedProxySelector {
    val preferences = settings.proxyPreferences.preferences.first()
    val router = ScopedProxySelector().apply { registerMirror(mirror) }
    prepareProxyRoutes(router, preferences, services)
    return router
}
