package com.solthariel.pawchan.infrastructure

import android.content.Context
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.solthariel.pawchan.data.network.proxy.ProxyCatalogRuntime
import com.solthariel.pawchan.data.network.proxy.ProxyRoute
import com.solthariel.pawchan.data.network.proxy.ProxyRuntimeStatus
import com.solthariel.pawchan.data.network.proxy.ScopedProxySelector
import com.solthariel.pawchan.data.network.proxy.ServiceProxyRuntime
import com.solthariel.pawchan.data.preferences.DataStoreSettingsRepository
import com.solthariel.pawchan.domain.proxy.ProxyMode
import com.solthariel.pawchan.domain.proxy.ProxyPreferences
import com.solthariel.pawchan.domain.proxy.ProxyService
import com.solthariel.pawchan.infrastructure.sites.CachedBoardDirectory
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

internal val LocalProxyRuntime = staticCompositionLocalOf<ServiceProxyRuntime?> { null }
internal val LocalProxyCatalog = staticCompositionLocalOf<ProxyCatalogRuntime?> { null }

internal class AppNetworkViewModel private constructor(
    val stack: AppNetworkStack,
    settings: DataStoreSettingsRepository,
    initial: ProxyPreferences,
    router: ScopedProxySelector,
) : ViewModel() {
    val proxies = ServiceProxyRuntime(router, initial, viewModelScope) { _, _ -> }
    val proxyCatalog =
        ProxyCatalogRuntime(proxies, initial, viewModelScope, persist = settings.proxyPreferences::saveCatalog)

    init {
        stack.sources.forEach { source ->
            val directory = source.directory as? CachedBoardDirectory ?: return@forEach
            val service =
                when (source.site.value) {
                    "dvach" -> ProxyService.DVACH
                    "4chan" -> ProxyService.FOUR_CHAN
                    else -> return@forEach
                }
            viewModelScope.launch {
                directory.restore()
                proxies.states.map { it[service]?.status }.distinctUntilChanged().collectLatest { status ->
                    if (status == ProxyRuntimeStatus.DIRECT || status == ProxyRuntimeStatus.READY) {
                        directory.refreshAtStartup()
                    }
                }
            }
        }
    }

    override fun onCleared() {
        stack.close()
    }

    companion object {
        fun factory(
            context: Context,
            settings: DataStoreSettingsRepository,
            initial: ProxyPreferences,
            cacheMb: Int = com.solthariel.pawchan.domain.settings.StoragePreferences.DEFAULT_MB,
            mirrorHost: String = "2ch.su",
        ): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    val router = ScopedProxySelector()
                    // No client exists yet; restore enabled services as blocked before construction.
                    ProxyService.entries.forEach { service ->
                        if (initial.profile(service).mode != ProxyMode.DIRECT) {
                            router.update(service, ProxyRoute.Unavailable)
                        }
                    }
                    val stack =
                        AppNetworkStack.create(
                            context.applicationContext,
                            settings,
                            cacheMb,
                            mirrorHost,
                            proxySelector = router,
                        )
                    AppNetworkViewModel(stack, settings, initial, router)
                }
            }
    }
}
