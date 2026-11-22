package com.solthariel.pawchan.data.network.proxy

import com.solthariel.pawchan.domain.proxy.ProxyMode
import com.solthariel.pawchan.domain.proxy.ProxyPreferences
import com.solthariel.pawchan.domain.proxy.ProxyService

/** Apply persisted policy before isolated clients can make requests. */
fun prepareProxyRoutes(
    router: ScopedProxySelector,
    preferences: ProxyPreferences,
    services: Set<ProxyService>,
) {
    services.forEach { service ->
        if (preferences.profile(service).mode != ProxyMode.DIRECT) {
            router.update(service, ProxyRoute.Unavailable)
        }
    }
    services.forEach { service ->
        val profile = preferences.profile(service)
        if (profile.mode == ProxyMode.DIRECT) {
            router.update(service, ProxyRoute.Direct)
        } else {
            proxyRouteFor(profile)?.let { router.update(service, it) }
        }
    }
}
