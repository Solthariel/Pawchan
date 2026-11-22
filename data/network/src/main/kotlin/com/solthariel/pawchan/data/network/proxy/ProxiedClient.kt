package com.solthariel.pawchan.data.network.proxy

import okhttp3.Dns
import okhttp3.OkHttpClient

/**
 * The one supported way to let an OkHttp client reach proxied resources. It installs everything
 * that must work together: resolution without leaks, the tunnelling socket factory, the selector
 * and the guard against stale connections and redirects out of the resource. [directDns] resolves
 * only resources whose route is direct; a proxied resource is never resolved on this device.
 *
 * Do not copy a client's proxy selector into another builder: use this function again, or derive
 * the new client with `newBuilder()`, which keeps all four pieces.
 */
fun OkHttpClient.Builder.viaProxyRouter(
    router: ScopedProxySelector,
    trust: ProxyTlsTrust = ProxyTlsTrust.system(),
    directDns: Dns = Dns.SYSTEM,
): OkHttpClient.Builder =
    proxySelector(router.tunnelSelector())
        .dns(RoutedDns(router, directDns))
        .socketFactory(RoutedSocketFactory(router, trust))
        .addInterceptor(StaleProxyConnectionRetry())
        .addNetworkInterceptor(ProxyRedirectGuard(router))

/** The router this client was configured with by [viaProxyRouter], or null for an unrouted client. */
fun OkHttpClient.proxyRouter(): ScopedProxySelector? = (proxySelector as? TunnelProxySelector)?.router
