package com.solthariel.pawchan.infrastructure

import androidx.compose.runtime.staticCompositionLocalOf
import com.solthariel.pawchan.domain.settings.ServiceDns
import com.solthariel.pawchan.infrastructure.dns.GithubDnsSelector
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/** App-owned transport: shares only connection resources, never forum cookies or interceptors. */
internal class GithubNetwork(
    shared: OkHttpClient,
    initial: ServiceDns = ServiceDns(),
) {
    private val bootstrap =
        OkHttpClient
            .Builder()
            .dispatcher(shared.dispatcher)
            .connectionPool(shared.connectionPool)
            .build()
    val dns = GithubDnsSelector(bootstrap).apply { update(initial) }
    val historyClient: OkHttpClient =
        bootstrap
            .newBuilder()
            .dns(dns)
            .callTimeout(HISTORY_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .followSslRedirects(false)
            .build()

    fun update(settings: ServiceDns) {
        dns.update(settings)
    }
}

internal val LocalGithubNetwork =
    staticCompositionLocalOf<GithubNetwork> {
        error("GitHub transport must be provided by the app network owner")
    }

private const val HISTORY_TIMEOUT_SECONDS = 15L
