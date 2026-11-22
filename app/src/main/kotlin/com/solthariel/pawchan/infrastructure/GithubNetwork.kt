package com.solthariel.pawchan.infrastructure

import androidx.compose.runtime.staticCompositionLocalOf
import com.solthariel.pawchan.data.network.proxy.proxyRouter
import com.solthariel.pawchan.data.network.proxy.viaProxyRouter
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/** App-owned transport: shares only connection resources, never forum cookies or interceptors. */
internal class GithubNetwork(
    shared: OkHttpClient,
) {
    private val bootstrap =
        OkHttpClient
            .Builder()
            .dispatcher(shared.dispatcher)
            .connectionPool(shared.connectionPool)
            .apply { shared.proxyRouter()?.let { viaProxyRouter(it) } }
            .build()
    val historyClient: OkHttpClient =
        bootstrap
            .newBuilder()
            .callTimeout(HISTORY_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .followSslRedirects(false)
            .build()
}

internal val LocalGithubNetwork =
    staticCompositionLocalOf<GithubNetwork> {
        error("GitHub transport must be provided by the app network owner")
    }

private const val HISTORY_TIMEOUT_SECONDS = 15L
